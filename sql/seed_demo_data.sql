-- DocFlow AI Demo Data Seed
-- Story: E-commerce company tech ops team (电商公司技术运维团队的一天)

USE docflow_ai;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- CLEAN: Remove all dirty data
-- ============================================================
DELETE FROM notification WHERE deleted = 0;
DELETE FROM ai_task_log;
DELETE FROM ticket_attachment WHERE deleted = 0;
DELETE FROM ticket_comment WHERE deleted = 0;
DELETE FROM ticket_record;
DELETE FROM ticket WHERE deleted = 0;
DELETE FROM kb_article_version;
DELETE FROM kb_article WHERE deleted = 0;

-- ============================================================
-- USERS: Ensure 3 users exist
-- ============================================================
INSERT IGNORE INTO sys_user (id, username, password, nickname, real_name, email, phone, status, dept_id, create_by, update_by, create_time, update_time, deleted)
VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', 'admin', '系统管理员', 'admin@docflow.local', NULL, 1, NULL, 1, 1, '2026-05-20 09:00:00', '2026-05-20 09:00:00', 0),
(2, 'support01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', 'support01', '王磊', 'wanglei@docflow.local', NULL, 1, NULL, 1, 1, '2026-05-20 09:00:00', '2026-05-20 09:00:00', 0),
(3, 'user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', 'user01', '张伟', 'zhangwei@docflow.local', NULL, 1, NULL, 1, 1, '2026-05-20 09:00:00', '2026-05-20 09:00:00', 0);

-- Reset passwords to BCrypt('123456')
UPDATE sys_user SET password = '$2a$10$.0RYMV26CfnyJXrK.iMj5Msknu0FPAeCSWXkeuxGKoMSdPrS'
WHERE username IN ('admin', 'support01', 'user01') AND deleted = 0;

-- ============================================================
-- TICKETS: 5 tickets with a story
-- ============================================================

-- Ticket 1: Payment callback failure (RESOLVED)
INSERT INTO ticket (id, ticket_no, title, content, type, category_id, priority, status, submit_user_id, assignee_user_id, dept_id, source, expected_finish_time, actual_finish_time, close_time, create_by, update_by, create_time, update_time, deleted)
VALUES
(1001, 'INC-20260527-0001', '支付回调签名验证失败', 
'【问题描述】\n今天上午10:00起，多个商户反馈支付完成后订单状态未更新。经排查，支付网关回调请求签名验证持续失败。\n\n【影响范围】\n涉及商户约30家，累计受影响订单约200笔。\n\n【初步判断】\n回调签名密钥可能在上次部署时未正确同步。',
'INCIDENT', 3, 4, 3, 3, 2, NULL, 'portal', '2026-05-27 18:00:00', '2026-05-27 14:30:00', '2026-05-27 14:30:00', 3, 2, '2026-05-27 10:15:00', '2026-05-27 14:32:00', 0);

-- Ticket 2: Refund delay (IN PROGRESS)
INSERT INTO ticket (id, ticket_no, title, content, type, category_id, priority, status, submit_user_id, assignee_user_id, dept_id, source, expected_finish_time, create_by, update_by, create_time, update_time, deleted)
VALUES
(1002, 'INC-20260527-0002', '退款到账延迟，商户投诉', 
'【问题描述】\n有5家商户反馈，近3天的退款申请均显示“处理中”但未到账，客户投诉量上升。\n\n【排查方向】\n1. 检查退款接口调用日志\n2. 确认银行通道是否正常\n3. 排查是否有资金冻结情况',
'INCIDENT', 3, 3, 2, 3, 2, NULL, 'portal', '2026-05-28 12:00:00', 3, 2, '2026-05-27 11:30:00', '2026-05-27 16:00:00', 0);

