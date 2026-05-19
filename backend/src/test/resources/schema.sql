DROP TABLE IF EXISTS kb_article_version;
DROP TABLE IF EXISTS kb_article;
DROP TABLE IF EXISTS ticket_comment;
DROP TABLE IF EXISTS ticket_record;
DROP TABLE IF EXISTS ticket;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NULL,
    nickname VARCHAR(64) NULL,
    real_name VARCHAR(64) NULL,
    email VARCHAR(128) NULL,
    phone VARCHAR(32) NULL,
    avatar VARCHAR(255) NULL,
    status INT NOT NULL DEFAULT 1,
    dept_id BIGINT NULL,
    last_login_time DATETIME NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NULL,
    status INT NOT NULL DEFAULT 1,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NULL,
    status INT NOT NULL DEFAULT 1,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);

CREATE TABLE ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_no VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(32) NOT NULL,
    category_id BIGINT NULL,
    priority INT NULL,
    status INT NULL,
    submit_user_id BIGINT NULL,
    assignee_user_id BIGINT NULL,
    dept_id BIGINT NULL,
    source VARCHAR(64) NULL,
    expected_finish_time DATETIME NULL,
    actual_finish_time DATETIME NULL,
    close_time DATETIME NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE ticket_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    operator_user_id BIGINT NULL,
    action_type VARCHAR(64) NOT NULL,
    old_status INT NULL,
    new_status INT NULL,
    remark VARCHAR(500) NULL,
    create_time DATETIME NULL
);

CREATE TABLE ticket_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    comment_type INT NULL,
    is_internal INT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE kb_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500) NULL,
    content LONGTEXT NOT NULL,
    category_id BIGINT NULL,
    source_ticket_id BIGINT NULL,
    author_user_id BIGINT NULL,
    status INT NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    collect_count INT NOT NULL DEFAULT 0,
    publish_time DATETIME NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE kb_article_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500) NULL,
    content LONGTEXT NOT NULL,
    operator_user_id BIGINT NULL,
    remark VARCHAR(255) NULL,
    create_time DATETIME NULL
);
