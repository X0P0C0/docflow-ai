-- V5: 站内通知 —— 注：notification 表已在 V1 中创建，此处只插入示例数据
-- 字段映射：receiver_user_id, notification_type, related_business_type, related_business_id, is_read

INSERT INTO notification (receiver_user_id, notification_type, title, content, related_business_type, related_business_id, is_read, create_time, deleted)
SELECT 1, 'ASSIGN', '工单指派通知', '支持工程师李晓安将工单 INC-20260522-001「支付回调偶发超时」指派给您', 'TICKET', 1, 0, NOW() - INTERVAL 2 HOUR, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '工单指派通知');

INSERT INTO notification (receiver_user_id, notification_type, title, content, related_business_type, related_business_id, is_read, create_time, deleted)
SELECT 1, 'AI_SUGGESTION', 'AI 建议通知', 'AI 为工单 INC-20260522-002 生成了回复建议，请查看', 'TICKET', 2, 0, NOW() - INTERVAL 1 HOUR, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = 'AI 建议通知');

INSERT INTO notification (receiver_user_id, notification_type, title, content, related_business_type, related_business_id, is_read, create_time, deleted)
SELECT 2, 'STATUS_CHANGE', '工单状态变更', '您提交的工单 QST-20260523-001「如何配置双因素认证」状态已变更为处理中', 'TICKET', 5, 1, NOW() - INTERVAL 1 DAY, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '工单状态变更');

INSERT INTO notification (receiver_user_id, notification_type, title, content, related_business_type, related_business_id, is_read, create_time, deleted)
SELECT 1, 'SYSTEM', '系统通知', '系统已完成知识库索引更新，新增 3 篇文章', NULL, NULL, 0, NOW() - INTERVAL 30 MINUTE, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '系统通知');

INSERT INTO notification (receiver_user_id, notification_type, title, content, related_business_type, related_business_id, is_read, create_time, deleted)
SELECT 3, 'COMMENT', '评论通知', '李晓安在工单 INC-20260522-001 中添加了评论', 'TICKET', 1, 0, NOW() - INTERVAL 15 MINUTE, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM notification WHERE title = '评论通知');