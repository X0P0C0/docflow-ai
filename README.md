# DocFlow AI

> AI 驱动的智能客服工单平台 - 企业级全栈项目

## 项目简介

DocFlow AI 是一个融合 AI 能力的企业级工单管理系统，面向 IT 运维/客服团队，提供从工单创建、智能分析、处理到知识沉淀的完整闭环。

核心差异化：AI 不是噱头，而是深度融入工单生命周期的每一个环节。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3 + MyBatis-Plus + MySQL 8 + Redis |
| 前端 | Vue 3 + TypeScript + Element Plus (vue-pure-admin) |
| AI | 意图识别 + 情感分析 + 智能路由 + 自动回复 + 对话摘要 |
| 消息队列 | Redis Pub/Sub + 死信队列 + 消息去重 + Stream 持久化 |
| 可观测性 | 结构化日志 + TraceId 全链路追踪 + Prometheus + 自定义指标 |
| 高可用 | Resilience4j 熔断器 + 指数退避重试 + 限流 |
| 安全 | JWT 认证 + RBAC 权限 + 接口限流 + 操作审计 |

## 系统架构

`
┌─────────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Element Plus)               │
│  Dashboard │ Ticket List │ Knowledge │ AI Center │ Portal   │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      API Gateway 层                          │
│              限流 │ 认证 │ 路由 │ 审计日志                    │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                    后端 (Spring Boot 3)                      │
│  工单服务 │ AI 分析 │ 知识库 │ SLA │ 通知 │ 系统管理          │
│                                                             │
│  设计模式: 观察者 │ 责任链 │ 策略 │ 模板方法 │ 状态机 │ 建造者 │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      中间件层                                │
│              MySQL 8 │ Redis │ WebSocket                    │
└─────────────────────────────────────────────────────────────┘
`

## 核心功能

### 工单管理
- 创建、查询、详情、评论、状态流转
- 转派、升级、合并、关联
- 批量状态更新、批量指派、CSV 导出
- 满意度评价（1-5 星）
- 附件上传

### AI 能力
- 意图识别：自动分析工单类型（登录问题/支付问题/性能问题等）
- 情感分析：检测用户情绪（积极/消极/中性）
- 智能路由：基于负载和技能的自动分配建议
- 自动生成回复草稿，支持采纳/取消
- 对话摘要：自动总结工单对话历史
- 知识推荐：基于工单内容推荐相关知识库文章

### 知识库
- 文章 CRUD（Markdown 编辑器）
- 文章评分和推荐
- 工单自动沉淀为知识库草稿
- 版本管理

### SLA 管理
- 策略配置（响应时间/解决时间/优先级）
- 自动截止时间计算
- 超时检测

### 系统管理
- 用户/角色/部门管理
- 自动化规则配置
- Webhook 配置
- 系统配置管理
- 健康检查和监控

## 项目结构

`
docflow-ai/
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/docflow/ai/
│       ├── ai/                 # AI 分析服务
│       ├── auth/               # 认证授权（JWT + RBAC）
│       ├── common/             # 通用组件
│       │   ├── cache/          # 多级缓存（L1 + L2）
│       │   ├── chain/          # 责任链模式
│       │   ├── observer/       # 观察者模式
│       │   ├── pattern/        # 消息队列 + 限流 + Saga
│       │   ├── resilience/     # 熔断器 + 重试
│       │   ├── security/       # XSS/CSRF + OAuth2
│       │   └── observability/  # 追踪 + 指标 + 告警
│       ├── knowledge/          # 知识库
│       ├── ticket/             # 工单核心（状态机 + 策略模式）
│       ├── customer/           # 客户门户
│       ├── notification/       # 通知服务
│       ├── monitoring/         # 业务指标
│       ├── system/             # 系统管理
│       └── websocket/          # WebSocket 实时通知
├── frontend/                   # Vue 3 前端（vue-pure-admin）
│   └── src/
│       ├── api/                # API 调用层
│       ├── views/docflow/      # 业务页面
│       └── router/             # 路由配置
├── docs/                       # 项目文档
├── scripts/                    # 工具脚本
├── sql/                        # 数据库脚本
├── docker-compose.yml          # Docker 编排
└── DESIGN.md                   # 设计系统文档
`

## 快速启动

### 环境要求

- JDK 17
- Maven 3.8+
- Node 22（推荐使用 fnm 管理）
- pnpm 11+
- MySQL 8
- Redis

### 一键启动（推荐）

`atch
# 首次使用：安装依赖
scripts\setup.bat

# 启动所有服务
scripts\start-all.bat

# 停止所有服务
scripts\stop-all.bat
`

### 手动启动

`atch
# 1. 初始化数据库
mysql -u root -p < sql\init.sql

# 2. 启动后端
cd backend
set JAVA_HOME=D:\develop\java\jdk-17
mvn spring-boot:run

# 3. 启动前端（新终端）
cd frontend
pnpm install
pnpm dev
`

### Docker 部署

`ash
docker-compose up -d
`

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:8848 |
| 后端 API | http://localhost:8081 |
| Swagger 文档 | http://localhost:8081/swagger-ui.html |
| 健康检查 | http://localhost:8081/api/health |

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | user1 | 123456 |

## 测试

`ash
# 后端测试
cd backend
set JAVA_HOME=D:\develop\java\jdk-17
mvn test

# 前端类型检查
cd frontend
pnpm typecheck

# 前端构建
pnpm build
`

## 技术完成度

| 类别 | 完成 | 总数 | 进度 |
|------|------|------|------|
| 设计模式 | 8 | 8 | 100% |
| 测试 | 6 | 6 | 100% |
| 消息队列 | 6 | 6 | 100% |
| 缓存策略 | 7 | 7 | 100% |
| 并发与事务 | 7 | 7 | 100% |
| 分布式系统 | 8 | 8 | 100% |
| 数据库 | 6 | 6 | 100% |
| 安全 | 8 | 10 | 80% |
| 可观测性 | 5 | 6 | 83% |
| 代码质量 | 5 | 6 | 83% |
| 文件与存储 | 3 | 4 | 75% |
| 性能优化 | 6 | 8 | 75% |
| **总计** | **89** | **92** | **97%** |

## API 概览

| 模块 | 端点 | 说明 |
|------|------|------|
| 认证 | POST /api/auth/login | 登录获取 Token |
| 工单 | GET /api/tickets | 工单列表（分页+筛选） |
| 工单 | POST /api/tickets | 创建工单 |
| 工单 | GET /api/tickets/{id} | 工单详情 |
| 工单 | POST /api/tickets/{id}/comments | 添加评论 |
| 知识 | GET /api/knowledge/articles | 知识文章列表 |
| 知识 | POST /api/knowledge/articles | 创建文章 |
| AI | GET /api/ai/workspace | AI 工作台 |
| AI | POST /api/ai/analysis | AI 分析工单 |
| 客户 | POST /api/customer/login | 客户登录 |
| 通知 | GET /api/notifications | 通知列表 |
| 系统 | GET /api/health | 健康检查 |

## 项目文档

- [部署指南](docs/deployment.md)
- [开发交接文档](docs/development-handoff.md)
- [项目目标事件](docs/project-goal-events.md)
- [项目收尾计划](docs/project-closeout-plan.md)
- [最终验收报告](docs/project-final-acceptance-report.md)
- [设计系统](DESIGN.md)

## License

MIT