# DocFlow AI 数据库设计 V1

## 1. 这份文档的作用

这份文档是项目第一版数据库设计说明。

它的目标不是一步到位把所有表设计到完美，而是先把 MVP 阶段最核心的业务对象定下来。

这样后面做接口、写代码、画页面的时候，就不会一直飘。

当前原则：

- 先满足 MVP
- 字段命名尽量规范
- 尽量贴近真实业务
- 后面允许逐步演进，不追求一开始就极度复杂

---

## 2. 当前数据库范围

第一版主要覆盖这几块：

- 用户与权限
- 知识库
- 工单系统
- 评论与协作
- 通知中心
- AI 调用记录

数据库建议：

- `MySQL 8`

字符集建议：

- `utf8mb4`

---

## 3. 通用字段设计约定

后面大部分表，建议统一带上这些字段：

- `id`：主键
- `create_time`：创建时间
- `update_time`：更新时间
- `create_by`：创建人 ID
- `update_by`：更新人 ID
- `deleted`：逻辑删除标记

为什么这样设计：

- 方便审计
- 方便后面做后台管理
- 方便排查是谁创建和修改的数据

建议：

- 主键先用 `bigint`
- 时间类型使用 `datetime`
- `deleted` 用 `tinyint`

---

## 4. 用户与权限模块

这一块是整个系统的基础。

### 4.1 用户表 `sys_user`

作用：

保存系统用户基础信息。

核心字段建议：

- `id`
- `username`：登录用户名
- `password`：登录密码（加密后）
- `nickname`：昵称
- `real_name`：真实姓名
- `email`
- `phone`
- `avatar`
- `status`：状态，1 启用，0 禁用
- `dept_id`：所属部门 ID
- `last_login_time`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

说明：

- `username` 要唯一
- 密码一定不能明文保存
- `status` 用于停用账号

### 4.2 角色表 `sys_role`

作用：

定义系统里的角色。

建议字段：

- `id`
- `role_name`：角色名称
- `role_code`：角色编码，例如 `ADMIN`、`SUPPORT`、`USER`
- `description`
- `status`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

### 4.3 权限表 `sys_permission`

作用：

定义系统里的权限点。

建议字段：

- `id`
- `permission_name`
- `permission_code`
- `permission_type`：菜单、按钮、接口权限等
- `parent_id`
- `path`
- `component`
- `icon`
- `sort_order`
- `status`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

说明：

- 如果做前端动态菜单，这张表很有用
- `parent_id` 可以支持菜单树结构

### 4.4 用户角色关系表 `sys_user_role`

作用：

表示用户和角色是多对多关系。

建议字段：

- `id`
- `user_id`
- `role_id`
- `create_time`

### 4.5 角色权限关系表 `sys_role_permission`

作用：

表示角色和权限是多对多关系。

建议字段：

- `id`
- `role_id`
- `permission_id`
- `create_time`

### 4.6 部门表 `sys_dept`

作用：

表示部门或团队结构。

建议字段：

- `id`
- `dept_name`
- `parent_id`
- `leader_user_id`
- `sort_order`
- `status`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

---

## 5. 知识库模块

知识库是项目的核心模块之一。

### 5.1 知识分类表 `kb_category`

作用：

保存知识文章分类。

建议字段：

- `id`
- `name`
- `parent_id`
- `sort_order`
- `status`
- `description`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

### 5.2 知识标签表 `kb_tag`

作用：

保存标签信息。

建议字段：

- `id`
- `tag_name`
- `tag_color`
- `description`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

### 5.3 知识文章表 `kb_article`

作用：

保存知识文章主信息。

建议字段：

- `id`
- `title`
- `summary`：文章摘要
- `content`：文章正文，建议 longtext
- `category_id`
- `author_user_id`
- `status`：草稿、已发布、已归档
- `view_count`
- `like_count`
- `collect_count`
- `publish_time`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

说明：

- `content` 可以先直接存数据库
- 后期如果想更复杂，可以拆版本表

### 5.4 文章标签关系表 `kb_article_tag`

作用：

表示文章和标签的多对多关系。

建议字段：

- `id`
- `article_id`
- `tag_id`
- `create_time`

### 5.5 文章版本表 `kb_article_version`

作用：

保存文章历史版本，便于做编辑记录。

建议字段：

- `id`
- `article_id`
- `version_no`
- `title`
- `summary`
- `content`
- `operator_user_id`
- `remark`
- `create_time`

说明：

- MVP 阶段可以先建表，功能后面再完善

---

## 6. 工单模块

工单模块是另一个核心模块。

### 6.1 工单表 `ticket`

作用：

保存工单主信息。

建议字段：

- `id`
- `ticket_no`：工单编号
- `title`
- `content`：问题描述
- `type`：工单类型
- `priority`：优先级，例如低、中、高、紧急
- `status`：新建、处理中、已解决、已关闭
- `submit_user_id`：提交人
- `assignee_user_id`：当前处理人
- `dept_id`：所属部门
- `source`：来源，例如 Web、移动端
- `expected_finish_time`
- `actual_finish_time`
- `close_time`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

