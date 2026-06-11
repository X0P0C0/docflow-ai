UPDATE customer_user SET password='$2b$10$Lu56yub6ZSX1IQu8ZmBgU.sbVfAYeeNdcvmiCTYLLb42hmwGCtq6a';
SELECT id, username, LENGTH(password) as len FROM customer_user WHERE id=7;
