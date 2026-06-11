-- V12: Customer portal support

CREATE TABLE IF NOT EXISTS customer_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    password        VARCHAR(200) NOT NULL,
    email           VARCHAR(200),
    phone           VARCHAR(32),
    company         VARCHAR(200),
    real_name       VARCHAR(100),
    avatar          VARCHAR(500),
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS webhook_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    url             VARCHAR(500) NOT NULL,
    secret          VARCHAR(200),
    events          VARCHAR(500) NOT NULL,
    status          TINYINT DEFAULT 1,
    create_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS webhook_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id      BIGINT NOT NULL,
    event           VARCHAR(100) NOT NULL,
    payload         TEXT,
    response_code   INT,
    response_body   TEXT,
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_webhook (webhook_id),
    INDEX idx_event (event)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO customer_user (username, password, email, company, real_name, status) VALUES
('customer01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'customer01@example.com', 'Acme Corp', 'John Smith', 1),
('customer02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'customer02@example.com', 'TechStart Inc', 'Jane Doe', 1);
