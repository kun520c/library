# 测试与验证

## 自动测试范围

| 层次 | 测试位置 | 验证内容 |
| --- | --- | --- |
| 单元 / Mockito | [service](../src/test/java/com/library/service) | 注册与 BCrypt、资料与用户投影、分类约束、ISBN、缓存降级、借还书错误边界 |
| Web / MockMvc | [ApiSecurityWebTest](../src/test/java/com/library/controller/ApiSecurityWebTest.java) | 401/403、角色、DTO、分页/路径边界、未知字段、错误脱敏、UTF-8 密码限制；Service 被 Mock |
| JWT / 拦截器 | [security](../src/test/java/com/library/security) | 签名、过期、篡改、密钥长度、Bearer 语法与上下文清理 |
| 配置回归 | [LocalCorsConfigurationTest](../src/test/java/com/library/config/LocalCorsConfigurationTest.java) | 真实 YAML 按 local 优先顺序绑定 CORS 环境变量；空值拒绝 |
| H2 / Spring 事务 | [BorrowTransactionIntegrationTest](../src/test/java/com/library/service/BorrowTransactionIntegrationTest.java) | 扣减后插入失败回滚、10 请求不超卖、并发归还、借阅中删除限制、管理员 delta 与借书并发、库存上下界 |
| H2 / SQL 查询 | [UserManagementIntegrationTest](../src/test/java/com/library/service/UserManagementIntegrationTest.java) | 用户分页、安全列、资料更新；借阅详情 JOIN 另在事务测试覆盖 |
| 缓存提交时序 | [BookCacheInvalidatorTest](../src/test/java/com/library/service/BookCacheInvalidatorTest.java) | 提交前不删、提交后删除、Redis 删除失败兜底 |
| 日志脱敏 | [GlobalExceptionHandlerTest](../src/test/java/com/library/exception/GlobalExceptionHandlerTest.java) | 请求解析异常消息中的输入值不进入日志 |
| 文档端点 | [DocumentationAccessIntegrationTest](../src/test/java/com/library/controller/DocumentationAccessIntegrationTest.java) | 测试环境公开文档可访问 |

## 执行命令

```powershell
.\mvnw.cmd -version
.\mvnw.cmd -B clean verify
cd frontend
npm ci
npm run type-check
npm run lint
npm run build
npm audit --omit=dev
```

Linux 使用 `./mvnw`。后端自动测试不需要外部 MySQL/Redis，test profile 使用 H2 和 Mock。Surefire 明细输出到忽略的 `target/surefire-reports/`。如果 PATH 首项为 Java 8，先把 `JAVA_HOME` 设置为自己的 JDK 21 路径；不要把机器私人路径提交到配置。

[CI](../.github/workflows/ci.yml) 在 main push / pull_request 触发，使用 Java 21、Node 24，执行 Maven `clean verify`、`npm ci`、类型检查、ESLint、`build-only`；没有 Docker 或浏览器 E2E 步骤。README 徽章指向 main，不证明功能分支已经在远程通过。

## H2 与 MySQL 的边界

H2 使用 `MODE=MySQL` 和独立 [schema-test.sql](../src/test/resources/schema-test.sql)，并不执行完整 MySQL DDL。MySQL 的 UNSIGNED、ENUM、utf8mb4 排序规则、生成列、优化器、InnoDB 行锁与默认隔离级别都不能由 H2 完整模拟。H2 测试通过不等于 MySQL 生产兼容性、吞吐量或死锁行为已通过验证。

真实环境最小回归脚本是 [Test-Demo.ps1](../scripts/Test-Demo.ps1)，步骤见 [DEMO](DEMO.md)。它使用现有 HttpClient 与 PowerShell，没有为一次演示引入浏览器测试框架。浏览器端本轮实际操作验证，尚未建立独立自动化 E2E；后续若持续维护前端，再基于关键流程引入固定数据与浏览器 CI。

## 本次实际记录（2026-10-18，Asia/Shanghai）

环境：Windows、JDK 21.0.11、Maven Wrapper 3.9.15、Node 24.15.0、npm 11.12.1。独立 Compose 项目 `library-portfolio`，MySQL 8.4.11、Redis 7.4.11，数据库 `library_portfolio`，使用全新独立数据卷和随机本地凭据。没有连接工作区原有数据库。

| 检查 | 本地执行结果 |
| --- | --- |
| Maven `-B clean verify` | 110 项，110 通过，0 失败、0 错误、0 跳过；JAR 打包成功 |
| 前端 `npm ci` / type-check / lint / build | 通过；构建保留大于 500 kB 的 chunk 提示 |
| `docker compose config --quiet` | 通过；同时完成独立服务健康检查 |
| 自定义库名建表 | 真 MySQL 在 `library_portfolio` 建立四张表，未依赖固定 `USE library` |
| seed 重复执行 | 两次执行后仍为 3 分类、8 本书（Smoke 数据加入前） |
| 真实 HTTP Smoke | 36/36：10 次并发借书 1 成功 + 9 个 409；2 次并发归还 1 成功 + 1 个 409，库存恢复一次；跨用户查询/归还 403；库存上限归还 409 且状态回滚，管理员腾出容量后可归还 |
| 浏览器 | 运行真实前后端验证注册、登录、筛选、详情、借阅和归还、USER 页面守卫与 ADMIN 管理概览、图书管理页面；5 张实际截图 |
| npm 依赖审计 | 兼容更新 brace-expansion、shell-quote、source-map-js 后，生产依赖 0 项；完整审计仍有 4 项 high（同一 braces → micromatch → fast-glob → Vue ESLint 配置开发链），未做破坏性降级 |

本轮新增 6 项 JUnit 测试（CORS 2 项、UTF-8 Web 边界 2 项、日志脱敏 1 项、归还库存上界与事务回滚 1 项），没有删除原测试。脚本每次生成不同演示账号，日志仅输出断言标签与状态，不输出凭据。初次测试的沙箱 JVM 附加与 npm 目录权限问题在正常本地权限下解决；未把受阻尝试计为通过。

## 未覆盖项

- 没有压测、生产部署、浏览器跨版本/移动端矩阵、自动化视觉回归或完整前端 E2E。
- MySQL Smoke 覆盖关键请求与并发防重，没有在真实 MySQL 注入借阅写入失败；该回滚边界目前由 H2 集成测试验证。
- 旧库迁移未执行；缓存竞态、故障恢复与持续可靠失效没有完整自动化验证。
- 完整开发依赖审计非零，属于构建/检查工具链风险；没有把 `npm audit` 非零写成通过。

剩余问题与优先级见 [审查记录](REVIEW.md)。
