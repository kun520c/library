# 配置与数据库

Spring Boot 不会自动读取根目录 `.env`。Compose 用它做变量替换；运行应用前还需把其中的变量导入当前 shell。PowerShell 执行 `. ./scripts/Import-LocalEnv.ps1`；bash 可用 `set -a; . ./.env; set +a`（本批未新增独立 bash 脚本）。导入逻辑只接受示例中的普通 `KEY=VALUE`，不执行 shell 表达式或自动去除引号。

| 环境变量 | 默认 / 要求 | 作用 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | 生产显式使用 `prod` |
| `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD` | Compose 必填 | 首次创建库和用户；仅首次初始化生效 |
| `MYSQL_PORT` | Compose `3306`；示例 `3307` | MySQL 宿主机映射端口 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | prod 必填 | JDBC 连接；需与上述库名、用户和端口同步 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | prod 必填 | Redis 连接与 Compose 端口、密码保持一致 |
| `REDIS_CONNECT_TIMEOUT` / `REDIS_TIMEOUT` | `2s` / `2s` | 连接与命令超时 |
| `JWT_SECRET` / `JWT_EXPIRATION` | UTF-8 至少 32 字节 / `12h` | HS256 签名与有效期；示例密钥仅供本地替换 |
| `CORS_ALLOWED_ORIGINS` | 必填，逗号分隔 | 明确 http/https 来源，如 `http://localhost:5173`，不使用通配符 |
| `BOOK_CACHE_TTL` / `BOOK_NULL_CACHE_TTL` | `10m` / `1m` | 详情与空值缓存 TTL |
| `VITE_API_BASE_URL` | `/api` | 构建时前端 API 地址；Vite 开发代理在 8080 |

local 示例提供数据库和 Redis 的开发默认值，真实 `application-local.yaml` 被 Git 忽略。生产配置没有密码或密钥默认值，并关闭 Knife4j、Swagger UI 与 OpenAPI。生产应设置独立随机凭据、HTTPS 和网络访问控制；本地示例密码不是生产凭据。8080 和 5173 分别是后端与前端端口；前端 `--strictPort` 避免静默换端口后与 CORS 配置不符。

## CORS 的配置优先级

公共 [application.yaml](../src/main/resources/application.yaml) 用 `${CORS_ALLOWED_ORIGINS}` 绑定列表。profile 中的索引列表会替换公共列表，因此新版 [local 示例](../src/main/resources/application-local.example.yaml) 不再定义它。[LocalCorsConfigurationTest](../src/test/java/com/library/config/LocalCorsConfigurationTest.java) 按 local 高于公共配置的顺序加载仓库文件，断言环境来源保留、空来源启动失败。它刻意不加载开发者私人的 local 文件；已有副本需自行同步。

## 无 Docker 的全新 MySQL

只在自己的本地全新库执行。先用有建库权限的账号创建目标库，再选库运行 [schema.sql](../sql/schema.sql)。脚本只建表，不再固定创建或切换到 `library`，兼容 Compose 的 `MYSQL_DATABASE`。

```powershell
mysql -u root -p -e 'CREATE DATABASE IF NOT EXISTS library CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;'
# SOURCE 路径相对于启动 mysql 时的仓库目录。
mysql --default-character-set=utf8mb4 -u root -p --database=library
```

在 MySQL 客户端执行：

```sql
SOURCE sql/schema.sql;
SOURCE sql/seed-demo.sql;
```

单独启动 Redis，设置 `.env` 中的连接和密码，复制 local 示例后导入环境变量，再运行 `.\mvnw.cmd spring-boot:run`（Linux/macOS 为 `./mvnw spring-boot:run`）。不要用 Windows PowerShell 的 `<` 进行标准输入文件重定向；bash 可用 `< sql/seed-demo.sql` 这类重定向。

## 已有库与旧表迁移

Compose 已有 Volume 不重放初始化脚本。`schema.sql` 中的 `IF NOT EXISTS` 也不会升级已有表结构，禁止把全新安装脚本当作迁移工具。

从原始 `User` / `Book` 表迁移时，先备份、检查重复账号与活动 ISBN，再人工审核 [migration-from-legacy.sql](../sql/migration-from-legacy.sql)。该历史迁移参考仍显式 `USE library`，自定义库名必须在受控副本中修改目标；它不是自动升级流程，不能在已迁移库重复执行。本轮未在真实旧库上执行迁移。

`.env*`（保留 `.env.example`）、私人 local YAML、构建输出、依赖目录与日志不入库。不要提交本地连接配置，也不要对现有业务库运行演示测试。
