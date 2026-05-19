# DocFlow AI 数据库设计 V1

## 设计目标

数据库设计需要支撑以下核心场景：

- 用户登录与权限区分
- 工单创建、评论、指派、状态流转
- 知识文章创建、编辑、版本管理
- 工单与知识之间的来源关联

## 核心实体

### 1. 用户与权限

- `user`
- `role`
- `permission` 或能力映射
- `user_role`
- `role_permission`

### 2. 工单域

- `ticket`
- `ticket_comment`
- `ticket_activity`
- `ticket_assignee_history`

### 3. 知识域

- `kb_article`
- `kb_article_version`
- `kb_article_draft`

### 4. 关联关系

- `kb_article.source_ticket_id`
- 或独立的工单与知识关联表

## 关键字段建议

工单表建议至少包含：

- 标题
- 描述
- 状态
- 优先级
- 创建人
- 当前处理人
- 创建时间
- 更新时间

知识文章建议至少包含：

- 标题
- 摘要
- 正文
- 状态
- 来源工单
- 当前版本号
- 创建时间
- 更新时间

## 审计字段

建议所有核心业务表统一保留：

- `created_by`
- `created_at`
- `updated_by`
- `updated_at`
- 视情况保留逻辑删除字段

## 当前实现结论

当前仓库已经有：

- 初始化 SQL
- 基础表结构
- 审计字段自动填充闭环

但仍建议继续补强：

- 索引策略说明
- 唯一约束说明
- 数据库迁移版本化机制

## 后续建议

- 从单一 `init.sql` 过渡到版本化迁移
- 补业务实体关系图
- 明确状态流转与版本策略
