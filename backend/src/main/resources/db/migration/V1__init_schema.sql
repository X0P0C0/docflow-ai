-- V1: Initial schema - all core tables
-- This is the baseline migration; Flyway skips it for existing databases (baseline-on-migrate=true)

CREATE TABLE IF NOT EXISTS sys_dept (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID',
  leader_user_id BIGINT DEFAULT NULL COMMENT '负责人用户ID',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(64) NOT NULL COMMENT '登录用户名',
  password VARCHAR(255) NOT NULL COMMENT '加密密码',
  
ickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  
eal_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  create_by BIGINT DEFAULT NULL,
  update_by BIGINT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Insert seed users (password: 123456, BCrypt encoded)
INSERT IGNORE INTO sys_user (id, username, password, 
ickname, 
eal_name, status, dept_id) VALUES
(1, 'admin', '\\.0RYMV26CfnyJXrK.iMj5Msknu0FPAeCSWXkeuxGKoMSdPrS', '管理员', '系统管理员', 1, NULL),
(2, 'support01', '\\.0RYMV26CfnyJXrK.iMj5Msknu0FPAeCSWXkeuxGKoMSdPrS', '客服01', '客服专员', 1, NULL),
(3, 'user01', '\\.0RYMV26CfnyJXrK.iMj5Msknu0FPAeCSWXkeuxGKoMSdPrS', '用户01', '普通用户', 1, NULL);

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
ole_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  
ole_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  sort_order INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_by BIGINT DEFAULT NULL,
  update_by BIGINT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (
ole_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

INSERT IGNORE INTO sys_role (id, 
ole_code, 
ole_name, sort_order) VALUES
(1, 'ADMIN', '管理员', 1),
(2, 'SUPPORT', '客服', 2),
(3, 'USER', '普通用户', 3);

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  
ole_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, 
ole_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

INSERT IGNORE INTO sys_user_role (id, user_id, 
ole_id) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 3);

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  perm_code VARCHAR(128) NOT NULL COMMENT '权限编码',
  perm_name VARCHAR(128) NOT NULL COMMENT '权限名称',
  PRIMARY KEY (id),
  UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  
ole_id BIGINT NOT NULL,
  perm_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_perm (
ole_id, perm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS 	icket (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  	icket_no VARCHAR(32) NOT NULL COMMENT '工单编号',
  	itle VARCHAR(255) NOT NULL COMMENT '工单标题',
  content TEXT COMMENT '工单描述',
  	ype VARCHAR(32) NOT NULL DEFAULT 'TICKET' COMMENT '工单类型：INCIDENT/TASK/QUESTION',
  status INT NOT NULL DEFAULT 1 COMMENT '状态：1待处理 2处理中 3已解决 4已关闭',
  priority INT NOT NULL DEFAULT 2 COMMENT '优先级：1紧急 2高 3中 4低',
  submit_user_id BIGINT NOT NULL COMMENT '提交人ID',
  ssignee_user_id BIGINT DEFAULT NULL COMMENT '处理人ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_no (	icket_no),
  KEY idx_submit_user_id (submit_user_id),
  KEY idx_assignee_user_id (ssignee_user_id),
  KEY idx_status (status),
  KEY idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

CREATE TABLE IF NOT EXISTS 	icket_comment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  	icket_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  comment_type INT NOT NULL DEFAULT 1 COMMENT '类型：1公开评论 2内部备注 3系统记录',
  is_internal TINYINT NOT NULL DEFAULT 0 COMMENT '是否内部评论：0否 1是',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_ticket_id (	icket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单评论表';

CREATE TABLE IF NOT EXISTS 	icket_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  	icket_id BIGINT NOT NULL,
  user_id BIGINT DEFAULT NULL,
  ction VARCHAR(64) NOT NULL COMMENT '操作类型',
  
emark VARCHAR(512) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ticket_id (	icket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单操作记录表';

CREATE TABLE IF NOT EXISTS kb_article (
  id BIGINT NOT NULL AUTO_INCREMENT,
  	itle VARCHAR(255) NOT NULL COMMENT '文章标题',
  summary TEXT COMMENT '摘要',
  content LONGTEXT COMMENT '文章内容',
  category_id BIGINT DEFAULT NULL,
  source_ticket_id BIGINT DEFAULT NULL COMMENT '来源工单ID',
  status INT NOT NULL DEFAULT 1 COMMENT '状态：1草稿 2已发布 3已归档',
  uthor_user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_author_user_id (uthor_user_id),
  KEY idx_status (status),
  KEY idx_source_ticket_id (source_ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文章表';

CREATE TABLE IF NOT EXISTS kb_article_version (
  id BIGINT NOT NULL AUTO_INCREMENT,
  rticle_id BIGINT NOT NULL,
  ersion INT NOT NULL DEFAULT 1,
  content LONGTEXT,
  change_note VARCHAR(512) DEFAULT NULL,
  create_by BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_article_id (rticle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文章版本表';

CREATE TABLE IF NOT EXISTS i_task_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  	icket_id BIGINT DEFAULT NULL,
  	ask_type VARCHAR(64) NOT NULL COMMENT '任务类型',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  
esult LONGTEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ticket_id (	icket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI任务日志表';

CREATE TABLE IF NOT EXISTS 
otification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  	itle VARCHAR(255) NOT NULL,
  content TEXT,
  is_read TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
