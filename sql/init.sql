CREATE DATABASE IF NOT EXISTS `docflow_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `docflow_ai`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `ai_task_log`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `ticket_attachment`;
DROP TABLE IF EXISTS `ticket_comment`;
DROP TABLE IF EXISTS `ticket_record`;
DROP TABLE IF EXISTS `ticket_category`;
DROP TABLE IF EXISTS `ticket`;
DROP TABLE IF EXISTS `kb_article_version`;
DROP TABLE IF EXISTS `kb_article_tag`;
DROP TABLE IF EXISTS `kb_article`;
DROP TABLE IF EXISTS `kb_tag`;
DROP TABLE IF EXISTS `kb_category`;
DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_dept`;

CREATE TABLE `sys_dept` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID',
  `leader_user_id` BIGINT DEFAULT NULL COMMENT '负责人用户ID',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_leader_user_id` (`leader_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `permission_type` TINYINT NOT NULL COMMENT '权限类型：1菜单 2按钮 3接口',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID',
  `path` VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关系表';

CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关系表';

CREATE TABLE `kb_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识分类表';

CREATE TABLE `kb_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tag_name` VARCHAR(64) NOT NULL COMMENT '标签名称',
  `tag_color` VARCHAR(32) DEFAULT NULL COMMENT '标签颜色',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '标签描述',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识标签表';

CREATE TABLE `kb_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
  `content` LONGTEXT NOT NULL COMMENT '文章正文',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `source_ticket_id` BIGINT DEFAULT NULL COMMENT '来源工单ID',
  `author_user_id` BIGINT NOT NULL COMMENT '作者用户ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0草稿 1已发布 2已归档',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `collect_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_source_ticket_id` (`source_ticket_id`),
  KEY `idx_author_user_id` (`author_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文章表';

CREATE TABLE `kb_article_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关系表';

CREATE TABLE `kb_article_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` BIGINT NOT NULL COMMENT '文章ID',
  `version_no` INT NOT NULL COMMENT '版本号',
  `title` VARCHAR(200) NOT NULL COMMENT '历史标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '历史摘要',
  `content` LONGTEXT NOT NULL COMMENT '历史正文',
  `operator_user_id` BIGINT NOT NULL COMMENT '操作人ID',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '版本说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_version` (`article_id`, `version_no`),
  KEY `idx_operator_user_id` (`operator_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章版本表';

CREATE TABLE `ticket_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序值',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单分类表';

CREATE TABLE `ticket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_no` VARCHAR(64) NOT NULL COMMENT '工单编号',
  `title` VARCHAR(200) NOT NULL COMMENT '工单标题',
  `content` TEXT NOT NULL COMMENT '问题描述',
  `type` VARCHAR(64) DEFAULT NULL COMMENT '工单类型',
  `category_id` BIGINT DEFAULT NULL COMMENT '工单分类ID',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1低 2中 3高 4紧急',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1新建 2处理中 3已解决 4已关闭',
  `submit_user_id` BIGINT NOT NULL COMMENT '提交人ID',
  `assignee_user_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
  `source` VARCHAR(32) DEFAULT 'WEB' COMMENT '来源',
  `expected_finish_time` DATETIME DEFAULT NULL COMMENT '期望完成时间',
  `actual_finish_time` DATETIME DEFAULT NULL COMMENT '实际完成时间',
  `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_submit_user_id` (`submit_user_id`),
  KEY `idx_assignee_user_id` (`assignee_user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

CREATE TABLE `ticket_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` BIGINT NOT NULL COMMENT '工单ID',
  `operator_user_id` BIGINT NOT NULL COMMENT '操作人ID',
  `action_type` VARCHAR(64) NOT NULL COMMENT '动作类型',
  `old_status` TINYINT DEFAULT NULL COMMENT '原状态',
  `new_status` TINYINT DEFAULT NULL COMMENT '新状态',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_operator_user_id` (`operator_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单处理记录表';

CREATE TABLE `ticket_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` BIGINT NOT NULL COMMENT '工单ID',
  `user_id` BIGINT NOT NULL COMMENT '评论人ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `comment_type` TINYINT NOT NULL DEFAULT 1 COMMENT '评论类型：1普通评论 2处理说明 3系统备注',
  `is_internal` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内部可见：0否 1是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单评论表';

CREATE TABLE `ticket_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` BIGINT NOT NULL COMMENT '工单ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件地址',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小，单位字节',
  `file_type` VARCHAR(64) DEFAULT NULL COMMENT '文件类型',
  `upload_user_id` BIGINT NOT NULL COMMENT '上传人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_upload_user_id` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单附件表';

CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` VARCHAR(1000) NOT NULL COMMENT '通知内容',
  `notification_type` VARCHAR(64) NOT NULL COMMENT '通知类型',
  `receiver_user_id` BIGINT NOT NULL COMMENT '接收人ID',
  `related_business_type` VARCHAR(64) DEFAULT NULL COMMENT '关联业务类型',
  `related_business_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0否 1是',
  `read_time` DATETIME DEFAULT NULL COMMENT '已读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_user_id` (`receiver_user_id`),
  KEY `idx_related_business` (`related_business_type`, `related_business_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

CREATE TABLE `ai_task_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_type` VARCHAR(64) NOT NULL COMMENT '任务类型',
  `business_type` VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
  `business_id` BIGINT DEFAULT NULL COMMENT '业务ID',
  `input_text` LONGTEXT COMMENT '输入内容',
  `output_text` LONGTEXT COMMENT '输出内容',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1成功 0失败',
  `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `token_usage` INT DEFAULT NULL COMMENT 'token消耗',
  `response_time_ms` INT DEFAULT NULL COMMENT '响应耗时毫秒',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_business` (`business_type`, `business_id`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI任务日志表';

ALTER TABLE `sys_user`
  ADD CONSTRAINT `fk_sys_user_dept_id` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`);

ALTER TABLE `sys_user_role`
  ADD CONSTRAINT `fk_sys_user_role_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  ADD CONSTRAINT `fk_sys_user_role_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`);

ALTER TABLE `sys_role_permission`
  ADD CONSTRAINT `fk_sys_role_permission_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  ADD CONSTRAINT `fk_sys_role_permission_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`);

ALTER TABLE `kb_article`
  ADD CONSTRAINT `fk_kb_article_category_id` FOREIGN KEY (`category_id`) REFERENCES `kb_category` (`id`),
  ADD CONSTRAINT `fk_kb_article_author_user_id` FOREIGN KEY (`author_user_id`) REFERENCES `sys_user` (`id`);

ALTER TABLE `kb_article_tag`
  ADD CONSTRAINT `fk_kb_article_tag_article_id` FOREIGN KEY (`article_id`) REFERENCES `kb_article` (`id`),
  ADD CONSTRAINT `fk_kb_article_tag_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `kb_tag` (`id`);

ALTER TABLE `kb_article_version`
  ADD CONSTRAINT `fk_kb_article_version_article_id` FOREIGN KEY (`article_id`) REFERENCES `kb_article` (`id`),
  ADD CONSTRAINT `fk_kb_article_version_operator_user_id` FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`);

