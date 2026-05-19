# DocFlow AI 开发交接文档

## 1. 文档目的

这份文档用于帮助后续维护者快速接手 `DocFlow AI`。

适用对象：

- 新接手仓库的开发者
- 继续推进本项目的后续 AI / 编码助手会话
- 需要快速了解当前实现状态的人

建议和下列文档配合阅读：

- [README.md](D:\java\project\docflow-ai\README.md)
- [frontend-delivery-demo-guide.md](D:\java\project\docflow-ai\docs\frontend-delivery-demo-guide.md)
- [frontend-project-closeout.md](D:\java\project\docflow-ai\docs\frontend-project-closeout.md)
- [goal-execution-roadmap.md](D:\java\project\docflow-ai\docs\goal-execution-roadmap.md)
- [runtime-modes.md](D:\java\project\docflow-ai\docs\runtime-modes.md)

## 2. 项目概览

`DocFlow AI` 是一个面向内部协同场景的全栈项目，当前围绕三条线展开：

- 工单工作流
- 知识库工作流
- AI 辅助工作台

当前技术栈：

- 前端：Vue 3、TypeScript、Vite、Vue Router
- 后端：Java 17、Spring Boot 3、MyBatis-Plus、MySQL 8、Redis

仓库根目录：

- `D:\java\project\docflow-ai`

## 3. 本地环境

前端：

- 推荐 Node：`20.x`
- 工作目录：`D:\java\project\docflow-ai\frontend`

后端：

- 必须使用 JDK：`17`
- 工作目录：`D:\java\project\docflow-ai\backend`

数据库：

- 默认库名：`docflow_ai`
- 初始化脚本：[sql/init.sql](D:\java\project\docflow-ai\sql\init.sql)

## 4. 启动方式

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

实际后端端口：

- `http://127.0.0.1:8081`

常见前端开发地址：

- `http://127.0.0.1:5173`

如果前端端口被占用，Vite 也可能切到 `5174` 等附近端口。

## 5. 初始化账号

- `admin / password`
- `support01 / password`
- `user01 / password`

如需完整演示或做主要路径验证，优先使用 `admin`。

## 6. 当前前端状态

前端已经进入可交付状态，不再是早期原型阶段。

已完成的基础层：

- 统一 `AppShell`、侧边导航和顶部区域
- 登录、鉴权持久化、路由守卫、能力限制跳转
- 真实后端模式与 demo fallback 模式区分
- 主要业务页统一为管理后台式结构
- 单测基线已恢复并通过
- 交付、演示、收尾文档已补齐

已完成的主要页面：

- `/login`
- `/dashboard`
- `/tickets`
- `/tickets/create`
- `/tickets/:id`
- `/knowledge/articles`
- `/knowledge/articles/create`
- `/knowledge/articles/:id`
- `/knowledge/articles/:id/edit`
- `/ai-center`
- `/notifications`
- `/profile`
- `/settings`

当前最清晰的前端主流程：

1. 登录
2. 进入 Dashboard
3. 进入工单列表和工单详情
4. 在工单上下文中处理问题
5. 从工单进入知识创建 / 编辑
6. 查看知识详情与来源上下文
7. 查看 AI Center 工作台概览

## 7. 当前后端状态

后端已具备的基础能力：

- 健康检查
- 登录
- 当前用户查询
- JWT 鉴权基础
- 工单列表 / 详情 / 创建 / 评论 / 状态 / 指派接口
- 知识文章列表 / 详情接口

已知接口示例：

