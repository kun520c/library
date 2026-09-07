# 图书管理系统

这是一个面向本科 Java 后端学习与项目讲解的 Spring Boot REST API。系统形成了“注册/登录 → JWT 身份认证 → USER/ADMIN 权限 → 图书与分类管理 → 借书原子扣库存 → 还书恢复库存 → 借阅历史查询”的完整业务闭环。

项目保持单体、分层、易理解，不包含微服务、消息队列、搜索引擎或复杂 RBAC。

## 技术栈

| 组件 | 版本/用途 |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring MVC | REST API、参数校验、拦截器 |
| MyBatis | 3.0.5，数据库访问 |
| MySQL | 8.x；Docker Compose 使用 8.4 |
| Redis | 图书详情 Cache-Aside 缓存 |
| JJWT | 0.12.6，HS256 JWT |
| BCrypt | Spring Security Crypto，仅用于密码哈希 |
| Jakarta Validation | DTO、分页和路径参数校验 |
| Knife4j / springdoc | API 文档 |
| H2 | 仅用于事务和并发集成测试 |
| Maven Wrapper | 构建与测试 |

权限认证没有引入完整 Spring Security。项目使用一个 MVC 拦截器解析 Bearer Token，以 `UserContext` 保存当前请求的用户，并通过 `@RequireRole` 做 ADMIN 检查；请求结束后一定清理 ThreadLocal。

## 功能清单

- 用户注册：账号唯一，密码 BCrypt 哈希，固定创建 `USER`。
- 用户登录：账号密码校验，签发包含 `userId`、`username`、`role` 的 JWT。
- 当前用户：安全返回用户基本信息，不返回密码。
- 图书管理：分页筛选、详情、新增、更新、逻辑删除、活动 ISBN 唯一；存在未归还记录时拒绝删除。
- 分类管理：一级分类 CRUD、活动名称唯一、在用分类禁止删除。
- 借书：30 天借期，条件 UPDATE 原子扣库存，事务内创建借阅记录。
- 还书：所有权校验，条件 UPDATE 防重复归还，事务内恢复库存。
- 借阅记录：用户查询自己的记录，管理员查询全部记录，支持状态和书名筛选。
- Redis 降级：缓存异常只记录日志，图书详情查询仍回源 MySQL。
- 统一响应：`Result<T>` 与 `PageVO<T>`，错误 HTTP Status 和响应 `code` 一致。

## 权限说明

| 能力 | 游客 | USER | ADMIN |
| --- | --- | --- | --- |
| 注册、登录 | 允许 | 允许 | 允许 |
| 查询图书、分类 | - | 允许 | 允许 |
| 查看 `/user/me` | - | 允许 | 允许 |
| 借书、归还自己的记录、查看自己的借阅记录 | - | 允许 | 允许 |
| 新增、修改、删除图书 | - | - | 允许 |
| 新增、修改、删除分类 | - | - | 允许 |
| 查询全部借阅记录、代用户还书 | - | - | 允许 |

普通注册接口的 DTO 不包含 `role`，并启用了未知 JSON 字段拒绝策略，因此不能通过注册请求创建 ADMIN。管理员由维护者在数据库中设置：

```sql
UPDATE users SET role = 'ADMIN' WHERE account = 'admin-account';
```

## 项目结构

```text
src/main/java/com/library/
├── common/                  泛型统一响应
├── config/                  Web、密码、时间、OpenAPI 和配置属性
├── controller/              用户、图书、分类、借阅接口
├── exception/               业务异常与全局异常响应
├── interceptor/             Bearer JWT 身份和角色拦截器
├── mapper/                  MyBatis Mapper
├── model/{dto,entity,vo}/   入参、数据库实体、出参
├── security/                JWT、当前用户上下文、角色注解
└── service/                 业务接口、实现与缓存失效协作
src/main/resources/
├── mapper/                  动态分页 SQL
└── application-*.yaml       公共、生产和本地示例配置
src/test/                    单元、Web、JWT、事务和并发测试
sql/                         全新建库和升级脚本
```

## 核心业务流程

### JWT 登录

1. 注册时使用 BCrypt 保存密码哈希，角色固定为 `USER`。
2. 登录成功后签发 HS256 JWT，默认有效期 12 小时。
3. 受保护接口必须传递 `Authorization: Bearer <JWT>`。
4. 拦截器验证签名与过期时间，将身份写入 `UserContext`。
5. `@RequireRole(ADMIN)` 接口在进入 Controller 前完成权限校验。
6. 请求完成后调用 `ThreadLocal.remove()`，避免线程池复用造成身份泄漏。

缺失、格式错误、空、非法或过期 Token 返回 HTTP 401；身份有效但权限不足返回 HTTP 403。

