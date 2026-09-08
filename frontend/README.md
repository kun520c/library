# 一方书室 Web Client

Vue 3 + Vite + TypeScript 前端，直接连接仓库中的 Spring Boot REST API。生产代码不包含 mock 数据或 fake API。

## 技术栈

- Vue 3、Vue Router、Pinia
- Axios 统一 API Client
- Element Plus 与 Element Plus Icons
- TypeScript、ESLint、Vite

视觉采用克制的现代编辑式图书产品风格：暖纸色背景、森林绿主色、宋体式标题、低圆角和 1px 分割线。图书没有后端封面字段，因此 `BookCover` 根据图书 ID、分类、书名和作者生成稳定的 CSS 书封，不请求占位图片。

## 安装与启动

要求 Node.js 24+、npm 11+。先在仓库根目录启动后端，再启动前端：

```powershell
cd frontend
npm ci
npm run dev
```

开发地址为 <http://localhost:5173>。Vite 默认把 `/api` 代理到 <http://localhost:8080>，无需修改后端接口路径。

## 环境变量

复制 `.env.example` 为 `.env.local` 后可覆盖：

```text
VITE_API_BASE_URL=/api
```

部署到前后端不同域名时，可以把该值设为完整后端地址；后端的 `CORS_ALLOWED_ORIGINS` 需要明确包含前端来源。若保持 `/api`，生产 Web Server 需要反向代理到 Spring Boot。

## 页面与权限

普通用户：登录、注册、书库、图书详情、借阅/归还、借阅详情和个人资料。

管理员：在普通用户能力之外，提供真实数据管理概览、图书 CRUD、库存增量调整、分类 CRUD、用户分页查询、全部借阅查询和代还。

应用启动时若本地存在 Token，会调用 `GET /user/me` 恢复身份。路由守卫在页面渲染前拦截游客和非管理员；401 会清理 Token 并回到登录页，其他业务错误显示后端安全 `message`。

## 质量检查与构建

```powershell
npm run type-check
npm run lint
npm run build
```

生产文件输出到 `frontend/dist/`，该目录不提交 Git。

## 联调提示

- 注册接口只能创建 `USER`。管理员账号由后端维护者在数据库中授予 `ADMIN`，不要把管理员密码写入仓库。
- 编辑图书不会携带 `stock`；库存只调用 `PATCH /book/{id}/stock` 并发送 `delta`。
- 用户名修改后前端会重新调用 `/user/me`，不依赖旧 JWT 中可能滞后的 username claim。
- 后端没有封面、简介、评分、出版社、头像、邮箱、罚款或续借字段，前端也不会虚构这些数据。
