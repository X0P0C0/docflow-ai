-- 审计日志表：记录所有关键操作
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       COMMENT '操作人 ID',
    username        VARCHAR(64)  COMMENT '操作人用户名',
    module          VARCHAR(32)  NOT NULL COMMENT '操作模块',
    action          VARCHAR(64)  NOT NULL COMMENT '操作描述',
    method          VARCHAR(10)  COMMENT '请求方法',
    uri             VARCHAR(256) COMMENT '请求 URI',
    status_code     INT          COMMENT 'HTTP 状态码',
    duration_ms     BIGINT       COMMENT '执行耗时（毫秒）',
    client_ip       VARCHAR(64)  COMMENT '客户端 IP',
    operate_time    DATETIME     NOT NULL COMMENT '操作时间',
    success         TINYINT      DEFAULT 1 COMMENT '是否成功（1=成功，0=失败）',
    error_message   TEXT         COMMENT '错误信息',
    trace_id        VARCHAR(64)  COMMENT '链路追踪 ID',
    INDEX idx_user_id (user_id),
    INDEX idx_operate_time (operate_time),
    INDEX idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';