# 本地演示手册

以下操作仅用于本地开发，不使用真实个人信息。没有内置管理员或固定生产密码。建议新建 Compose 项目与全新演示库，避免与已有业务数据混用。

## 1. 初始化环境

普通启动可直接使用 [README 快速开始](../README.md#快速开始)（含 Windows PowerShell 与 Linux/macOS bash 对照）。隔离 Smoke 下列步骤仍以 PowerShell 为例。bash 不要 `source` 或 `.` `.env.portfolio`；使用与 README 相同的 `import_local_env .env.portfolio`，并把 `.\mvnw.cmd` 换成 `./mvnw`。复现本轮 Smoke Test 时，在根目录准备独立环境文件；已有同名文件请保留并核对：

```powershell
if (!(Test-Path .env.portfolio)) { Copy-Item .env.example .env.portfolio }
```

编辑 `.env.portfolio`，至少修改这些值，其他项沿用示例：

```dotenv
MYSQL_DATABASE=library_portfolio
MYSQL_USER=portfolio
MYSQL_PORT=13307
REDIS_PORT=16379
DB_URL=jdbc:mysql://localhost:13307/library_portfolio?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
DB_USERNAME=portfolio
```

自行设置随机 `MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`；`DB_PASSWORD` 等于 `MYSQL_PASSWORD`。JWT 密钥至少 32 字节。随后：

```powershell
. ./scripts/Import-LocalEnv.ps1 -Path .env.portfolio
docker compose --env-file .env.portfolio -p library-portfolio config --quiet
docker compose --env-file .env.portfolio -p library-portfolio up -d --wait
$OutputEncoding = [Text.UTF8Encoding]::new($false)
Get-Content -Raw -Encoding utf8 sql/seed-demo.sql |
    docker compose --env-file .env.portfolio -p library-portfolio exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" "$MYSQL_DATABASE"'
# 避免意外使用已有 application-local.yaml 的私人连接配置。
$env:SPRING_PROFILES_ACTIVE = 'prod'
.\mvnw.cmd spring-boot:run
```

Linux / macOS 导入同一份 `.env.portfolio` 后再启动后端。`import_local_env` 与 README、配置说明中的函数相同，只赋值、不 source。前面的 `docker compose --env-file .env.portfolio -p library-portfolio` 命令在 bash 中同样适用；导入 seed 时把 PowerShell 管道换成 `docker compose --env-file .env.portfolio -p library-portfolio exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" "$MYSQL_DATABASE"' < sql/seed-demo.sql`。

```bash
# 与 scripts/Import-LocalEnv.ps1 相同：只解析普通 KEY=VALUE 并赋值。
# 不要 source 或 . 原始环境文件。
import_local_env() {
  local file="$1" line key val
  [[ -n "$file" && -f "$file" ]] || { echo "Environment file not found: ${file:-}" >&2; return 1; }
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ "$line" == *[![:space:]]* ]] || continue
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" || "$line" == '#'* ]] && continue
    if [[ "$line" != *=* ]]; then
      echo "Expected plain KEY=VALUE entries in the local environment file." >&2
      return 1
    fi
    key="${line%%=*}"
    val="${line#*=}"
    if [[ ! "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
      echo "Expected plain KEY=VALUE entries in the local environment file." >&2
      return 1
    fi
    export "$key=$val"
  done < "$file"
}
import_local_env .env.portfolio || exit 1
./mvnw spring-boot:run
```

另开终端执行 `cd frontend`、`npm ci`、`npm run dev -- --strictPort`，打开 <http://localhost:5173>。prod 只用于本次隔离演示，其文档端点关闭，不代表生产部署已验证。

`seed-demo.sql` 添加 3 个 `[演示]` 分类和 8 本虚构书目，ISBN 为 `DEMO-*` 标记；它不创建账号、不覆盖记录、不重置库存，可重复执行。请在本地应用停机时导入，避免与管理操作并发；初始化和 seed 均不是数据库迁移框架。

## 2. 创建普通用户

在登录页点击“注册读者账号”，填写演示用户名、账号（4–32 位字母、数字、点、下划线或连字符）和自行设置的密码。密码至少 6 个字符且 UTF-8 最多 72 字节。注册后登录进入书库。准备第二个普通用户用于验证越权，使用另一个浏览器会话，或退出后切换账号。

## 3. 建立管理员账号

先通过前端正常注册 `demo-admin`，再在**本地演示库**授权：

```powershell
docker compose --env-file .env.portfolio -p library-portfolio exec mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"'
```

在 MySQL 提示符执行，确认只匹配自己刚创建的账号：

```sql
SELECT id, account, role FROM users WHERE account = 'demo-admin';
UPDATE users SET role = 'ADMIN' WHERE account = 'demo-admin' AND role = 'USER';
```

退出客户端后用 `demo-admin` **重新登录**。角色保存在 JWT 快照中，授权前的 Token 不会变成 ADMIN；降权也不会立即撤销旧 Token。

## 4. 添加分类、图书与调整库存

管理员进入“分类管理”新增一个演示分类，再到“图书管理”新增图书，设置唯一 ISBN、价格、分类与初始库存 2。记录图书 ID。编辑基本信息不会提交库存；“调整库存”输入增量 `+1` 后，应显示库存 3，输入 `-1` 恢复 2。库存不足的负调整返回冲突，数据不变。

## 5–8. 借书、查库存、还书与恢复

1. 普通用户找到这本书，记下可借数量 N，点击书名进入详情，再点“借阅此书”并确认。
2. “我的借阅”应出现 `BORROWED` 记录，借期 30 天；记下记录 ID。返回图书详情，库存为 N−1。
3. 在“我的借阅”点击“归还”并确认，记录变为 `RETURNED`，详情库存恢复 N。
4. 使用相同记录 ID 再调用归还接口应得到 409，库存不再增加。已有未归还记录时，管理员删除图书应得到 409。

如果其他演示操作同时发生，库存以各次 delta 的净变化为准，不要把不同操作前后的缓存快照当成严格事务读。

## 9. 验证越权被拒绝

用第二个普通用户登录，直接访问第一个用户的 `/borrow/{recordId}` 页面，接口应返回 **403**。普通用户访问 `/admin` 会被前端守卫引导到书库；使用普通 Token 直接请求 `/admin/users` 也应返回 **403**。后端校验不能由前端隐藏按钮代替。

如需观察 HTTP 状态，可打开浏览器开发工具 Network。只在本地查看 Token，不要将其写入文档或截图。接口与权限表见 [API](API.md)。

## 可重复 HTTP Smoke Test

完成隔离启动后，在仓库根目录执行：

```powershell
./scripts/Test-Demo.ps1 -BaseUrl http://localhost:8080 -EnvFile .env.portfolio -ComposeProject library-portfolio -ConfirmIsolatedEnvironment
```

只有核对当前后端的启动配置确实来自同一隔离环境文件后，才传入 `-ConfirmIsolatedEnvironment`。不传该开关时，脚本会在读取配置和发送 HTTP 前退出。预检要求 localhost、专用 Compose 项目/库名、JDBC 与 MySQL 配置一致，并通过容器项目标签、实际映射端口和 `SELECT DATABASE()` 核对 Compose 目标。**这些检查仍不能自动证明正在运行的后端连接了该库**；该连接由操作者明确确认。无法确认时不要运行，先停止并以演示配置重新启动后端。HTTP Client 禁用代理和重定向，避免本地测试请求被转发到其他地址。每次创建随机账号、一个分类和图书，只提升本轮创建的管理员，不修改已有记录。演示数据保留供检查，重复运行会增加独立数据。覆盖 36 项断言：真实 HTTP 注册登录、角色、图书维护、缓存失效、10 请求争抢 1 本、2 请求并发归还、重复归还、所有权、借阅中删除限制与归还库存上界。它不是前端 E2E 或吞吐量测试。

## 截图复现

实际截图保存在 [images](images)：[登录](images/login.jpg)、[书库](images/library.jpg)、[详情](images/book-detail.jpg)、[管理概览](images/admin.jpg)、[借阅记录](images/borrows.jpg)。书库与借阅记录截图使用 1440×900 桌面视口，本轮在新隔离库重拍；书库选择 `[演示] 后端工程` 分类，借阅记录包含借出中与已归还状态。登录、详情、管理概览保留首轮实际截图并复核对应页面；管理概览为演示库实时查询，数量会随操作变化。截图不展示密码、Token、个人资料或真实业务数据。

## 停止本轮依赖

后端与前端各按 Ctrl+C；执行 `docker compose --env-file .env.portfolio -p library-portfolio stop` 停止隔离依赖。保留数据卷便于继续演示，不要误用 `down -v` 删除已有数据。
