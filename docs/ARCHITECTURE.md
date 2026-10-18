# 系统架构

## 组件与调用关系

```mermaid
flowchart TB
    Browser[浏览器 Vue 3] -->|开发环境 /api 代理| Vite[Vite 5173]
    Vite -->|移除 /api 前缀| MVC[Spring MVC 8080]
    MVC --> Auth[AuthenticationInterceptor]
    Auth --> Controllers[Controller]
    Controllers --> Services[Service 事务与业务规则]
    Services --> Mappers[MyBatis Mapper]
    Mappers --> MySQL[(MySQL)]
    Services -->|仅 BookService 详情读写| Redis[(Redis)]
    Services --> Invalidator[BookCacheInvalidator]
    Invalidator -->|事务提交后删除| Redis
```

生产构建输出静态文件，由部署者配置 Web Server 和 `/api` 反向代理；Compose 当前只提供 MySQL 和 Redis，没有封装应用部署。没有网关、独立认证服务或消息系统。

## 分层职责

| 位置 | 职责与实际边界 |
| --- | --- |
| `frontend/src/api`、`stores`、`router`、`views` | Axios 调用、身份恢复、页面守卫、真实接口数据展示；权限最终由后端执行 |
| [Controller](../src/main/java/com/library/controller) | HTTP 路由、DTO 校验、Result 包装、管理员注解 |
| [Service](../src/main/java/com/library/service/impl) | 事务、所有权、分类/ISBN/借阅约束；Book 管缓存，Borrow 管借还 |
| [Mapper](../src/main/java/com/library/mapper) / [XML](../src/main/resources/mapper) | 参数化 SQL、分页、条件更新、必要行锁 |
| [DTO / Entity / VO](../src/main/java/com/library/model) | 输入白名单 / 数据库映射 / 输出契约；管理员用户分页直接投影安全 VO |
| [security](../src/main/java/com/library/security)、[interceptor](../src/main/java/com/library/interceptor) | JWT 签发解析、当前用户上下文、角色注解 |
| [config](../src/main/java/com/library/config)、[exception](../src/main/java/com/library/exception) | 类型化配置、CORS、时钟、密码编码、异常 HTTP 状态与脱敏 |

## 表关系

```mermaid
erDiagram
    users ||--o{ borrow_records : borrows
    books ||--o{ borrow_records : recorded_in
    categories o|--o{ books : classifies
    users {
        int id PK
        string account UK
        string password "BCrypt hash"
        string role "USER or ADMIN"
    }
    categories {
        int id PK
        string active_name UK "generated; deleted rows NULL"
        boolean is_deleted
    }
    books {
        int id PK
        string active_isbn UK "generated; deleted rows NULL"
        int category_id FK "nullable"
        int stock
        boolean is_deleted
    }
    borrow_records {
        bigint id PK
        int user_id FK
        int book_id FK
        datetime borrow_time
        datetime due_time
        datetime return_time "nullable until returned"
        string status "BORROWED or RETURNED"
    }
```

完整 DDL 见 [schema.sql](../sql/schema.sql)。分类与图书逻辑删除，借阅保留历史关联。账号唯一；生成列保证活动 ISBN 和活动分类名称唯一。外键约束关联存在，CHECK 约束借期与归还状态/时间，索引覆盖活动图书、分类和用户/图书/状态借阅查询。`overdue` 由未归还状态与当前时间计算，不持久化。

## JWT 请求流程

```mermaid
sequenceDiagram
    participant UI as Vue / Axios
    participant Auth as AuthenticationInterceptor
    participant JWT as JwtService
    participant C as Controller
    participant S as Service
    UI->>C: POST /user/login（公开）
    C->>S: login(LoginDTO)
    S->>S: 查询账号，BCrypt 校验密码
    S->>JWT: generateToken(user)
    JWT-->>S: Bearer Token
    S-->>C: LoginVO
    C-->>UI: Result 包装的 Token
    UI->>Auth: 受保护请求 + Authorization
    Auth->>JWT: 验签、过期校验、提取身份
    Auth->>Auth: UserContext.set + RequireRole 校验
    Auth->>C: 校验成功后执行请求
    C->>S: 业务调用（借阅检查所有权）
    S-->>C: 业务结果
    C-->>UI: Result 响应
    Auth->>Auth: afterCompletion 清理 ThreadLocal
```

未认证返回 401，已认证但角色/所有权不符返回 403。OPTIONS 放行，CORS 来源仍由 WebConfig 限制；登录、注册、错误页和开发文档不经过认证。prod 关闭文档映射。JWT 中的角色与用户名是签发快照，详见 [权限边界](DESIGN_DECISIONS.md#8-jwt-与角色权限)。

## 借还书事务

```mermaid
flowchart TD
    Borrow[借书：当前用户与有效图书] --> Dec{条件扣减影响 1 行?}
    Dec -->|否| Conflict[409，事务回滚]
    Dec -->|是| Insert[插入 BORROWED 记录，借期 30 天]
    Insert --> Commit[事务提交]
    Return[还书：查记录并校验本人或 ADMIN] --> Mark{BORROWED 条件更新影响 1 行?}
    Mark -->|否| Conflict
    Mark -->|是| Inc[库存加 1]
    Inc --> Commit
    Insert -->|失败| Rollback[异常回滚全部数据库修改]
    Inc -->|失败| Rollback
    Commit --> Evict[afterCommit 删除对应图书详情缓存]
```

图书删除先锁定图书行，再检查未归还记录；分类维护先锁定分类行，阻止并发新增/修改图书引用已删除分类。管理员编辑基本信息不更新库存，库存接口使用受范围约束的 delta SQL。并发测试位置见 [TESTING](TESTING.md)。
