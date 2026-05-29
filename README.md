# 图书管理系统

基于 Spring Boot 3 + Redis + MySQL 的图书管理后台系统。

## 技术栈

| 技术 | 用途 |
|------|------|
| Spring Boot 3.3.5 | 应用框架 |
| MyBatis 3.0.3 | 数据库 ORM |
| MySQL | 数据库 |
| Redis | 列表缓存 |
| Spring Security Crypto | BCrypt 密码加密 |
| JWT (jjwt 0.11.5) | 用户认证 |
| Knife4j 4.5.0 | API 接口文档 |

## 功能

- 图书 CRUD：增删改查 + 分页条件搜索 + Redis 缓存
- 用户模块：注册（BCrypt 加密）+ 登录（JWT 令牌）
- 逻辑删除
- Knife4j 在线接口文档

## 快速启动

1. 安装 MySQL 和 Redis
2. 创建数据库 library，执行建表 SQL
3. 修改 `application-local.yaml` 中的数据库和 Redis 连接信息
4. 启动项目：`mvn spring-boot:run`
5. 访问接口文档：http://localhost:8080/doc.html

## API

| 方法 | URL | 说明 |
|------|-----|------|
| GET | /book | 查询图书（支持 ?title=&author= 条件搜索） |
| GET | /book/{id} | 按 ID 查询 |
| POST | /book | 新增图书 |
| PUT | /book/{id} | 更新图书 |
| DELETE | /book/{id} | 逻辑删除 |
| POST | /user/register | 注册 |
| POST | /user/login | 登录（返回 JWT Token） |

## 项目结构

```
com.library/
├── common/          通用类（Result 统一返回体）
├── config/          配置（CORS、Swagger、拦截器注册）
├── controller/      REST 接口层
├── exception/       全局异常处理
├── interceptor/     JWT 登录拦截器
├── mapper/          MyBatis 数据访问层
├── model/
│   ├── entity/      数据库实体
│   ├── dto/         入参 DTO
│   └── vo/          出参 VO
├── service/         业务接口 + 实现
└── utils/           工具类（JWT、密码加密）
```