- `GET /api/health`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/comments`
- `POST /api/tickets/{id}/status`
- `POST /api/tickets/{id}/assignee`
- `GET /api/knowledge/articles`
- `GET /api/knowledge/articles/{id}`

## 8. 当前路由参考

核心路由：

- `/login`
- `/dashboard`
- `/tickets`
- `/tickets/create`
- `/tickets/:id`
- `/knowledge/articles`
- `/knowledge/articles/create`
- `/knowledge/articles/:id`
- `/knowledge/articles/:id/edit`
- `/ai-center`

次级路由：

- `/notifications`
- `/profile`
- `/settings`

## 9. 关键文件

前端应用结构：

- [frontend/src/main.ts](D:\java\project\docflow-ai\frontend\src\main.ts)
- [frontend/src/App.vue](D:\java\project\docflow-ai\frontend\src\App.vue)
- [frontend/src/router/index.ts](D:\java\project\docflow-ai\frontend\src\router\index.ts)
- [frontend/src/style.css](D:\java\project\docflow-ai\frontend\src\style.css)

前端核心页面：

- [frontend/src/views/LoginView.vue](D:\java\project\docflow-ai\frontend\src\views\LoginView.vue)
- [frontend/src/views/DashboardView.vue](D:\java\project\docflow-ai\frontend\src\views\DashboardView.vue)
- [frontend/src/views/TicketListView.vue](D:\java\project\docflow-ai\frontend\src\views\TicketListView.vue)
- [frontend/src/views/TicketCreateView.vue](D:\java\project\docflow-ai\frontend\src\views\TicketCreateView.vue)
- [frontend/src/views/TicketDetailView.vue](D:\java\project\docflow-ai\frontend\src\views\TicketDetailView.vue)
- [frontend/src/views/KnowledgeArticleListView.vue](D:\java\project\docflow-ai\frontend\src\views\KnowledgeArticleListView.vue)
- [frontend/src/views/KnowledgeArticleEditorView.vue](D:\java\project\docflow-ai\frontend\src\views\KnowledgeArticleEditorView.vue)
- [frontend/src/views/KnowledgeArticleDetailView.vue](D:\java\project\docflow-ai\frontend\src\views\KnowledgeArticleDetailView.vue)
- [frontend/src/views/AiCenterView.vue](D:\java\project\docflow-ai\frontend\src\views\AiCenterView.vue)

前端 API / 鉴权辅助：

- [frontend/src/auth.ts](D:\java\project\docflow-ai\frontend\src\auth.ts)
- [frontend/src/api/auth.ts](D:\java\project\docflow-ai\frontend\src\api\auth.ts)
- [frontend/src/api/ticket.ts](D:\java\project\docflow-ai\frontend\src\api\ticket.ts)
- [frontend/src/api/knowledge.ts](D:\java\project\docflow-ai\frontend\src\api\knowledge.ts)

前端回归基线：

- [frontend/tests/unit](D:\java\project\docflow-ai\frontend\tests\unit)

后端入口：

- [backend/src/main/resources/application.yml](D:\java\project\docflow-ai\backend\src\main\resources\application.yml)
- [backend/src/main/java/com/docflow/ai/DocflowAiBackendApplication.java](D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\DocflowAiBackendApplication.java)

## 10. 运行模式

当前项目同时支持：

- 真实后端模式
- demo fallback 模式

理解方式：

- 只要真实 API 可用，就优先走真实接口
- 为了保证演示连续性，部分前端流程仍保留 fallback 行为

在排查模式相关问题前，建议先读 [runtime-modes.md](D:\java\project\docflow-ai\docs\runtime-modes.md)。

## 11. 验证基线

前端：

```bash
cd frontend
npm run test:unit
npm run build
```

当前收尾时点，这两条命令都已通过。

## 12. 剩余但不阻塞的工作

这些项不阻塞当前交付，但值得继续推进：

- 构建产物体积优化与拆包
- 继续清理剩余重复的 view 级 CSS
- 为次级路由接入更深的真实后端覆盖
- 在现有 AI Center 壳层上增加更真实的 AI 行为
- 继续产品化 notifications / profile / settings

## 13. 建议阅读顺序

如果后续有人继续接手，建议按这个顺序阅读：

1. [README.md](D:\java\project\docflow-ai\README.md)
2. [frontend-delivery-demo-guide.md](D:\java\project\docflow-ai\docs\frontend-delivery-demo-guide.md)
3. [frontend-project-closeout.md](D:\java\project\docflow-ai\docs\frontend-project-closeout.md)
4. [goal-execution-roadmap.md](D:\java\project\docflow-ai\docs\goal-execution-roadmap.md)
5. [runtime-modes.md](D:\java\project\docflow-ai\docs\runtime-modes.md)
6. [integration-verification-2026-05-19.md](D:\java\project\docflow-ai\docs\integration-verification-2026-05-19.md)

## 14. 一句话状态

`DocFlow AI 前端已经成为一个结构统一、可测试、可演示、可继续维护的管理台前端，当前重点已从基础重构转向优化、深化和增量演进。`
## 15. AI claim freshness configuration

The AI workspace claim freshness window is now backend-configurable.

- Source config: [application.yml](D:/java/project/docflow-ai/backend/src/main/resources/application.yml)
- Property key: `app.ai.claim-stale-after`
- Environment variable override: `DOCFLOW_AI_CLAIM_STALE_AFTER`
- Default: `2h`

Behavior summary:

- The backend derives `lastActivityAt` from the claim time plus real handling activity, such as operator comments, status or assignment records, and linked knowledge updates.
- The backend returns `claimFreshness` as `fresh` or `stale` based on the configured `app.ai.claim-stale-after` window.
- The frontend AI Center consumes the backend freshness value directly and shows a neutral state when that field is unavailable.

Common override examples:

- `45m`
- `90m`
- `2h`
- `4h`
