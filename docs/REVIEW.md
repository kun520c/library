# 求职展示专项审查记录

审查基线：`3a3d4103e88825ff036fb95a841fec4b8f2f11fc`（`main`），仓库 `kun520c/library`。2026-10-18 在干净工作区创建 `codex/library-portfolio-polish`，保留业务与分层结构。没有改写历史、修改真实业务数据或覆盖私人连接配置。

## 分级发现与处理

| 级别 | 发现 | 处理与证据 |
| --- | --- | --- |
| P0 / P1 | 本轮审查范围内未发现确定性的认证绕过、越权读写或并发超卖问题 | 对拦截器、所有权、条件 UPDATE、事务、SQL 参数绑定、DTO 与安全 VO 进行了代码审阅与现有回归；不是全面安全认证 |
| P2 | local 示例固定 CORS 列表覆盖公共环境变量 | 删除 profile 列表；新增 2 项绑定回归，已有私人副本需手动同步 |
| P2 | schema 固定创建/切换 `library`，与 Compose 可配置库名冲突 | 改为选库后建表；隔离 MySQL 自定义 `library_portfolio` 初始化通过 |
| P2 | 管理员把可借库存调到 Integer 上限后还书可能产生 Java 无法读取的值 | 还书 SQL 增加上界/有效状态条件；失败返回 409 并回滚状态；H2 与真实 MySQL 均验证调整容量后可正常归还 |
| P2 | 密码字符长度校验不能表达 BCrypt 的 UTF-8 72 字节限制 | 注册与登录增加字节校验，前端注册同步提示；24 个汉字接受、25 个拒绝（400），Service 不被调用 |
| P2 | 解析异常日志直接记录底层消息，可能带入请求输入值 | 仅记录异常类型；新增日志捕获回归，不记录示例敏感值 |
| P2 | npm audit 初始报告 7 项（开发链含 critical） | 锁文件兼容更新 3 包，生产依赖审计 0；开发链仍 4 项 high，保留记录，未使用 force 降级 |
| P3 | POM 空许可证、开发者、SCM 等占位元数据 | 删除空项，补真实仓库地址与描述，不虚构许可证 |
| P3 | 缺少演示 seed、截图、独立技术文档，首页信息过长 | 新增演示资源与文档，README 改为展示入口；链接、图与真实启动均检查 |
| P3 | `.env` 变体和日志容易误入库，About 缺少信息 | 扩展忽略规则，保留示例；远程 Description 与 8 个 Topics 已按任务更新 |

P2 剩余边界：JWT 角色修改不即时撤销旧 Token；Cache-Aside 允许 TTL 内旧值；没有登录限流或复杂权限模型。它们已在 [设计决策](DESIGN_DECISIONS.md) 写明，没有将当前教学项目包装成生产系统。完整开发依赖审计仍非零。P3 剩余项：前端大 chunk 提示、无独立浏览器自动化 E2E。

## 文件级交付

| 类别 | 文件 | 目的 |
| --- | --- | --- |
| 工程修复 | [pom.xml](../pom.xml)、[.gitignore](../.gitignore)、[local 示例](../src/main/resources/application-local.example.yaml)、[schema.sql](../sql/schema.sql) | 清理元数据与配置、初始化库名一致性 |
| 安全与边界 | [LoginDTO](../src/main/java/com/library/model/dto/LoginDTO.java)、[RegisterDTO](../src/main/java/com/library/model/dto/RegisterDTO.java)、[Utf8Length](../src/main/java/com/library/validation/Utf8Length.java)、[Validator](../src/main/java/com/library/validation/Utf8LengthValidator.java)、[异常处理](../src/main/java/com/library/exception/GlobalExceptionHandler.java)、[BookMapper](../src/main/java/com/library/mapper/BookMapper.java)、[BorrowServiceImpl](../src/main/java/com/library/service/impl/BorrowServiceImpl.java) | 字节限制、日志脱敏、归还上界与回滚 |
| 前端 | [注册页](../frontend/src/views/auth/RegisterView.vue)、[package-lock.json](../frontend/package-lock.json) | 同步密码提示、兼容依赖修复；保留既有视觉设计 |
| 首页 | [README.md](../README.md) | 定位、真实截图、四项源码入口、启动与验证边界 |
| 文档 | [ARCHITECTURE](ARCHITECTURE.md)、[DESIGN_DECISIONS](DESIGN_DECISIONS.md)、[DEMO](DEMO.md)、[TESTING](TESTING.md)、[CONFIGURATION](CONFIGURATION.md)、[API](API.md)、本记录 | 把架构、取舍、操作与证据从首页拆出 |
| 演示资源 | [seed-demo.sql](../sql/seed-demo.sql)、[Import-LocalEnv.ps1](../scripts/Import-LocalEnv.ps1)、[Test-Demo.ps1](../scripts/Test-Demo.ps1)、[5 张 JPG](images) | 可重复书目、PowerShell 启动、36 项真实 HTTP 检查、真实界面 |
| 测试 | [LocalCorsConfigurationTest](../src/test/java/com/library/config/LocalCorsConfigurationTest.java)、[ApiSecurityWebTest](../src/test/java/com/library/controller/ApiSecurityWebTest.java)、[GlobalExceptionHandlerTest](../src/test/java/com/library/exception/GlobalExceptionHandlerTest.java)、[BorrowTransactionIntegrationTest](../src/test/java/com/library/service/BorrowTransactionIntegrationTest.java) | 新增 6 项 JUnit 回归，不删除已有测试 |

## 验证证据与限制

最终 Maven 110/110，0 失败、0 错误、0 跳过；前端 npm ci、类型检查、Lint、Build 通过。真实 MySQL/Redis HTTP 36/36、seed 重复执行与自定义库名初始化通过，5 张图来自真实 Vue 前端；4 张 Mermaid 使用本地渲染器实际渲染。详细命令、环境与未验证内容见 [TESTING](TESTING.md)。

已读取基线远程 CI：[2026-09-08 success](https://github.com/kun520c/library/actions/runs/34191017247)，对应基线 SHA；该结果不代表本轮新提交已通过远程 CI，功能分支以 PR 的最新检查为准。没有性能、用户规模、生产运行或真实旧库迁移验证。

远程 About Description：基于 Spring Boot 3 与 Vue 3 的图书管理系统，包含 JWT 权限认证、MySQL 事务、并发库存控制和 Redis 缓存。

Topics：`java`、`spring-boot`、`mybatis`、`mysql`、`redis`、`vue3`、`typescript`、`library-management`。仓库名称与可见性保持原样。

## 面试官视角

首页可以快速辨认业务闭环与技术入口，代码组织保留单体分层，亮点能定位到条件 SQL、事务和权限代码。演示手册、seed 与本地 HTTP 脚本能支撑复现，截图有真实数据来源。现有质量适合展示本科生的后端工程实践，但不能据此宣称生产级安全、强一致缓存或高并发性能；开发依赖告警、Token 生命周期和前端自动化仍是可以继续讨论的局限。
