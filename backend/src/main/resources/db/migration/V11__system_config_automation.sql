-- V11: System config, automation rules, KB article tags

CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key      VARCHAR(200) NOT NULL,
    config_value    TEXT,
    config_type     VARCHAR(32) DEFAULT 'SYSTEM',
    description     VARCHAR(500),
    status          TINYINT DEFAULT 1,
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS automation_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(500),
    trigger_type    VARCHAR(50) NOT NULL,
    conditions      TEXT NOT NULL,
    actions         TEXT NOT NULL,
    priority        INT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS kb_article_tag (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id      BIGINT NOT NULL,
    tag_id          BIGINT NOT NULL,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_tag (article_id, tag_id),
    INDEX idx_article (article_id),
    INDEX idx_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sys_config (config_key, config_value, config_type, description) VALUES
('site.name', 'DocFlow AI', 'SYSTEM', 'Site name'),
('ticket.auto_assign', 'false', 'TICKET', 'Auto-assign tickets based on workload'),
('ticket.sla_enabled', 'true', 'TICKET', 'Enable SLA tracking'),
('notification.email_enabled', 'false', 'NOTIFICATION', 'Enable email notifications'),
('ai.auto_suggest', 'true', 'AI', 'Enable AI auto-suggestion'),
('security.max_login_attempts', '5', 'SECURITY', 'Max login attempts before lockout'),
('security.session_timeout', '7200', 'SECURITY', 'Session timeout in seconds');

INSERT IGNORE INTO automation_rule (name, description, trigger_type, conditions, actions, priority, status) VALUES
('Auto-assign P1', 'Auto-assign critical tickets to senior support', 'TICKET_CREATED', 
 '[{"field":"priority","op":"eq","value":"1"}]', 
 '[{"action":"ASSIGN","params":{"role":"SENIOR_SUPPORT"}}]', 100, 1),
('SLA Warning', 'Notify when SLA is about to breach', 'SLA_WARNING',
 '[{"field":"sla_remaining","op":"lt","value":"30"}]',
 '[{"action":"NOTIFY","params":{"target":"assignee","message":"SLA deadline approaching"}}]', 90, 1),
('Auto-close Resolved', 'Auto-close resolved tickets after 7 days', 'SCHEDULE',
 '[{"field":"status","op":"eq","value":"3"},{"field":"resolved_days","op":"gt","value":"7"}]',
 '[{"action":"CLOSE","params":{"remark":"Auto-closed after 7 days"}}]', 50, 1);
