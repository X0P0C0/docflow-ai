-- V10: Tags and tag relations

CREATE TABLE IF NOT EXISTS ticket_tag (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    color           VARCHAR(32) DEFAULT '#409EFF',
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket_tag_relation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    tag_id          BIGINT NOT NULL,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_tag (ticket_id, tag_id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO ticket_tag (name, color) VALUES
('urgent', '#F56C6C'),
('bug', '#E6A23C'),
('feature', '#67C23A'),
('documentation', '#909399'),
('customer-facing', '#409EFF'),
('internal', '#9B59B6');
