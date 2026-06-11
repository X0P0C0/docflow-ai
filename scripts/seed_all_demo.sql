-- ============================================================
-- DocFlow AI - Demo Data (corrected schema)
-- Run: mysql -u root -p123456 docflow_ai < seed_all_demo.sql
-- ============================================================

SET NAMES utf8mb4;

-- 1. SLA Policies
INSERT IGNORE INTO sla_policy (name, ticket_type, priority, response_hours, resolve_hours, business_hours_only, status, create_time, update_time) VALUES
('P4 紧急工单 SLA', 'INCIDENT', 4, 1, 4, 0, 1, NOW(), NOW()),
('P3 高优先级 SLA', 'INCIDENT', 3, 1, 8, 0, 1, NOW(), NOW()),
('P2 普通工单 SLA', 'TASK', 2, 2, 24, 1, 1, NOW(), NOW()),
('P1 低优先级 SLA', 'QUESTION', 1, 4, 72, 1, 1, NOW(), NOW());

-- 2. Categories
INSERT IGNORE INTO ticket_category (name, description, parent_id, sort_order, status, create_by, create_time, update_time) VALUES
('技术支持', '技术相关问题', 0, 1, 1, 1, NOW(), NOW()),
('账户问题', '账户和权限相关', 0, 2, 1, 1, NOW(), NOW()),
('功能建议', '产品功能改进建议', 0, 3, 1, 1, NOW(), NOW()),
('网络故障', '网络连接和访问问题', 1, 1, 1, 1, NOW(), NOW()),
('系统故障', '系统崩溃和错误', 1, 2, 1, 1, NOW(), NOW()),
('密码重置', '密码找回和重置', 2, 1, 1, 1, NOW(), NOW()),
('权限申请', '系统权限申请', 2, 2, 1, 1, NOW(), NOW());

-- 3. Tags
INSERT IGNORE INTO ticket_tag (name, color, create_by, create_time) VALUES
('紧急', '#ef4444', 1, NOW()),
('需要跟进', '#f59e0b', 1, NOW()),
('已解决', '#10b981', 1, NOW()),
('客户反馈', '#3b82f6', 1, NOW()),
('重复问题', '#8b5cf6', 1, NOW()),
('文档缺失', '#ec4899', 1, NOW()),
('性能问题', '#f97316', 1, NOW()),
('安全问题', '#dc2626', 1, NOW());

-- 4. Ticket Templates
INSERT IGNORE INTO ticket_template (name, title_template, content_template, type, priority, category_id, is_public, create_by, create_time, update_time) VALUES
('网络故障报告', '网络连接异常', '【问题描述】\n用户报告无法访问内部系统\n\n【影响范围】\n影响用户数：\n影响系统：\n\n【已尝试的解决方案】\n1. 清除浏览器缓存\n2. 检查网络连接\n3. \n\n【紧急程度】\n请选择：紧急/高/普通/低', 'INCIDENT', 3, 4, 1, 1, NOW(), NOW()),
('系统性能问题', '系统响应缓慢', '【问题描述】\n系统响应时间明显变长\n\n【具体表现】\n- 页面加载时间：\n- API响应时间：\n- 数据库查询时间：\n\n【影响时间段】\n开始时间：\n持续时间：\n\n【相关日志】\n请附上相关错误日志', 'INCIDENT', 2, 5, 1, 1, NOW(), NOW()),
('功能需求', '新功能需求: [功能名称]', '【需求背景】\n描述需求的业务背景\n\n【功能描述】\n详细描述期望的功能\n\n【验收标准】\n1. \n2. \n3. \n\n【优先级评估】\n业务价值：高/中/低\n实现难度：高/中/低', 'TASK', 2, 3, 1, 1, NOW(), NOW()),
('权限申请', '权限申请: [系统名称]', '【申请人信息】\n姓名：\n部门：\n工号：\n\n【申请权限】\n系统名称：\n权限级别：只读/读写/管理员\n\n【申请理由】\n\n【审批人】\n直属上级：', 'QUESTION', 2, 7, 1, 1, NOW(), NOW());

