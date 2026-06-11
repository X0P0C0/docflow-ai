SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

INSERT IGNORE INTO ticket (ticket_no, title, content, type, priority, status, submit_user_id, assignee_user_id, source, deleted, create_time, update_time) VALUES
('INC-20260528-0010', 'OA系统审批流程卡死，无法提交', '用户反馈OA系统审批流程在提交环节卡死，页面一直转圈。影响全公司日常审批，已持续2小时。', 'INCIDENT', 4, 2, 1, 2, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 0 HOUR)),
('INC-20260528-0011', '报销系统上传附件失败', '报销系统上传发票附件时提示文件格式不支持，但用户确认是PDF格式。影响财务报销流程。', 'INCIDENT', 3, 2, 1, 3, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('TASK-20260528-0012', '新员工入职培训文档更新', '需要更新2026年Q2新员工入职培训文档，增加远程办公规范和安全培训章节。', 'TASK', 2, 2, 1, 4, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('INC-20260528-0013', '会议室预订系统时间冲突', '多个部门反映会议室预订系统出现时间冲突，同一时段同一会议室被重复预订。', 'INCIDENT', 3, 3, 1, 2, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
('QST-20260528-0014', '如何配置VPN双因素认证', '客户咨询如何启用VPN双因素认证功能，需要操作指南。', 'QUESTION', 2, 3, 1, NULL, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR)),
('INC-20260528-0015', '生产环境日志文件占满磁盘', '生产服务器/var/log目录磁盘使用率达到95%，需要紧急清理并配置日志轮转策略。', 'INCIDENT', 4, 3, 1, 5, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR)),
('TASK-20260528-0016', '客户满意度调查功能开发', '需要在工单关闭后自动发送客户满意度调查问卷，收集服务质量反馈。', 'TASK', 2, 1, 1, NULL, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 7 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
('INC-20260528-0017', '移动端APP推送通知不生效', 'iOS和Android客户端推送通知功能失效，用户无法收到工单状态变更提醒。', 'INCIDENT', 3, 2, 1, 3, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR)),
('QST-20260528-0018', 'API接口限流策略咨询', '开发团队咨询当前API限流策略配置，需要了解各接口的QPS限制和熔断规则。', 'QUESTION', 1, 3, 1, NULL, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 9 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR)),
('INC-20260528-0019', '数据库主从同步延迟告警', 'MySQL主从复制延迟超过30秒，读写分离策略下部分用户看到旧数据。', 'INCIDENT', 4, 1, 1, 2, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 9 HOUR)),
('TASK-20260528-0020', '知识库文章分类优化', '当前知识库分类层级不够清晰，需要重新整理分类结构并迁移现有文章。', 'TASK', 2, 2, 1, 4, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 11 HOUR), DATE_SUB(NOW(), INTERVAL 10 HOUR)),
('INC-20260528-0021', 'SSO单点登录间歇性失败', '部分用户反馈SSO登录偶尔失败，需要刷新页面重试才能成功。影响约10%的登录尝试。', 'INCIDENT', 3, 1, 1, 2, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 11 HOUR)),
('INC-20260528-0022', '报表导出PDF格式错乱', '月度报表导出PDF时表格排版错乱，中文字符显示为方块。已影响管理层月度汇报。', 'INCIDENT', 3, 3, 1, 2, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 13 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR)),
('TASK-20260528-0023', '自动化测试覆盖率提升', '当前单元测试覆盖率仅42%，目标提升至70%。优先补充核心业务逻辑的测试用例。', 'TASK', 1, 2, 1, 5, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 14 HOUR), DATE_SUB(NOW(), INTERVAL 13 HOUR)),
('INC-20260528-0024', 'Redis缓存雪崩导致服务降级', 'Redis集群节点故障引发缓存雪崩，后端服务大面积降级。已启动应急预案。', 'INCIDENT', 4, 4, 1, 5, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 15 HOUR), DATE_SUB(NOW(), INTERVAL 14 HOUR));