### 借书

1. 从 `UserContext` 取得当前用户。
2. 查询有效图书。
3. 执行原子 SQL：

   ```sql
   UPDATE books
   SET stock = stock - 1
   WHERE id = ? AND stock > 0 AND is_deleted = 0;
   ```

4. 受影响行数不是 1 时按库存不足处理。
5. 在同一事务中写入 `BORROWED` 借阅记录，`due_time = borrow_time + 30 天`。
6. 数据库提交成功后删除对应图书详情缓存。

条件 UPDATE 由数据库串行修改同一库存行；并发请求不能让库存变成负数。若记录插入失败，事务会回滚之前的库存扣减。

### 还书

1. 查询借阅记录；USER 只能操作自己的记录，ADMIN 可代处理。
2. 执行 `WHERE status = 'BORROWED'` 的条件 UPDATE，只有一个并发请求能把记录改为 `RETURNED`。
3. 更新成功后库存 `stock = stock + 1`。
4. 状态更新和库存恢复在同一事务中；任一步失败都会整体回滚。
5. 提交成功后删除图书详情缓存。

`OVERDUE` 不持久化。它由“当前时间晚于 `due_time` 且状态仍为 `BORROWED`”动态计算，避免每天批量同步一个容易过期的冗余状态。

## Redis 缓存策略

只缓存 `GET /book/{id}`，不缓存任意组合的分页搜索：

- 正常详情默认缓存 10 分钟。
- 不存在的 ID 使用空值标记缓存 1 分钟，降低缓存穿透。
- Redis 读、写、删失败时记录 warn，查询继续访问 MySQL。
- 图书更新、删除、借书和还书只在数据库事务提交成功后删除详情缓存。
- 分页条件组合多且失效范围大，因此不缓存；原来的无调用 `book:list` 路径已删除。

这是简单的 **Cache Aside + 最终一致性**，不是强一致缓存。事务提交与缓存删除之间、或 Redis 删除失败后的 TTL 窗口内可能短暂读到旧值；项目通过提交后删除、短 TTL 和数据库兜底收敛，没有引入分布式锁或消息队列。

## 数据库初始化与升级

- 全新数据库：执行 [`sql/schema.sql`](sql/schema.sql)。
- 从原始 `User` / `Book` 表迁移：备份并人工审核后执行 [`sql/migration-from-legacy.sql`](sql/migration-from-legacy.sql)。

仓库没有发布过可验证的“已具备 `users` / `books`、角色和活动 ISBN，但缺少分类借阅闭环”的数据库基线，因此不保留仅服务开发中间状态的 `upgrade_v2.sql`。迁移脚本不会由应用自动执行，也不会删除现有数据；它会为旧 `category_id` 创建“迁移分类-ID”占位分类，启动后可通过分类接口改名。

主要约束和索引：

- `users.account` 唯一。
- `books.active_isbn` 是生成列：有效图书等于 ISBN，逻辑删除图书为 NULL；唯一索引允许删除后重新录入同 ISBN。
- `categories.active_name` 用相同方式保证有效分类名称唯一。
- `books.category_id`、`borrow_records.user_id`、`book_id`、`status/due_time` 建立业务查询索引。
- 借阅表通过外键关联用户和图书，并约束借期及归还状态/时间的一致性。

## 配置与敏感信息

仓库不提交真实密码和 JWT 密钥。默认 profile 是 `local`，但真实的 `application-local.yaml` 已加入 `.gitignore`；首次使用需从示例复制。

| 环境变量 | 必需/默认 | 说明 |
| --- | --- | --- |
| `DB_URL` | prod 必需 | MySQL JDBC URL |
| `DB_USERNAME` | prod 必需 | 数据库用户 |
| `DB_PASSWORD` | prod 必需 | 数据库密码 |
| `REDIS_HOST` | prod 必需 | Redis 主机 |
| `REDIS_PORT` | prod 必需 | Redis 端口 |
| `REDIS_PASSWORD` | prod 必需 | Redis 密码 |
| `REDIS_CONNECT_TIMEOUT` | `2s` | Redis 建连超时 |
| `REDIS_TIMEOUT` | `2s` | Redis 命令超时 |
| `JWT_SECRET` | 必需 | UTF-8 后至少 32 字节的 HS256 密钥 |
| `JWT_EXPIRATION` | `12h` | Token 有效期 |
| `CORS_ALLOWED_ORIGINS` | 必需 | 逗号分隔的明确 http/https 来源，不允许 `*` |
| `BOOK_CACHE_TTL` | `10m` | 正常图书详情 TTL |
| `BOOK_NULL_CACHE_TTL` | `1m` | 空值标记 TTL |

