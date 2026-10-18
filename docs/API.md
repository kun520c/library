# API 参考

开发环境 OpenAPI：<http://localhost:8080/doc.html>（local profile）。前端通过 Vite `/api` 代理访问以下后端原生路径。

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/user/register` | 公开 | 注册 USER |
| POST | `/user/login` | 公开 | 登录并返回 Bearer Token |
| GET | `/user/me` | USER/ADMIN | 当前用户信息 |
| PUT | `/user/me` | USER/ADMIN | 只修改当前用户的 `username` |
| GET | `/admin/users` | ADMIN | 用户分页查询，支持用户名、账号筛选 |
| GET | `/book` | USER/ADMIN | 图书分页筛选 |
| GET | `/book/{id}` | USER/ADMIN | 图书详情 |
| POST | `/book` | ADMIN | 新增图书 |
| PUT | `/book/{id}` | ADMIN | 更新基本信息，不接受 `stock` |
| PATCH | `/book/{id}/stock` | ADMIN | 使用 `delta` 原子增减库存 |
| DELETE | `/book/{id}` | ADMIN | 逻辑删除图书；存在未归还记录时返回 409 |
| GET | `/category` | USER/ADMIN | 分类列表 |
| GET | `/category/{id}` | USER/ADMIN | 分类详情 |
| POST | `/category` | ADMIN | 新增分类 |
| PUT | `/category/{id}` | ADMIN | 更新分类 |
| DELETE | `/category/{id}` | ADMIN | 删除未被有效图书使用的分类 |
| POST | `/borrow/{bookId}` | USER/ADMIN | 借书 |
| POST | `/borrow/{recordId}/return` | USER/ADMIN | 还书；USER 仅限本人 |
| GET | `/borrow/{recordId}` | USER/ADMIN | 借阅详情；USER 仅限本人，ADMIN 可查任意记录 |
| GET | `/borrow/my` | USER/ADMIN | 我的借阅记录 |
| GET | `/admin/borrow` | ADMIN | 全部借阅记录 |

图书分页参数为 `title`、`author`、`isbn`、`categoryId`、`page`、`size`；ISBN 精确匹配，书名和作者模糊匹配。用户分页参数为 `username`、`account`、`page`、`size`，两种文本条件均为模糊匹配，结果按 `id DESC` 稳定排序，且查询列不包含密码。新增图书 DTO 包含初始 `stock`，基本信息更新 DTO 不包含 `stock`，库存调整请求只包含 `delta`。借阅分页支持 `status`、`bookTitle`、`page`、`size`。所有页码从 1 开始，单页最多 100 条。

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
