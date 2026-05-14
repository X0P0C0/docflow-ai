# DocFlow AI 开发接力文档

## 1. 这份文档的作用

这份文档是给“新对话 / 新接力开发者 / 后续 AI 协作”准备的。

它的目标是：

- 让新对话快速理解当前项目状态
- 避免每次开新对话都从头讲背景
- 让 AI 能按统一方向持续开发
- 尽量减少上下文丢失带来的重复沟通成本

一句话理解：

`这是 DocFlow AI 项目的接力开发说明书。`

---

## 2. 项目基础信息

### 2.1 项目名称

`DocFlow AI`

### 2.2 项目定位

一个面向团队内部协作的：

- AI 知识库系统
- 工单协同系统
- 前后端分离全栈项目

### 2.3 当前项目目标

这个项目的目标不是单纯做一个练手 demo，而是做一个：

- 可用于求职展示的旗舰项目
- 可覆盖 Java 面试高频知识点的代表作
- 可持续扩展的产品化系统

---

## 3. 当前技术栈

### 3.1 前端

- Vue 3
- TypeScript
- Vite
- Vue Router

### 3.2 后端

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL 8
- Redis
- Spring Security

### 3.3 本地环境说明

#### Java

旧项目环境仍然可能使用 JDK 8，但本项目后端必须使用：

`D:\develop\java\jdk-17`

#### Node

本项目前端当前应优先使用：

`C:\Program Files\nodejs`

推荐版本：

- Node.js 20

如果终端默认仍指向旧 Node，可在当前命令里临时切 PATH。

---

## 4. 关键文档位置

新对话接手前，建议优先阅读这些文档：

- 项目路线图：`D:\java\project\docflow-ai\docs\project-roadmap.md`
- 项目立项：`D:\java\project\docflow-ai\docs\project-init.md`
- UI 风格方案：`D:\java\project\docflow-ai\docs\ui-design-plan.md`
- 数据库设计：`D:\java\project\docflow-ai\docs\database-design-v1.md`
- 开发总大纲：`D:\java\project\docflow-ai\docs\development-outline.md`
- 当前接力文档：`D:\java\project\docflow-ai\docs\development-handoff.md`
- 运行模式说明：`D:\java\project\docflow-ai\docs\runtime-modes.md`

---

## 5. 当前目录结构

项目根目录：

`D:\java\project\docflow-ai`

主要结构：

```text
D:\java\project\docflow-ai
├─ frontend
├─ backend
├─ docs
├─ sql
└─ assets
```

说明：

- `frontend`：前端工程
- `backend`：后端工程
- `docs`：项目文档
- `sql`：数据库初始化脚本
- `assets`：静态原型和后续素材

---

## 6. 已完成的内容

以下内容截至当前对话已经完成。

### 6.1 文档层

已完成：

- 项目路线图
- 项目立项文档
- UI 风格方案
- 数据库设计第一版
- 开发接力文档

### 6.2 数据库层

已完成：

- 初始化 SQL 文件编写
- 本地 MySQL 已执行初始化脚本

SQL 文件位置：

`D:\java\project\docflow-ai\sql\init.sql`

数据库名称：

`docflow_ai`

### 6.3 后端层

已完成：

- Spring Boot 后端骨架搭建
- 基础配置文件
- 统一返回体
- 全局异常处理
- JWT 认证骨架
- 登录接口
- 当前用户接口
- 健康检查接口

已完成模块：

- 知识文章列表接口
- 知识文章详情接口
- 工单列表接口
- 工单详情接口
- 工单评论数据并入详情接口
- 工单处理记录细化并入详情接口
- 工单评论写入接口
- 工单状态更新接口
- 工单指派处理人接口
- 新建工单接口
- 工单列表筛选接口

### 6.4 前端层

已完成：

- Vue 3 前端骨架搭建
- 首页产品化样式落地
- 页面拆组件
- 路由系统接入
- 登录页
- 路由守卫
- token 持久化与恢复逻辑

已完成页面：

- 登录页
- 控制台首页
- 知识文章列表页
- 知识文章详情页
- 工单列表页
- 工单详情页
- 工单详情页评论区与处理记录区细化
- 工单详情页评论提交表单
- 工单详情页状态流转表单
- 工单详情页处理人指派表单
- 新建工单页
- 工单列表筛选表单

