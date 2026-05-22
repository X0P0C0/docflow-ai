-- V3: Ensure seed user passwords are set to BCrypt-encoded '123456'
-- This is idempotent - safe to run multiple times

UPDATE sys_user
SET password = '\\.0RYMV26CfnyJXrK.iMj5Msknu0FPAeCSWXkeuxGKoMSdPrS'
WHERE username IN ('admin', 'support01', 'user01')
  AND deleted = 0;