ALTER TABLE `ticket`
  ADD CONSTRAINT `fk_ticket_category_id` FOREIGN KEY (`category_id`) REFERENCES `ticket_category` (`id`),
  ADD CONSTRAINT `fk_ticket_submit_user_id` FOREIGN KEY (`submit_user_id`) REFERENCES `sys_user` (`id`),
  ADD CONSTRAINT `fk_ticket_assignee_user_id` FOREIGN KEY (`assignee_user_id`) REFERENCES `sys_user` (`id`),
  ADD CONSTRAINT `fk_ticket_dept_id` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`);

ALTER TABLE `kb_article`
  ADD CONSTRAINT `fk_kb_article_source_ticket_id` FOREIGN KEY (`source_ticket_id`) REFERENCES `ticket` (`id`);

ALTER TABLE `ticket_record`
  ADD CONSTRAINT `fk_ticket_record_ticket_id` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`),
  ADD CONSTRAINT `fk_ticket_record_operator_user_id` FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`);

ALTER TABLE `ticket_comment`
  ADD CONSTRAINT `fk_ticket_comment_ticket_id` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`),
  ADD CONSTRAINT `fk_ticket_comment_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`);

ALTER TABLE `ticket_attachment`
  ADD CONSTRAINT `fk_ticket_attachment_ticket_id` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`),
  ADD CONSTRAINT `fk_ticket_attachment_upload_user_id` FOREIGN KEY (`upload_user_id`) REFERENCES `sys_user` (`id`);

ALTER TABLE `notification`
  ADD CONSTRAINT `fk_notification_receiver_user_id` FOREIGN KEY (`receiver_user_id`) REFERENCES `sys_user` (`id`);

INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `leader_user_id`, `sort_order`, `status`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, '平台研发部', 0, NULL, 1, 1, 1, 1, 0),
  (2, '技术支持部', 0, NULL, 2, 1, 1, 1, 0),
  (3, '产品运营部', 0, NULL, 3, 1, 1, 1, 0);

INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `real_name`, `email`, `phone`, `avatar`, `status`, `dept_id`, `last_login_time`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, 'admin', '$2a$10$.Xu8WvGNQjOiIXCMlhrfPeuGHaJn9fR4dYc1qbF8Dsy8wQk6vL/iG', '系统管理员', '系统管理员', 'admin@docflow.ai', '13800000000', NULL, 1, 1, NULL, 1, 1, 0),
  (2, 'support01', '$2a$10$gkmFHkbo2aEveBED3K9mxOEhe83qbdyJ59L5rQARRhwwcU/zg3lQC', '支持小李', '李晓安', 'support01@docflow.ai', '13800000001', NULL, 1, 2, NULL, 1, 1, 0),
  (3, 'user01', '$2a$10$gkmFHkbo2aEveBED3K9mxOEhe83qbdyJ59L5rQARRhwwcU/zg3lQC', '业务小王', '王晨', 'user01@docflow.ai', '13800000002', NULL, 1, 3, NULL, 1, 1, 0);

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, '系统管理员', 'ADMIN', '拥有系统全部权限', 1, 1, 1, 0),
  (2, '技术支持', 'SUPPORT', '负责处理工单和维护知识库', 1, 1, 1, 0),
  (3, '普通用户', 'USER', '负责提交工单和查询知识库', 1, 1, 1, 0);

INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `status`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, '控制台', 'dashboard:view', 1, 0, '/dashboard', 'dashboard/index', 'LayoutDashboard', 1, 1, 1, 1, 0),
  (2, '知识库', 'knowledge:view', 1, 0, '/knowledge', 'knowledge/index', 'BookOpen', 2, 1, 1, 1, 0),
  (3, '工单中心', 'ticket:view', 1, 0, '/tickets', 'ticket/index', 'Ticket', 3, 1, 1, 1, 0),
  (4, '系统管理', 'system:view', 1, 0, '/system', 'system/index', 'Settings', 9, 1, 1, 1, 0),
  (5, '文章发布', 'knowledge:article:create', 2, 2, NULL, NULL, NULL, 1, 1, 1, 1, 0),
  (6, '工单指派', 'ticket:assign', 2, 3, NULL, NULL, NULL, 1, 1, 1, 1, 0),
  (7, '用户管理', 'system:user:manage', 2, 4, NULL, NULL, NULL, 1, 1, 1, 1, 0),
  (8, '角色管理', 'system:role:manage', 2, 4, NULL, NULL, NULL, 2, 1, 1, 1, 0);

INSERT INTO `sys_user_role` (`user_id`, `role_id`)
VALUES
  (1, 1),
  (2, 2),
  (3, 3);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
VALUES
  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
  (2, 1), (2, 2), (2, 3), (2, 5), (2, 6),
  (3, 1), (3, 2), (3, 3);

INSERT INTO `kb_category` (`id`, `name`, `parent_id`, `sort_order`, `status`, `description`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, '系统使用指南', 0, 1, 1, '平台使用相关知识', 1, 1, 0),
  (2, '故障排查', 0, 2, 1, '常见问题与排查手册', 1, 1, 0),
  (3, '支付与订单', 2, 1, 1, '支付与订单相关问题', 1, 1, 0);

INSERT INTO `kb_tag` (`id`, `tag_name`, `tag_color`, `description`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, '支付', '#2563eb', '支付业务相关', 1, 1, 0),
  (2, '高频问题', '#d97706', '高频问题标签', 1, 1, 0),
  (3, '运维', '#15803d', '运维相关', 1, 1, 0);

INSERT INTO `kb_article` (`id`, `title`, `summary`, `content`, `category_id`, `author_user_id`, `status`, `view_count`, `like_count`, `collect_count`, `publish_time`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, '支付回调失败排查手册', '用于指导支持人员快速定位支付回调失败问题。', '第一步检查签名配置；第二步检查回调地址连通性；第三步检查幂等控制键是否异常；第四步检查日志链路与消息重试记录。', 3, 2, 1, 1284, 36, 19, NOW(), 2, 2, 0),
  (2, '生产环境发布异常应急处理流程', '发布失败、回滚和异常上报的标准操作流程。', '当发布异常发生时，先确认影响范围，再执行回滚或降级方案，同时记录事件时间线并同步相关负责人。', 2, 2, 1, 926, 21, 14, NOW(), 2, 2, 0);

INSERT INTO `kb_article_tag` (`article_id`, `tag_id`)
VALUES
  (1, 1), (1, 2),
  (2, 3);

INSERT INTO `kb_article_version` (`article_id`, `version_no`, `title`, `summary`, `content`, `operator_user_id`, `remark`)
VALUES
  (1, 1, '支付回调失败排查手册', '初始版本。', '第一步检查签名配置；第二步检查回调地址连通性；第三步检查幂等控制键是否异常；第四步检查日志链路与消息重试记录。', 2, '创建初始版本');

INSERT INTO `ticket_category` (`id`, `name`, `parent_id`, `description`, `status`, `sort_order`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, '系统故障', 0, '系统异常、故障类问题', 1, 1, 1, 1, 0),
  (2, '账号权限', 0, '账号、登录、权限相关问题', 1, 2, 1, 1, 0),
  (3, '业务咨询', 0, '业务操作咨询类问题', 1, 3, 1, 1, 0);

INSERT INTO `ticket` (`id`, `ticket_no`, `title`, `content`, `type`, `category_id`, `priority`, `status`, `submit_user_id`, `assignee_user_id`, `dept_id`, `source`, `expected_finish_time`, `actual_finish_time`, `close_time`, `create_by`, `update_by`, `deleted`)
VALUES
  (1, 'INC-20260511-001', '支付回调接口偶发超时', '支付完成后，少量订单没有及时收到回调，怀疑是网络抖动或重试机制问题。', 'INCIDENT', 1, 4, 2, 3, 2, 2, 'WEB', DATE_ADD(NOW(), INTERVAL 4 HOUR), NULL, NULL, 3, 2, 0),
  (2, 'TASK-20260511-001', '员工无法访问内部文档附件', '点击知识文章里的附件后提示无权限访问，需要协助排查。', 'TASK', 2, 3, 1, 3, NULL, 3, 'WEB', DATE_ADD(NOW(), INTERVAL 8 HOUR), NULL, NULL, 3, 3, 0);

UPDATE `kb_article`
SET `source_ticket_id` = 1
WHERE `id` = 1;

INSERT INTO `ticket_record` (`ticket_id`, `operator_user_id`, `action_type`, `old_status`, `new_status`, `remark`)
VALUES
  (1, 3, 'CREATE', NULL, 1, '用户提交工单'),
  (1, 2, 'ASSIGN', 1, 2, '指派给技术支持处理'),
  (2, 3, 'CREATE', NULL, 1, '用户提交工单');

INSERT INTO `ticket_comment` (`ticket_id`, `user_id`, `content`, `comment_type`, `is_internal`, `deleted`)
VALUES
  (1, 2, '已初步定位到支付回调重试逻辑，需要结合日志继续确认。', 2, 0, 0),
  (1, 2, '建议先检查 Redis 幂等键有效期配置是否过短。', 3, 1, 0),
  (2, 3, '附件打开时报 403，请协助处理。', 1, 0, 0);

INSERT INTO `ticket_attachment` (`ticket_id`, `file_name`, `file_url`, `file_size`, `file_type`, `upload_user_id`, `deleted`)
VALUES
  (2, 'error-screenshot.png', '/uploads/tickets/error-screenshot.png', 245678, 'image/png', 3, 0);

INSERT INTO `notification` (`title`, `content`, `notification_type`, `receiver_user_id`, `related_business_type`, `related_business_id`, `is_read`, `deleted`)
VALUES
  ('工单已分配', '工单 INC-20260511-001 已分配给你，请尽快处理。', 'TICKET_ASSIGN', 2, 'TICKET', 1, 0, 0),
  ('文章已发布', '知识文章《支付回调失败排查手册》已发布。', 'ARTICLE_PUBLISH', 3, 'ARTICLE', 1, 0, 0);

INSERT INTO `ai_task_log` (`task_type`, `business_type`, `business_id`, `input_text`, `output_text`, `status`, `error_message`, `model_name`, `token_usage`, `response_time_ms`)
VALUES
  ('ARTICLE_SUMMARY', 'ARTICLE', 1, '请总结支付回调失败排查手册的核心要点。', '建议优先检查签名配置、回调地址连通性、幂等键有效期和日志链路。', 1, NULL, 'gpt-4.1', 318, 1280),
  ('TICKET_REPLY_SUGGEST', 'TICKET', 1, '请为支付回调超时工单给出一段专业回复建议。', '建议先向用户说明问题正在排查，并同步预计完成时间，同时检查回调重试和幂等策略。', 1, NULL, 'gpt-4.1', 402, 1540);

SET FOREIGN_KEY_CHECKS = 1;