JWT 密钥为空或不足 32 字节会在启动时给出明确错误。生产 profile 默认关闭 Knife4j、Swagger UI 和 OpenAPI JSON；MyBatis 不输出 SQL 到标准输出，本地仅将 Mapper 日志级别设为 debug。

## 本地启动

环境要求：JDK 21、Git；依赖可使用 Docker Desktop / Docker Compose，也可自行安装 MySQL 8 和 Redis。

### 使用 Docker Compose

PowerShell：

```powershell
Copy-Item .env.example .env
Copy-Item src\main\resources\application-local.example.yaml src\main\resources\application-local.yaml
docker compose up -d
Get-Content .env |
    Where-Object { $_ -and -not $_.TrimStart().StartsWith("#") -and $_.Contains("=") } |
    ForEach-Object {
        $parts = $_ -split "=", 2
        Set-Item -Path "Env:$($parts[0].Trim())" -Value $parts[1]
    }
.\mvnw.cmd spring-boot:run
```

Bash：

```bash
cp .env.example .env
cp src/main/resources/application-local.example.yaml src/main/resources/application-local.yaml
docker compose up -d
set -a && . ./.env && set +a
./mvnw spring-boot:run
```

Compose 首次创建 MySQL Volume 时自动执行 `sql/schema.sql`。已有 Volume 不会自动重放脚本，应按版本选择升级 SQL。

### 不使用 Docker

1. 启动 MySQL 8 和 Redis。
2. 在 MySQL 执行 `sql/schema.sql`。
3. 复制 `application-local.example.yaml` 为 `application-local.yaml`。
4. 设置数据库、Redis、JWT 和 CORS 配置。
5. 运行 `./mvnw spring-boot:run`；Windows 使用 `.\mvnw.cmd spring-boot:run`。

## API 清单

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/user/register` | 公开 | 注册 USER |
| POST | `/user/login` | 公开 | 登录并返回 Bearer Token |
| GET | `/user/me` | USER/ADMIN | 当前用户信息 |
| GET | `/book` | USER/ADMIN | 图书分页筛选 |
| GET | `/book/{id}` | USER/ADMIN | 图书详情 |
| POST | `/book` | ADMIN | 新增图书 |
| PUT | `/book/{id}` | ADMIN | 更新图书 |
| DELETE | `/book/{id}` | ADMIN | 逻辑删除图书；存在未归还记录时返回 409 |
| GET | `/category` | USER/ADMIN | 分类列表 |
| GET | `/category/{id}` | USER/ADMIN | 分类详情 |
| POST | `/category` | ADMIN | 新增分类 |
| PUT | `/category/{id}` | ADMIN | 更新分类 |
| DELETE | `/category/{id}` | ADMIN | 删除未被有效图书使用的分类 |
| POST | `/borrow/{bookId}` | USER/ADMIN | 借书 |
| POST | `/borrow/{recordId}/return` | USER/ADMIN | 还书；USER 仅限本人 |
| GET | `/borrow/my` | USER/ADMIN | 我的借阅记录 |
| GET | `/admin/borrow` | ADMIN | 全部借阅记录 |

图书分页参数为 `title`、`author`、`isbn`、`categoryId`、`page`、`size`；ISBN 精确匹配，书名和作者模糊匹配。借阅分页支持 `status`、`bookTitle`、`page`、`size`。页码从 1 开始，单页最多 100 条。

统一返回结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

登录后调用示例：

```bash
curl http://localhost:8080/book \
  -H "Authorization: Bearer eyJ..."
```

## API 文档

local profile 默认开放：

- Knife4j：<http://localhost:8080/doc.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

在 Knife4j 的 Authorize 中填写登录响应的 `accessToken`，文档会自动加 Bearer 前缀。生产 profile 默认关闭文档端点。

## 构建与测试

```powershell
.\mvnw.cmd -version
.\mvnw.cmd -B test
.\mvnw.cmd -B clean verify
```

Linux/macOS 将 `.\mvnw.cmd` 替换为 `./mvnw`。测试不依赖外部 MySQL 或 Redis：大部分逻辑使用 Mockito，事务回滚与并发库存使用测试作用域内的 H2。覆盖注册/登录/BCrypt、JWT、Bearer 边界、角色权限、OPTIONS/CORS、DTO 校验、ISBN 冲突、图书缓存降级、分类约束、借书/还书、重复与并发归还、越权归还、借阅中禁止删书、事务回滚，以及库存为 1 时的数据库级并发不超卖。

## 当前范围

本项目没有支付、罚款、预约、续借、复杂 RBAC、消息队列、Elasticsearch、微服务或前端。这些能力不是当前图书管理业务闭环的必要条件。
