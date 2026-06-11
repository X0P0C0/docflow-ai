-- V8: Ticket enhancements - transfer, escalate, merge, link, satisfaction, SLA

-- 1. Add columns to ticket table
ALTER TABLE ticket ADD COLUMN escalation_level TINYINT DEFAULT 0 COMMENT 'Escalation level: 0=normal, 1=supervisor, 2=manager, 3=director';
ALTER TABLE ticket ADD COLUMN merged_into_id BIGINT DEFAULT NULL COMMENT 'Target ticket ID if merged';
ALTER TABLE ticket ADD COLUMN satisfaction_score TINYINT DEFAULT NULL COMMENT 'Customer satisfaction score (1-5)';
ALTER TABLE ticket ADD COLUMN satisfaction_comment VARCHAR(500) DEFAULT NULL COMMENT 'Customer satisfaction comment';
ALTER TABLE ticket ADD COLUMN sla_response_deadline DATETIME DEFAULT NULL COMMENT 'SLA response deadline';
ALTER TABLE ticket ADD COLUMN sla_resolve_deadline DATETIME DEFAULT NULL COMMENT 'SLA resolve deadline';
ALTER TABLE ticket ADD COLUMN first_response_time DATETIME DEFAULT NULL COMMENT 'First response time';
ALTER TABLE ticket ADD COLUMN resolved_time DATETIME DEFAULT NULL COMMENT 'Resolved time';

-- 2. Ticket link table (many-to-many)
CREATE TABLE IF NOT EXISTS ticket_link (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id       BIGINT NOT NULL,
    linked_ticket_id BIGINT NOT NULL,
    link_type       VARCHAR(32) DEFAULT 'RELATED' COMMENT 'RELATED, DUPLICATE, BLOCKED',
    create_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ticket_link (ticket_id, linked_ticket_id),
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_linked_ticket_id (linked_ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Ticket merge log table
CREATE TABLE IF NOT EXISTS ticket_merge_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_ticket_id BIGINT NOT NULL,
    target_ticket_id BIGINT NOT NULL,
    reason          VARCHAR(500),
    merge_by        BIGINT,
    merge_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_source (source_ticket_id),
    INDEX idx_target (target_ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. SLA policy table
CREATE TABLE IF NOT EXISTS sla_policy (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    ticket_type     VARCHAR(64),
    priority        TINYINT,
    response_hours  INT NOT NULL,
    resolve_hours   INT NOT NULL,
    business_hours_only TINYINT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Ticket template table
CREATE TABLE IF NOT EXISTS ticket_template (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    title_template  VARCHAR(200),
    content_template TEXT,
    type            VARCHAR(64),
    priority        TINYINT,
    category_id     BIGINT,
    is_public       TINYINT DEFAULT 0,
    create_by       BIGINT,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Default SLA policies
INSERT INTO sla_policy (name, ticket_type, priority, response_hours, resolve_hours) VALUES
('P1 Critical', 'INCIDENT', 1, 1, 4),
('P2 High', 'INCIDENT', 2, 4, 8),
('P3 Medium', 'INCIDENT', 3, 8, 24),
('P4 Low', 'INCIDENT', 4, 24, 72),
('Task High', 'TASK', 2, 4, 16),
('Task Medium', 'TASK', 3, 8, 40),
('Question High', 'QUESTION', 2, 4, 24),
('Question Medium', 'QUESTION', 3, 12, 48);

-- 7. Default ticket templates
INSERT INTO ticket_template (name, title_template, content_template, type, priority, is_public, create_by) VALUES
('Payment Issue', '[Payment] Problem description', 'Customer reports payment issue:\n\nSymptoms:\nImpact scope:\nExpected resolution time:', 'INCIDENT', 2, 1, 1),
('Login Error', '[Login] Problem description', 'Customer reports login error:\n\nSymptoms:\nDevice info:\nNetwork environment:', 'INCIDENT', 2, 1, 1),
('Feature Request', '[Feature] Suggestion content', 'Customer feature suggestion:\n\nSuggestion:\nUse case:\nExpected effect:', 'QUESTION', 3, 1, 1),
('Data Issue', '[Data] Problem description', 'Customer reports data anomaly:\n\nSymptoms:\nAffected data:\nDiscovery time:', 'INCIDENT', 1, 1, 1);