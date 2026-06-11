-- V9: Custom field system

CREATE TABLE IF NOT EXISTS custom_field (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_name      VARCHAR(100) NOT NULL,
    field_label     VARCHAR(200) NOT NULL,
    field_type      VARCHAR(32) NOT NULL,
    options         TEXT,
    default_value   VARCHAR(500),
    required        TINYINT DEFAULT 0,
    sort_order      INT DEFAULT 0,
    category_id     BIGINT,
    status          TINYINT DEFAULT 1,
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS custom_field_value (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    field_id        BIGINT NOT NULL,
    field_value     TEXT,
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_ticket_field (ticket_id, field_id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_field (field_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO custom_field (field_name, field_label, field_type, options, required, sort_order) VALUES
('environment', 'Environment', 'SELECT', '["Production","Staging","Development","Test"]', 1, 1),
('browser', 'Browser', 'SELECT', '["Chrome","Firefox","Safari","Edge","Other"]', 0, 2),
('os', 'Operating System', 'SELECT', '["Windows","macOS","Linux","iOS","Android"]', 0, 3),
('reproducible', 'Reproducible', 'CHECKBOX', NULL, 0, 4),
('affected_users', 'Affected Users Count', 'NUMBER', NULL, 0, 5);
