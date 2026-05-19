INSERT INTO sys_user (id, username, password, nickname, real_name, email, phone, avatar, status, dept_id, last_login_time, create_by, update_by, create_time, update_time, deleted)
VALUES
    (1, 'user01', NULL, 'user01', 'User One', NULL, NULL, NULL, 1, NULL, NULL, 1, 1, '2026-05-19 09:00:00', '2026-05-19 09:00:00', 0),
    (2, 'support01', NULL, 'support01', 'Support Wang', NULL, NULL, NULL, 1, NULL, NULL, 2, 2, '2026-05-19 09:00:00', '2026-05-19 09:00:00', 0);

INSERT INTO sys_role (id, role_code, role_name, status, create_by, update_by, create_time, update_time, deleted)
VALUES
    (1, 'USER', 'User', 1, 1, 1, '2026-05-19 09:00:00', '2026-05-19 09:00:00', 0),
    (2, 'SUPPORT', 'Support', 1, 2, 2, '2026-05-19 09:00:00', '2026-05-19 09:00:00', 0);

INSERT INTO sys_permission (id, permission_code, permission_name, status, create_by, update_by, create_time, update_time, deleted)
VALUES
    (1, 'knowledge:view', 'Knowledge View', 1, 1, 1, '2026-05-19 09:00:00', '2026-05-19 09:00:00', 0),
    (2, 'knowledge:article:create', 'Knowledge Create', 1, 2, 2, '2026-05-19 09:00:00', '2026-05-19 09:00:00', 0),
    (3, 'ticket:view', 'Ticket View', 1, 2, 2, '2026-05-19 09:00:00', '2026-05-19 09:00:00', 0),
    (4, 'ticket:assign', 'Ticket Assign', 1, 2, 2, '2026-05-19 09:00:00', '2026-05-19 09:00:00', 0);

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES
    (1, 1, 1),
    (2, 2, 2);

INSERT INTO sys_role_permission (id, role_id, permission_id)
VALUES
    (1, 1, 1),
    (2, 2, 1),
    (3, 2, 2),
    (4, 2, 3),
    (5, 2, 4);

INSERT INTO ticket (id, ticket_no, title, content, type, category_id, priority, status, submit_user_id, assignee_user_id, dept_id, source, expected_finish_time, actual_finish_time, close_time, create_by, update_by, create_time, update_time, deleted)
VALUES
    (100, 'INC-20260519-0100', 'Payment callback failed', 'Payment callback failed for several orders after the latest release.', 'INCIDENT', 3, 3, 3, 1, 2, NULL, 'portal', NULL, NULL, '2026-05-19 12:30:00', 1, 2, '2026-05-19 09:30:00', '2026-05-19 12:30:00', 0),
    (101, 'INC-20260519-0101', 'Refund callback pending', 'Refund callback is still pending for a subset of merchants.', 'INCIDENT', 3, 2, 2, 1, 2, NULL, 'portal', NULL, NULL, NULL, 1, 2, '2026-05-19 10:00:00', '2026-05-19 11:00:00', 0);

INSERT INTO ticket_record (id, ticket_id, operator_user_id, action_type, old_status, new_status, remark, create_time)
VALUES
    (1000, 100, 2, 'CREATE', NULL, 1, 'Ticket created', '2026-05-19 09:30:00'),
    (1001, 100, 2, 'STATUS_CHANGE', 2, 3, 'Resolved after callback hotfix', '2026-05-19 12:20:00'),
    (1002, 101, 2, 'CREATE', NULL, 1, 'Ticket created', '2026-05-19 10:00:00');

INSERT INTO ticket_comment (id, ticket_id, user_id, content, comment_type, is_internal, create_time, update_time, deleted)
VALUES
    (2000, 100, 2, 'Root cause traced to callback signature mismatch after deployment.', 2, 1, '2026-05-19 11:30:00', '2026-05-19 11:30:00', 0),
    (2001, 100, 1, 'Customer confirmed the callbacks are healthy again.', 1, 0, '2026-05-19 12:25:00', '2026-05-19 12:25:00', 0);