说明：

- `ticket_no` 建议唯一
- `status` 和 `priority` 后面建议定义枚举

### 6.2 工单处理记录表 `ticket_record`

作用：

记录工单的每一次流转和处理动作。

建议字段：

- `id`
- `ticket_id`
- `operator_user_id`
- `action_type`：创建、分配、更新状态、关闭等
- `old_status`
- `new_status`
- `remark`
- `create_time`

为什么重要：

- 后面做工单详情页时间线时会用到
- 面试时也能体现系统的可追踪性

### 6.3 工单评论表 `ticket_comment`

作用：

保存工单下的评论和沟通记录。

建议字段：

- `id`
- `ticket_id`
- `user_id`
- `content`
- `comment_type`：普通评论、处理说明、系统备注
- `is_internal`：是否内部可见
- `create_time`
- `update_time`
- `deleted`

### 6.4 工单附件表 `ticket_attachment`

作用：

保存工单附件信息。

建议字段：

- `id`
- `ticket_id`
- `file_name`
- `file_url`
- `file_size`
- `file_type`
- `upload_user_id`
- `create_time`
- `deleted`

### 6.5 工单分类表 `ticket_category`

作用：

保存工单分类。

建议字段：

- `id`
- `name`
- `parent_id`
- `description`
- `status`
- `sort_order`
- `create_time`
- `update_time`
- `create_by`
- `update_by`
- `deleted`

说明：

- 工单表里后面可以加 `category_id`
- 第一版如果想简单，也可以先只保留 `type`

---

## 7. 通知模块

### 7.1 通知表 `notification`

作用：

保存站内通知。

建议字段：

- `id`
- `title`
- `content`
- `notification_type`
- `receiver_user_id`
- `related_business_type`：关联业务类型，例如工单、文章
- `related_business_id`
- `is_read`
- `read_time`
- `create_time`
- `deleted`

使用场景：

- 工单被分配时通知用户
- 工单状态变化时通知提交人
- 文章更新后通知相关人员

---

## 8. AI 模块

AI 模块第一版不需要太复杂，但建议预留记录表。

### 8.1 AI 调用记录表 `ai_task_log`

作用：

记录 AI 的调用历史，方便排查和统计。

建议字段：

- `id`
- `task_type`：摘要、分类、回复建议等
- `business_type`：文章、工单等
- `business_id`
- `input_text`
- `output_text`
- `status`：成功、失败
- `error_message`
- `model_name`
- `token_usage`
- `response_time_ms`
- `create_time`

为什么建议建这张表：

- 方便排查 AI 结果问题
- 方便统计 AI 使用情况
- 面试时能体现工程化思维

---

## 9. 表关系理解

你可以先这样理解这些关系：

### 9.1 用户和角色

- 一个用户可以有多个角色
- 一个角色也可以分给多个用户
- 所以是多对多关系

### 9.2 角色和权限

- 一个角色可以对应多个权限
- 一个权限也可以属于多个角色
- 也是多对多关系

### 9.3 文章和标签

- 一篇文章可以有多个标签
- 一个标签也可以用于多篇文章
- 也是多对多关系

### 9.4 用户和工单

- 一个用户可以提交很多工单
- 一个用户也可以处理很多工单
- 一个工单通常有一个当前负责人

### 9.5 工单和评论

- 一个工单会有多条评论
- 一条评论只属于一个工单
- 这是典型的一对多关系

---

## 10. 第一版核心表清单

如果只看 MVP，当前最核心的表是这些：

### 用户权限相关

- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
- `sys_dept`

### 知识库相关

- `kb_category`
- `kb_tag`
- `kb_article`
- `kb_article_tag`
- `kb_article_version`

### 工单相关

- `ticket`
- `ticket_record`
- `ticket_comment`
- `ticket_attachment`
- `ticket_category`

### 其他相关

- `notification`
- `ai_task_log`

---

## 11. 现在先不做得太复杂的点

为了保证项目推进速度，以下内容第一版先不做复杂化：

- 多租户字段体系
- 组织树的复杂权限继承
- 文章协同编辑冲突处理
- 工单审批流引擎
- 消息中心的多渠道推送
- AI 多模型路由

这不是不重要，而是当前阶段不应该一开始就把战线拉得太长。

---

## 12. 下一步怎么接这份设计

这份数据库设计定下来之后，下一步建议这样衔接：

1. 输出数据库建表 SQL 初稿
2. 初始化后端 Spring Boot 工程
3. 先实现用户、知识文章、工单这三块的基础实体
4. 再逐步做接口和前端页面

---

## 13. 当前结论

数据库第一版的设计目标已经明确：

- 先服务 MVP
- 先覆盖核心业务闭环
- 先保证表关系清晰
- 不追求一开始设计得过度复杂

后面你只要记住一句话：

`数据库设计不是一次性拍脑袋定死的，而是随着业务理解不断演进的。`