-- 5. Custom Fields
INSERT IGNORE INTO custom_field (field_name, field_label, field_type, options, required, sort_order, status, create_by, create_time, update_time) VALUES
('impact_users', '影响用户数', 'NUMBER', NULL, 0, 1, 1, 1, NOW(), NOW()),
('affected_systems', '影响系统', 'MULTI_SELECT', 'OA系统,CRM系统,ERP系统,邮件系统,VPN', 0, 2, 1, 1, NOW(), NOW()),
('fault_time', '故障发生时间', 'DATE', NULL, 0, 3, 1, 1, NOW(), NOW()),
('expected_resolve', '期望解决时间', 'DATE', NULL, 0, 4, 1, 1, NOW(), NOW()),
('fault_category', '问题分类', 'SELECT', '硬件故障,软件故障,网络故障,人为操作,其他', 0, 5, 1, 1, NOW(), NOW()),
('need_callback', '是否需要回访', 'SELECT', '是,否', 0, 6, 1, 1, NOW(), NOW());

-- 6. Automation Rules
INSERT IGNORE INTO automation_rule (name, description, trigger_type, conditions, actions, priority, status, create_by, create_time, update_time) VALUES
('紧急工单自动升级', 'P4紧急工单创建后自动升级到经理', 'TICKET_CREATED', '{"priority": 4}', '{"action": "ESCALATE", "level": 2}', 10, 1, 1, NOW(), NOW()),
('SLA即将超时提醒', 'SLA响应时间剩余30%时自动提醒', 'SLA_BREACH', '{"threshold": 0.3}', '{"action": "NOTIFY", "role": "MANAGER"}', 20, 1, 1, NOW(), NOW()),
('新工单自动分配', '新工单根据类型自动分配给对应团队', 'TICKET_CREATED', '{"type": "INCIDENT"}', '{"action": "ASSIGN", "team": "TECH_SUPPORT"}', 30, 1, 1, NOW(), NOW()),
('解决后自动通知', '工单解决后自动通知提交人', 'STATUS_CHANGED', '{"toStatus": 3}', '{"action": "NOTIFY", "target": "SUBMITTER"}', 40, 1, 1, NOW(), NOW()),
('7天自动关闭', '已解决工单7天后自动关闭', 'STATUS_CHANGED', '{"toStatus": 3, "days": 7}', '{"action": "AUTO_CLOSE"}', 50, 0, 1, NOW(), NOW());

