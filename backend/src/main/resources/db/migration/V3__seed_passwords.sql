-- V3: 确保种子用户的密码设置为 BCrypt 编码的 'admin123'
-- BCrypt hash for 'admin123': $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- This is idempotent - safe to run multiple times

UPDATE sys_user
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi'
WHERE username IN ('admin', 'support01', 'user01')
  AND deleted = 0;