-- Ticket 3: Customer data export request (NEW - unassigned)
INSERT INTO ticket (id, ticket_no, title, content, type, category_id, priority, status, submit_user_id, assignee_user_id, dept_id, source, create_by, update_by, create_time, update_time, deleted)
VALUES
(1003, 'TASK-20260527-0003', '客户申请导出三个月订单明细', 
'客户“杭州云创科技”需要导出 2026年3月1日至5月31日的所有订单明细，用于财务对账。共约 1,200 笔订单。\n\n请协助导出 Excel 文件。',
'TASK', 1, 1, 1, 3, NULL, NULL, 'portal', 3, 3, '2026-05-27 14:00:00', '2026-05-27 14:00:00', 0);

-- Ticket 4: Login page blank screen (RESOLVED - linked to KB)
INSERT INTO ticket (id, ticket_no, title, content, type, category_id, priority, status, submit_user_id, assignee_user_id, dept_id, source, expected_finish_time, actual_finish_time, close_time, create_by, update_by, create_time, update_time, deleted)
VALUES
(1004, 'INC-20260527-0004', '管理后台登录页白屏', 
'【问题描述】\n新入职同事反馈，使用 Chrome 浏览器访问管理后台时，登录页面显示白屏，无法输入账号密码。\n\n【环境信息】\nChrome 125.0.6422.76 / Windows 11\n\n已在知识库找到相关文章。',
'INCIDENT', 2, 3, 3, 3, 2, NULL, 'portal', '2026-05-27 17:00:00', '2026-05-27 15:45:00', '2026-05-27 15:45:00', 3, 2, '2026-05-27 13:45:00', '2026-05-27 15:46:00', 0);

-- Ticket 5: Server CPU alert (URGENT - in progress)
INSERT INTO ticket (id, ticket_no, title, content, type, category_id, priority, status, submit_user_id, assignee_user_id, dept_id, source, expected_finish_time, create_by, update_by, create_time, update_time, deleted)
VALUES
(1005, 'INC-20260527-0005', '生产服务器 CPU 持续 95% 告警', 
'【告警信息】\n监控系统于 15:00 触发告警：生产集群 node-3 CPU 使用率持续 95% 超过 15 分钟。\n\n【初步排查】\n1. top 显示 java 进程占用 85% CPU\n2. 线程 dump 显示大量线程阻塞在数据库连接池\n3. 怀疑有慢查询或连接池泄漏\n\n【紧急措施】\n已临时扩容至 8 核，暂未影响线上服务。',
'INCIDENT', 3, 4, 2, 2, 2, NULL, 'monitor', '2026-05-27 20:00:00', 2, 2, '2026-05-27 15:05:00', '2026-05-27 16:30:00', 0);

-- ============================================================
-- TICKET RECORDS: Timeline for each ticket
-- ============================================================

-- Ticket 1001 timeline
INSERT INTO ticket_record (id, ticket_id, user_id, action, remark, create_time) VALUES
(2001, 1001, 3, 'CREATE', '用户提交工单', '2026-05-27 10:15:00'),
(2002, 1001, 1, 'ASSIGN', '系统管理员指派给王磊处理', '2026-05-27 10:20:00'),
(2003, 1001, 2, 'STATUS_CHANGE', '开始排查，确认为部署时密钥未同步', '2026-05-27 11:00:00'),
(2004, 1001, 2, 'RESOLVE', '已重新同步签名密钥，回调恢复正常。已通知受影响商户。', '2026-05-27 14:30:00');

-- Ticket 1002 timeline
INSERT INTO ticket_record (id, ticket_id, user_id, action, remark, create_time) VALUES
(2005, 1002, 3, 'CREATE', '用户提交工单', '2026-05-27 11:30:00'),
(2006, 1002, 1, 'ASSIGN', '指派给王磊处理', '2026-05-27 11:35:00'),
(2007, 1002, 2, 'STATUS_CHANGE', '已定位到银行通道异常，正在联系银行技术支持', '2026-05-27 13:00:00');

-- Ticket 1003 timeline
INSERT INTO ticket_record (id, ticket_id, user_id, action, remark, create_time) VALUES
(2008, 1003, 3, 'CREATE', '用户提交工单', '2026-05-27 14:00:00');