-- 7. Webhooks
INSERT IGNORE INTO webhook_config (name, url, secret, events, status, create_by, create_time, update_time) VALUES
('Slack通知', 'https://hooks.slack.com/services/xxx/yyy/zzz', 'slack_secret_123', 'TICKET_CREATED,TICKET_RESOLVED,SLA_BREACH', 1, 1, NOW(), NOW()),
('钉钉通知', 'https://oapi.dingtalk.com/robot/send?access_token=xxx', 'dingding_secret', 'TICKET_CREATED,TICKET_CLOSED', 1, 1, NOW(), NOW()),
('企业微信', 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx', NULL, 'TICKET_CREATED', 0, 1, NOW(), NOW()),
('数据同步', 'https://api.example.com/webhook/docflow', 'sync_secret_456', 'TICKET_UPDATED,COMMENT_ADDED', 1, 1, NOW(), NOW());


-- 10. Tickets
INSERT IGNORE INTO ticket (id, ticket_no, title, content, type, priority, status, submit_user_id, assignee_user_id, source, deleted, create_time, update_time) VALUES
(1, 'TK-20260525-001', 'VPN connection failure, unable to access internal systems', 'User reports VPN connection failure since this morning, error code 628. Affects all internal system access including OA and CRM. Tried restarting VPN client and switching networks, issue persists.', 'INCIDENT', 3, 3, 1, 2, 'CUSTOMER_PORTAL', 0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'TK-20260523-002', 'CRM system loading extremely slowly', 'CRM page load time exceeds 10 seconds, severely affecting customer service efficiency. Issue started yesterday afternoon. Other systems are normal, only CRM is affected.', 'INCIDENT', 2, 3, 1, 3, 'CUSTOMER_PORTAL', 0, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3, 'TK-20260521-003', 'Request to add batch export feature to reporting module', 'The reporting module currently only supports single record export. Need batch export functionality to support monthly report generation. Expected to export data in CSV and Excel formats.', 'TASK', 2, 2, 1, 4, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 'TK-20260526-004', 'New employee OA system permission setup', 'New employee Wang Xiaoming (employee ID: EMP20260526) needs OA system basic access permissions. Department: Technology Department. Start date: May 27.', 'QUESTION', 2, 1, 1, NULL, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 'TK-20260527-005', 'Email system send/receive failure', 'Since this morning, company email system cannot send or receive emails. Affects all employees. Error message: Connection timed out. Urgent, as it impacts normal business communication.', 'INCIDENT', 4, 2, 1, 5, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 'TK-20260528-006', 'ERP system password reset request', 'Employee Zhang San forgot ERP system password, needs reset. Account: zhangsan. Has verified identity information.', 'QUESTION', 2, 3, 1, 1, 'INTERNAL', 0, DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- 8. Notifications
INSERT IGNORE INTO notification (receiver_user_id, title, content, notification_type, is_read, related_business_id, related_business_type, create_time) VALUES
(1, '新工单待处理', '工单 TK-20260528-001 已创建，请及时处理', 'TICKET', 0, 1, 'TICKET', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 'SLA即将超时', '工单 TK-20260527-003 的响应时间即将超时', 'SLA', 0, 3, 'TICKET', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, '工单已解决', '工单 TK-20260526-002 已由张工解决', 'TICKET', 0, 2, 'TICKET', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, '新评论', '客户对工单 TK-20260525-001 添加了新评论', 'COMMENT', 1, 1, 'TICKET', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(1, '系统更新', '系统已更新到 v2.1.0，新增自动化规则功能', 'SYSTEM', 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 'AI分析完成', 'AI已完成对工单 TK-20260528-001 的分析', 'AI', 0, 1, 'AI_ANALYSIS', DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(1, '新客户注册', '新客户 科技有限公司 已完成注册', 'SYSTEM', 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 'Webhook测试成功', 'Webhook Slack通知 测试发送成功', 'SYSTEM', 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR));

-- 9. Customer Users
INSERT IGNORE INTO customer_user (username, password, email, phone, company, real_name, status, create_time, update_time) VALUES
('customer1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'customer1@example.com', '13800138001', '科技有限公司', '李明', 1, NOW(), NOW()),
('customer2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'customer2@example.com', '13800138002', '互联网公司', '王芳', 1, NOW(), NOW()),
('customer3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'customer3@example.com', '13800138003', '创业科技', '赵强', 1, NOW(), NOW());

-- 10. Knowledge Articles
INSERT IGNORE INTO kb_article (title, summary, content, category_id, source_ticket_id, author_user_id, status, publish_time, create_time, update_time) VALUES
('VPN连接故障排查指南', '常见VPN连接问题及解决方案', '# VPN连接故障排查指南\n\n## 常见问题\n\n### 1. 无法连接VPN\n- 检查网络连接\n- 确认VPN服务器地址正确\n- 检查防火墙设置\n\n### 2. 连接频繁断开\n- 更新VPN客户端到最新版本\n- 检查网络稳定性\n- 尝试更换VPN协议\n\n### 3. 连接速度慢\n- 选择就近的VPN节点\n- 检查带宽占用\n- 联系网络管理员', NULL, NULL, 1, 1, DATE_SUB(NOW(), INTERVAL 1 WEEK), DATE_SUB(NOW(), INTERVAL 2 WEEK), DATE_SUB(NOW(), INTERVAL 1 WEEK)),
('CRM系统使用手册', 'CRM系统基本操作指南', '# CRM系统使用手册\n\n## 登录\n1. 访问 https://crm.example.com\n2. 输入工号和密码\n3. 首次登录需修改密码\n\n## 客户管理\n- 添加客户：客户管理 > 新建客户\n- 编辑客户：点击客户名称 > 编辑\n- 导出客户：客户管理 > 导出\n\n## 报表查看\n- 月度报表：报表中心 > 月度报表\n- 自定义报表：报表中心 > 自定义查询', NULL, NULL, 1, 1, DATE_SUB(NOW(), INTERVAL 1 WEEK), DATE_SUB(NOW(), INTERVAL 2 WEEK), DATE_SUB(NOW(), INTERVAL 1 WEEK)),
('密码重置流程', '如何重置各系统密码', '# 密码重置流程\n\n## 自助重置\n1. 访问密码重置页面\n2. 输入工号和注册邮箱\n3. 收到重置链接\n4. 设置新密码\n\n## 人工重置\n如自助重置失败，请提交工单：\n- 类型：账户问题\n- 标题：密码重置申请\n- 内容：工号、姓名、需要重置的系统', NULL, NULL, 1, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 WEEK), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('工单处理最佳实践', '如何高效处理工单', '# 工单处理最佳实践\n\n## 响应及时\n- 紧急工单：15分钟内响应\n- 高优先级：30分钟内响应\n- 普通工单：2小时内响应\n\n## 沟通规范\n- 使用专业术语\n- 提供解决方案\n- 及时更新状态\n\n## 知识沉淀\n- 解决后创建知识文章\n- 记录解决方案\n- 分享给团队', NULL, NULL, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 WEEK), DATE_SUB(NOW(), INTERVAL 5 DAY)),
('系统架构说明', 'DocFlow AI系统架构概述', '# 系统架构说明\n\n## 技术栈\n- 后端：Spring Boot 3.3.1 + MyBatis-Plus\n- 前端：Vue 3 + Element Plus\n- 数据库：MySQL 8.0\n- 缓存：Redis\n\n## 核心模块\n1. 工单管理\n2. 知识库\n3. AI分析\n4. 客户门户\n5. 自动化规则\n\n## 部署架构\n- 单体部署（可扩展为微服务）\n- Docker容器化\n- Nginx反向代理', NULL, NULL, 1, 0, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));


-- 11. Ticket Records (Timeline)
INSERT IGNORE INTO ticket_record (ticket_id, operator_user_id, action_type, old_status, new_status, remark, create_time) VALUES
(1, 1, 'CREATE', NULL, 1, 'Customer reported VPN connection failure, unable to access internal systems', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 1, 'ASSIGN', 1, 1, 'Assigned to Zhang Wei for investigation', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 10 MINUTE),
(1, 2, 'STATUS_CHANGE', 1, 2, 'Started investigating, checking VPN server logs', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 30 MINUTE),
(1, 2, 'COMMENT', 2, 2, 'Found the issue: VPN server certificate expired, renewing now', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 2, 'STATUS_CHANGE', 2, 3, 'Certificate renewed, VPN service restored', DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 2 HOUR),
(2, 1, 'CREATE', NULL, 1, 'CRM system loading slowly, page load time exceeds 10 seconds', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 1, 'ASSIGN', 1, 1, 'Assigned to Li Na for performance analysis', DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 15 MINUTE),
(2, 3, 'STATUS_CHANGE', 1, 2, 'Started analysis, checking database query performance', DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 1 HOUR),
(2, 3, 'COMMENT', 2, 2, 'Root cause identified: missing index on customer table, adding index now', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 3, 'STATUS_CHANGE', 2, 3, 'Index added, page load time reduced to under 2 seconds', DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 3 HOUR),
(3, 1, 'CREATE', NULL, 1, 'Request to add batch export feature to the reporting module', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(3, 1, 'ASSIGN', 1, 1, 'Assigned to Wang Fang for requirements analysis', DATE_SUB(NOW(), INTERVAL 7 DAY) + INTERVAL 20 MINUTE),
(3, 4, 'STATUS_CHANGE', 1, 2, 'Requirements confirmed, starting development', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 1, 'CREATE', NULL, 1, 'New employee needs OA system access permissions', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 1, 'ASSIGN', 1, 1, 'Assigned to IT admin for permission setup', DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 5 MINUTE),
(5, 1, 'CREATE', NULL, 1, 'Email system cannot send or receive emails', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 1, 'ASSIGN', 1, 1, 'Assigned to Zhao Qiang for urgent investigation', DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 10 MINUTE),
(5, 5, 'STATUS_CHANGE', 1, 2, 'Checking mail server status and DNS configuration', DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 20 MINUTE),
(6, 1, 'CREATE', NULL, 1, 'Need to reset password for ERP system', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(6, 1, 'ASSIGN', 1, 1, 'Assigned to customer service for processing', DATE_SUB(NOW(), INTERVAL 6 HOUR) + INTERVAL 5 MINUTE),
(6, 1, 'STATUS_CHANGE', 1, 2, 'Verifying user identity information', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(6, 1, 'STATUS_CHANGE', 2, 3, 'Identity verified, password reset email sent', DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- Done
SELECT 'Demo data seeded!' AS result;
