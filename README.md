# DocFlow AI

DocFlow AI 是一个围绕工单协同、知识沉淀和 AI 辅助工作台构建的全栈项目。

当前仓库已经整理到适合本地直接联调、演示和继续维护的状态：前端支持真实后端模式与 demo fallback 模式，后端已具备登录、知识查询和工单主流程接口。

## 快速开始

### 1. 初始化数据库

执行 [sql/init.sql](D:\java\project\docflow-ai\sql\init.sql)。

如果是在已有库上继续升级，而不是全量重建，可按需补执行：

- [sql/alter_kb_article_add_source_ticket.sql](D:\java\project\docflow-ai\sql\alter_kb_article_add_source_ticket.sql)
- [sql/alter_seed_user_passwords.sql](D:\java\project\docflow-ai\sql\alter_seed_user_passwords.sql)

默认会创建数据库 `docflow_ai`，并写入最小联调数据，包括测试账号、角色权限、知识文章和初始化工单。

测试账号：

- `admin / password`
- `support01 / password`
- `user01 / password`

如果数据库是在密码统一前初始化的，真实后端可能仍要求旧密码 `123456`。这种情况下先执行 [sql/alter_seed_user_passwords.sql](D:\java\project\docflow-ai\sql\alter_seed_user_passwords.sql)。

### 2. 启动后端

后端默认端口是 `8081`，Spring Boot 3 需要 `Java 17`。

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

如果在 Windows PowerShell 里看到中文乱码，先执行：

```powershell
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
chcp 65001 > $null
```

更完整的说明见 [docs/deployment.md](D:\java\project\docflow-ai\docs\deployment.md)。

## 推荐联调路径

1. 使用 `admin / password` 登录
2. 新建一条工单
3. 返回工单列表确认新工单出现
4. 进入详情页补评论、指派处理人、修改状态
5. 从工单进入知识沉淀路径
6. 返回 Dashboard 确认概览信息同步变化

如果后端暂时不可用，登录页会自动回退到 demo fallback，方便继续查看前端交互；如果后端可用，则直接走真实接口。

## 当前已打通的能力

- 登录与当前用户查询
- 基于角色与能力的前端入口控制
- 工单列表、详情、新建、评论、指派、状态流转
- 从工单进入知识草稿沉淀
- 知识文章列表、详情、编辑、版本相关展示
- 工单与知识文章的来源关联展示
- Dashboard 对真实工单与本地展示数据的统一承接

## 交付与演示

- 前端交付 / 演示指引：[docs/frontend-delivery-demo-guide.md](D:\java\project\docflow-ai\docs\frontend-delivery-demo-guide.md)
- 前端项目收尾结论：[docs/frontend-project-closeout.md](D:\java\project\docflow-ai\docs\frontend-project-closeout.md)
- 当前版本最终验收结论：[docs/project-final-acceptance-report.md](D:\java\project\docflow-ai\docs\project-final-acceptance-report.md)
- 开发交接文档：[docs/development-handoff.md](D:\java\project\docflow-ai\docs\development-handoff.md)
- 改造目标执行路线：[docs/goal-execution-roadmap.md](D:\java\project\docflow-ai\docs\goal-execution-roadmap.md)
- 运行模式说明：[docs/runtime-modes.md](D:\java\project\docflow-ai\docs\runtime-modes.md)

## 已知边界

- Redis 当前主要为后续能力预留，主流程暂不依赖 Redis 数据结构
- 部分前端页面仍保留 fallback 逻辑，用于后端不可用时继续演示
- SQL 脚本包含比当前接口更多的基础表结构，便于后续扩展

## 本地开发环境约定

- 前端统一使用 `Node 20 LTS`
- 推荐使用 `fnm` 管理多版本 Node
- 后端使用 `JDK 17`
- 一键启动脚本见 [scripts/start-all.bat](D:\java\project\docflow-ai\scripts\start-all.bat)
