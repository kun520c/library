# 设计决策

## 1. 保持单体分层

**问题**：借还书需要跨库存和借阅记录保持一致，项目主要用于学习、演示和源码审阅。**方案**：保留 Controller → Service → Mapper 的 Spring Boot 单体及独立 Vue 客户端。**原因**：单数据库事务能够表达业务约束，本地依赖和调试路径少。**代价/局限**：没有服务独立伸缩、异步事件可靠投递等能力；本项目也没有相应需求或验证数据。

## 2. 借书使用条件 UPDATE

**问题**：先查 `stock > 0` 再无条件减库存，会让多个请求基于同一旧值作出决定。**方案**：[BookMapper.decrementStockIfAvailable](../src/main/java/com/library/mapper/BookMapper.java) 执行 `UPDATE books SET stock = stock - 1 WHERE id = ? AND stock > 0 AND is_deleted = 0`，以影响行数判断成功。**原因**：数据库在同一行更新中完成判断与扣减。**代价/局限**：热门图书仍会争用行锁；没有限流、重试或性能承诺。只保证可用库存不因这条扣减变负，不等于限制每人借阅次数。

## 3. 库存调整使用 delta

**问题**：管理员打开编辑表单后可能发生借还书，提交旧的绝对库存会覆盖这些变化。**方案**：基本信息 DTO 不包含库存；[库存调整接口](../src/main/java/com/library/controller/BookController.java) 独立接受正负 `delta`。SQL 用 DECIMAL 中间运算和 `BETWEEN 0 AND 2147483647` 检查范围，兼容 MySQL UNSIGNED 的下界计算；还书加库存也用条件更新限制上界。**原因**：各次变更在数据库中叠加，不覆盖旧快照，也不产生 Java Integer 无法读取的库存。**代价/局限**：运维需计算调整量；没有库存流水、盘点功能。可借库存已满时归还返回 409，并回滚借阅状态，管理员需先调减库存留出空间。

## 4. 借还书需要事务

**问题**：扣库存成功但借阅记录失败会丢库存；改归还状态成功但库存恢复失败会丢可借副本。**方案**：[BorrowServiceImpl](../src/main/java/com/library/service/impl/BorrowServiceImpl.java) 用 Spring 数据库事务覆盖两次写入，运行时异常整体回滚。归还用 `WHERE status='BORROWED'` 更新成功后才加库存。**原因**：单数据库事务直接维护业务不变量。**代价/局限**：数据库锁持续到提交；重复归还返回 409，属于业务防重，没有通用请求幂等键，也不缓存第一次响应。

## 5. Redis 只缓存图书详情

**问题**：分页条件包括书名、作者、ISBN、分类、页码，组合多且失效范围难界定。**方案**：[BookServiceImpl.getById](../src/main/java/com/library/service/impl/BookServiceImpl.java) 采用 Cache-Aside，详情 TTL 默认 10 分钟；不存在的 ID 使用 1 分钟空标记。**原因**：按图书 ID 的缓存键清晰，写操作可以定位失效目标。**代价/局限**：分页和分类始终访问 MySQL；没有热点互斥、TTL 随机化或缓存击穿保护。Redis 失败时回源会增加数据库压力，超时不等于零延迟。

## 6. 缓存一致性的限制

**问题**：事务提交前删缓存会让其他请求从尚未提交的数据库读旧值。**方案**：[BookCacheInvalidator](../src/main/java/com/library/service/BookCacheInvalidator.java) 注册 `afterCommit`，数据库提交后删除；回滚不删。读、写、删异常记录安全日志，查询继续回源。**原因**：不把 Redis 故障变成数据库业务失败。**代价/局限**：提交到删除之间有窗口；删除失败会保留旧值直到 TTL；并发读在删除后回填旧快照也会造成 TTL 内旧值。缓存不是强一致读取，库存成功与否最终以数据库条件更新为准。没有 Outbox、可靠重试、版本校验或双删机制。

## 7. 区分 DTO、Entity、VO

**问题**：数据库对象可能包含密码、角色、删除标记，直接绑定或返回会扩展攻击面。**方案**：[DTO](../src/main/java/com/library/model/dto) 明确输入字段与校验，Entity 映射数据库，VO 定义安全响应；未知 JSON 属性拒绝。管理员用户分页 SQL 直接选择安全列，避免查询密码。**原因**：注册不能提交 ADMIN，普通图书更新不能覆盖库存，用户接口不返回密码哈希。**代价/局限**：需要显式字段转换；简单查询允许 Mapper 投影 VO，没有为形式整洁增加额外转换层。密码另按 UTF-8 校验 72 字节 BCrypt 上限。

## 8. JWT 与角色权限

**问题**：用户页面与管理员能力需要隔离，借阅记录还需区分数据所有者。**方案**：[JwtService](../src/main/java/com/library/security/JwtService.java) 签发 HS256 Token；[拦截器](../src/main/java/com/library/interceptor/AuthenticationInterceptor.java) 验签和有效期，用 `@RequireRole` 限制管理员接口，Service 检查借阅所有权，`UserContext` 在请求完成后清理。密码仅保存 BCrypt 哈希，解析错误不记录输入值。**原因**：适合两种固定角色，依赖与调用链直观。**代价/局限**：不是完整 Spring Security 或复杂 RBAC；Token 默认 12 小时，无撤销、刷新和登录限流。数据库角色修改不会立即改变已签发的权限，需重新登录取得新 Token；旧 ADMIN Token 仍有效至过期。浏览器将 Token 放 localStorage，需防范 XSS；生产部署仍需 HTTPS、密钥管理和更完整的访问治理。