-- Ticket 1004 timeline
INSERT INTO ticket_record (id, ticket_id, user_id, action, remark, create_time) VALUES
(2009, 1004, 3, 'CREATE', '用户提交工单', '2026-05-27 13:45:00'),
(2010, 1004, 1, 'ASSIGN', '指派给王磊处理', '2026-05-27 13:50:00'),
(2011, 1004, 2, 'RESOLVE', '浏览器缓存问题，已指导清除缓存后正常登录。已关联知识库文章 #1。', '2026-05-27 15:45:00');

-- Ticket 1005 timeline
INSERT INTO ticket_record (id, ticket_id, user_id, action, remark, create_time) VALUES
(2012, 1005, 2, 'CREATE', '监控告警自动创建工单', '2026-05-27 15:05:00'),
(2013, 1005, 2, 'STATUS_CHANGE', '正在排查数据库连接池问题', '2026-05-27 15:30:00');

-- ============================================================
-- TICKET COMMENTS
-- ============================================================

-- Ticket 1001 comments
INSERT INTO ticket_comment (id, ticket_id, user_id, content, comment_type, is_internal, create_time, update_time, deleted) VALUES
(3001, 1001, 2, '【诊断结果】\n根因为昨晚部署时，ops 脚本未正确同步支付网关的 RSA 签名公钥。旧密钥在节点重启后被清除，导致验签失败。\n\n已执行修复：1) 重新同步签名密钥 2) 验证 200 笔受影响订单的支付状态 3) 批量补推送订单状态回调。', 2, 1, '2026-05-27 12:00:00', '2026-05-27 12:00:00', 0),
(3002, 1001, 3, '收到，测试了几笔订单回调已经正常了。谢谢！', 1, 0, '2026-05-27 14:25:00', '2026-05-27 14:25:00', 0);

-- Ticket 1002 comments
INSERT INTO ticket_comment (id, ticket_id, user_id, content, comment_type, is_internal, create_time, update_time, deleted) VALUES
(3003, 1002, 2, '【排查进展】\n已确认问题出在民生银行退款接口。银行侧于 5月25日升级了接口认证方式，我方尚未同步更新。预计今天完成适配开发，明天上午验证后上线。', 2, 1, '2026-05-27 14:00:00', '2026-05-27 14:00:00', 0);

-- Ticket 1005 comments
INSERT INTO ticket_comment (id, ticket_id, user_id, content, comment_type, is_internal, create_time, update_time, deleted) VALUES
(3004, 1005, 2, '【排查进展】\n通过 thread dump 分析，大量线程阻塞在 Druid 连接池的 getConnection() 方法。初步怀疑是新的报表服务未正确释放连接。正在审查报表服务的 HikariCP 配置。', 2, 1, '2026-05-27 16:00:00', '2026-05-27 16:00:00', 0);

-- ============================================================
-- KNOWLEDGE ARTICLES: 3 articles linked to tickets
-- ============================================================

INSERT INTO kb_article (id, title, summary, content, category_id, source_ticket_id, status, author_user_id, view_count, like_count, collect_count, publish_time, create_by, update_by, create_time, update_time, deleted) VALUES
(1, '支付回调故障排查手册',
'系统性地梳理支付回调异常的诊断流程与修复方案，涵盖签名验证失败、回调超时、重复通知等常见场景。',
'# 支付回调故障排查手册\n\n## 常见故障类型\n\n### 1. 签名验证失败\n**现象**：支付网关回调返回 signature verification failed\n**原因**：\n- 签名密钥未正确配置\n- 密钥轮换后未同步\n- 回调参数顺序与约定不一致\n\n**排查步骤**：\n1. 检查配置文件中的签名密钥是否与支付网关一致\n2. 查看部署脚本是否包含密钥同步步骤\n3. 对比本地签名结果与网关签名\n\n**修复方案**：\n- 重新同步签名密钥\n- 在 CI/CD 流程中加入密钥校验环节\n\n### 2. 回调超时\n**现象**：支付成功后超过 5 分钟未收到回调\n**原因**：\n- 回调 URL 不可达\n- 网关限流\n- DNS 解析异常\n\n**排查步骤**：\n1. 检查回调 URL 是否可公网访问\n2. 查看网关回调日志\n3. 测试 DNS 解析\n\n### 3. 重复通知\n**现象**：同一笔订单收到多次回调\n**处理**：\n- 以订单号为幂等键，已处理过的回调直接返回 success\n- 记录重复回调日志用于监控\n\n## 预防措施\n- 每次部署后执行回调健康检查\n- 配置回调失败告警\n- 定期与支付网关核对密钥有效期',
NULL, 1001, 1, 2, 48, 12, 5, '2026-05-27 15:00:00', 2, 2, '2026-05-27 15:00:00', '2026-05-27 15:00:00', 0),

