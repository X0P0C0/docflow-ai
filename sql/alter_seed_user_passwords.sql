UPDATE `sys_user`
SET `password` = '$2a$10$Ccx5k.0RYMV26CfnyJXrK.iMj5Msknu0FPAeCSWXkeuxGKoMSdPrS'
WHERE `username` IN ('admin', 'support01', 'user01')
  AND `deleted` = 0;
