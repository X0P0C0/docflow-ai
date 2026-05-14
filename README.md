# DocFlow AI

DocFlow AI 是一个把知识库、工单协作和 AI 辅助能力放在同一工作台里的全栈练习项目。当前仓库已经整理到适合本地直接联调的状态：前端支持演示模式，后端支持真实登录、知识文章查询和工单主流程接口。

## 快速开始

### 1. 初始化数据库

执行 [sql/init.sql](D:\java\project\docflow-ai\sql\init.sql)。

如果你是在已有库上继续升级，而不是重建整库，可以按需补执行：

- [sql/alter_kb_article_add_source_ticket.sql](D:\java\project\docflow-ai\sql\alter_kb_article_add_source_ticket.sql)
- [sql/alter_seed_user_passwords.sql](D:\java\project\docflow-ai\sql\alter_seed_user_passwords.sql)

默认会创建数据库 `docflow_ai`，并写入最小可联调测试数据，包括 3 个账号、角色权限、知识文章和 2 条初始工单。

测试账号：

- `admin / password`
- `support01 / password`
- `user01 / password`

如果你的数据库是在密码统一之前初始化的，真实后端可能仍然要求旧密码 `123456`。这种情况下先执行 [sql/alter_seed_user_passwords.sql](D:\java\project\docflow-ai\sql\alter_seed_user_passwords.sql)。

### 2. 启动后端

后端默认端口是 `8081`。Spring Boot 3.3 需要 `Java 17`。

```bash
cd backend
mvn spring-boot:run
```

常用环境变量：

```bash
DOCFLOW_DB_URL=jdbc:mysql://localhost:3306/docflow_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DOCFLOW_DB_USERNAME=root
DOCFLOW_DB_PASSWORD=123456
DOCFLOW_REDIS_HOST=localhost
DOCFLOW_REDIS_PORT=6379
DOCFLOW_JWT_SECRET=change-this-secret-in-dev-change-this-secret
```

健康检查：

- `GET http://127.0.0.1:8081/api/health`
- `GET http://127.0.0.1:8081/swagger-ui.html`

### 3. 启动前端

前端开发服务器默认端口通常是 `5173`，并通过 Vite 代理把 `/api` 转发到 `http://127.0.0.1:8081`。

```bash
cd frontend
npm install
npm run dev
```

## 推荐联调路径

1. 使用 `admin / password` 登录。
2. 新建一条工单。
3. 返回工单列表确认新工单已出现。
4. 进入详情页补评论、指派处理人、修改状态。
5. 返回 Dashboard 确认统计卡片和工单面板同步变化。

如果后端暂时不可用，登录页会自动回退到演示模式，方便继续查看前端交互；如果后端可用，则会直接走真实接口。

## 当前已打通的能力

- 登录与当前用户查询
- 基于角色和权限的前端入口控制
- 工单列表、详情、新建、评论、指派、状态流转
- 从工单生成知识草稿
- 知识文章列表、详情、编辑、版本记录
- 工单与知识文章的来源关联展示
- Dashboard 对真实工单和本地工单的合并展示

## 已知边界

- Redis 当前主要为后续能力扩展预留，主流程暂不依赖 Redis 数据结构。
- 部分前端页面仍保留演示模式回退逻辑，用于后端不可用时继续展示交互。
- SQL 脚本包含比当前接口更多的基础表结构，便于后续继续扩展。

## 本地开发环境约定

- 前端开发统一使用 `Node 20 LTS`
- 推荐使用 `fnm` 管理多版本 Node
- 后端使用 `JDK 17`
- 一键启动脚本见 [scripts/start-all.bat](D:\java\project\docflow-ai\scripts\start-all.bat)