(2, '退款流程异常处理指南',
'退款到账延迟、退款失败、重复退款等场景的诊断与处理流程。',
'# 退款流程异常处理指南\n\n## 退款状态说明\n- 处理中：已发起退款请求，等待银行处理\n- 已到账：银行确认退款成功\n- 失败：银行返回退款失败\n\n## 常见问题\n\n### 退款到账延迟\n**原因**：银行通道处理延迟或接口异常\n**处理流程**：\n1. 确认退款请求已正确提交\n2. 查询银行退款单号\n3. 联系银行技术支持确认状态\n4. 超过 48 小时未到账则升级为紧急工单\n\n### 退款金额错误\n**处理流程**：\n1. 核对原订单金额\n2. 检查退款计算公式\n3. 确认是否有优惠券/积分抵扣\n\n### 银行接口变更\n银行侧升级接口时可能导致退款失败：\n1. 提前关注银行通知\n2. 在测试环境验证新接口\n3. 制定切换方案和回滚预案',
NULL, 1002, 1, 2, 35, 8, 3, '2026-05-27 16:30:00', 2, 2, '2026-05-27 16:30:00', '2026-05-27 16:30:00', 0),

(3, '新手常见登录问题 FAQ',
'汇总新同事入职后常见的系统登录问题及解决方案，帮助快速上手。',
'# 新手常见登录问题 FAQ\n\n## Q1: 登录页白屏\n**原因**：浏览器缓存了旧版本的前端资源\n**解决**：\n1. Chrome: 按 F12 → Application → Clear Storage → Clear site data\n2. 或使用快捷键 Ctrl+Shift+Delete 清除缓存\n3. 刷新页面即可\n\n## Q2: 提示"账号或密码错误"\n**原因**：初始密码已过期或输入错误\n**解决**：\n1. 确认是否已修改初始密码\n2. 联系管理员重置密码\n3. 检查 Caps Lock 是否开启\n\n## Q3: 登录后立即跳回登录页\n**原因**：Token 过期或浏览器禁用了 Cookie\n**解决**：\n1. 检查浏览器是否允许第三方 Cookie\n2. 清除站点数据后重新登录\n3. 尝试无痕模式\n\n## Q4: 收不到验证码\n**解决**：\n1. 检查手机号是否正确\n2. 查看垃圾短信箱\n3. 等待 60 秒后重试\n4. 联系管理员',
NULL, 1004, 1, 2, 62, 15, 7, '2026-05-27 17:00:00', 2, 2, '2026-05-27 17:00:00', '2026-05-27 17:00:00', 0);

-- ============================================================
-- ARTICLE VERSIONS
-- ============================================================
INSERT INTO kb_article_version (id, article_id, version, content, change_note, create_by, create_time) VALUES
(1, 1, 1, '# 支付回调故障排查手册\n\n## 签名验证失败\n修复方案：重新同步签名密钥。', '初始版本', 2, '2026-05-27 15:00:00'),
(2, 2, 1, '# 退款流程异常处理指南\n\n## 退款到账延迟\n处理流程见正文。', '初始版本', 2, '2026-05-27 16:30:00'),
(3, 3, 1, '# 新手常见登录问题 FAQ\n\n## 登录页白屏\n清除浏览器缓存。', '初始版本', 2, '2026-05-27 17:00:00');

SET FOREIGN_KEY_CHECKS = 1;