### 6.5 联调层

已完成：

- 知识文章接口联调
- 工单接口联调
- Vite 代理到后端 `/api`

---

## 7. 当前真实可用接口

后端本地地址：

`http://127.0.0.1:8080`

当前真实接口：

- 健康检查：`GET /api/health`
- 登录：`POST /api/auth/login`
- 当前用户：`GET /api/auth/me`
- 知识文章列表：`GET /api/knowledge/articles`
- 知识文章详情：`GET /api/knowledge/articles/{id}`
- 工单列表：`GET /api/tickets`
- 新建工单：`POST /api/tickets`
- 工单详情：`GET /api/tickets/{id}`
- 可分配处理人列表：`GET /api/tickets/assignees`
- 新增工单评论：`POST /api/tickets/{id}/comments`
- 更新工单状态：`POST /api/tickets/{id}/status`
- 指派处理人：`POST /api/tickets/{id}/assignee`

---

## 8. 当前可访问页面

前端本地地址通常为：

- `http://127.0.0.1:5173`
- 如果端口被占用，也可能是 `5174`

当前页面：

- 登录页：`/login`
- 首页：`/dashboard`
- 知识文章列表：`/knowledge/articles`
- 知识文章详情：`/knowledge/articles/1`
- 工单列表：`/tickets`
- 工单详情：`/tickets/1`

例如：

- `http://127.0.0.1:5173/login`
- `http://127.0.0.1:5173/dashboard`
- `http://127.0.0.1:5173/knowledge/articles`
- `http://127.0.0.1:5173/knowledge/articles/1`
- `http://127.0.0.1:5173/tickets`
- `http://127.0.0.1:5173/tickets/1`

---

## 9. 当前前后端关键文件

### 9.1 后端关键文件

- 启动类：`D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\DocflowAiBackendApplication.java`
- 配置文件：`D:\java\project\docflow-ai\backend\src\main\resources\application.yml`
- 健康检查：`D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\controller\HealthController.java`

认证模块：

- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\auth\controller\AuthController.java`
- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\auth\service\impl\AuthServiceImpl.java`
- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\auth\security\JwtAuthenticationFilter.java`

知识库模块：

- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\knowledge\controller\KnowledgeArticleController.java`
- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\knowledge\service\impl\KnowledgeArticleServiceImpl.java`

工单模块：

- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\ticket\controller\TicketController.java`
- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\ticket\service\impl\TicketServiceImpl.java`
- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\ticket\dto\TicketCommentResponse.java`
- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\ticket\entity\TicketComment.java`
- `D:\java\project\docflow-ai\backend\src\main\java\com\docflow\ai\ticket\mapper\TicketCommentMapper.java`

### 9.2 前端关键文件

- 应用入口：`D:\java\project\docflow-ai\frontend\src\App.vue`
- 前端入口：`D:\java\project\docflow-ai\frontend\src\main.ts`
- 路由：`D:\java\project\docflow-ai\frontend\src\router\index.ts`
- 全局样式：`D:\java\project\docflow-ai\frontend\src\style.css`

认证相关：

- `D:\java\project\docflow-ai\frontend\src\views\LoginView.vue`
- `D:\java\project\docflow-ai\frontend\src\auth.ts`
- `D:\java\project\docflow-ai\frontend\src\api\auth.ts`

首页：

- `D:\java\project\docflow-ai\frontend\src\views\DashboardView.vue`

知识库相关：

- `D:\java\project\docflow-ai\frontend\src\api\knowledge.ts`
- `D:\java\project\docflow-ai\frontend\src\views\KnowledgeArticleListView.vue`
- `D:\java\project\docflow-ai\frontend\src\views\KnowledgeArticleDetailView.vue`

工单相关：

- `D:\java\project\docflow-ai\frontend\src\api\ticket.ts`
- `D:\java\project\docflow-ai\frontend\src\views\TicketListView.vue`
- `D:\java\project\docflow-ai\frontend\src\views\TicketDetailView.vue`

---

## 10. 当前哪些是真实数据，哪些还是 mock

### 10.1 已接真实接口

- 登录页当前用户恢复
- 首页知识文章列表
- 知识文章列表页
- 知识文章详情页
- 首页工单列表
- 工单列表页
- 工单详情页
- 工单详情页评论区与处理时间线

### 10.2 仍然保留 mock 兜底的数据

前端目前采用的是：

`真实接口优先，失败时回退到本地 mock`

补充说明：

- 演示模式、联调模式、真实业务模式的边界，见 `D:\java\project\docflow-ai\docs\runtime-modes.md`

这样做的目的：

- 保证开发时页面不会空白
- 保证后端临时没启动时前端仍可展示
- 提高开发连贯性

但长期来看，应逐步把主要页面都改成真实接口驱动。

---

## 11. 当前已知问题

### 11.1 中文响应乱码

现象：

通过 `PowerShell Invoke-WebRequest` 查看接口时，中文会显示乱码。

说明：

这不一定代表前端页面里一定乱码，更多是当前命令行编码显示问题。

后续建议：

- 检查后端统一响应编码
- 检查前端页面实际显示
- 必要时补 `produces = "application/json;charset=UTF-8"` 或统一编码配置

### 11.2 登录与权限骨架已接入，但权限菜单仍未完整

目前已经完成：

- 登录接口
- 当前用户接口
- JWT 校验骨架
- 路由守卫
- 退出登录
- token 持久化与恢复

后续仍需补：

- 基于角色的菜单控制
- 当前用户信息展示完善
- 页面级权限差异化

### 11.3 工单主线已进入可写阶段，但闭环仍未完整

目前已经完成：

- 工单评论数据展示
- 工单处理记录时间线展示
- 工单详情页信息分区细化
- 工单评论写入
- 工单状态流转
- 工单指派处理人
- 新建工单
- 工单列表筛选

后续仍需补：

- 附件上传

### 11.4 Redis 业务能力还未真正接入

当前 Redis 仍主要停留在配置阶段。

后续应补：

- 缓存知识文章
- 热点数据缓存
- 登录态 / 验证码
- 缓存问题场景设计

---

## 12. 推荐的下一步开发顺序

接力开发时，建议优先按下面顺序推进：

1. 基础布局壳与菜单权限
2. 用户模块
3. Redis 实战接入
4. AI 摘要与 AI 建议能力接入
5. 通知模块

如果只选一个最优先的下一步：

`优先做用户模块基础信息展示与菜单权限`

原因：

- 评论写入、状态流转、处理人指派、新建工单、列表筛选已经接上
- 下一步最该补的是“不同身份进入系统后看到什么、能做什么”
- 这样项目就会从功能演示进一步走向真实产品权限模型

---

## 13. 新对话如何快速接力

下次开新对话时，推荐直接把下面这句话发给新的 AI：

```text
请先阅读 D:\java\project\docflow-ai\docs\development-handoff.md，再继续作为 DocFlow AI 项目的接力开发助手。当前项目根目录是 D:\java\project\docflow-ai，请优先基于已有文档、已有代码和当前开发进度继续推进，不要重新做一套新方案。
```

如果你希望它更明确一点，也可以发这个版本：

```text
请先阅读：
1. D:\java\project\docflow-ai\docs\development-handoff.md
2. D:\java\project\docflow-ai\docs\development-outline.md
3. D:\java\project\docflow-ai\docs\project-roadmap.md
4. D:\java\project\docflow-ai\docs\project-init.md
5. D:\java\project\docflow-ai\docs\ui-design-plan.md
6. D:\java\project\docflow-ai\docs\database-design-v1.md

然后继续接力开发 D:\java\project\docflow-ai。
要求：保持现有技术栈、现有 UI 风格和现有开发节奏，不要重做项目方向。
```

---

## 14. 对接力开发的硬性要求

后续新对话 / 新 AI 接手时，应该遵守这些规则：

- 不要推翻现有项目方向
- 不要改成别的技术栈
- 不要重新做一套 UI 风格
- 优先基于现有结构继续推进
- 改动前先看 `development-handoff.md`
- 文件路径尽量继续使用绝对路径说明
- 优先保持“真实接口优先，mock 兜底”的开发方式

---

## 15. 当前一句话总结

`DocFlow AI 已完成项目立项、UI 定调、数据库落地、登录与 JWT 基础骨架、知识文章列表/详情主线，以及工单创建、评论写入、状态流转、处理人指派和列表筛选闭环；下一阶段应优先补菜单权限、用户模块和 Redis 场景。`
