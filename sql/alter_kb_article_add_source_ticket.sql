ALTER TABLE `kb_article`
  ADD COLUMN `source_ticket_id` BIGINT DEFAULT NULL COMMENT '来源工单ID' AFTER `category_id`,
  ADD KEY `idx_source_ticket_id` (`source_ticket_id`);

ALTER TABLE `kb_article`
  ADD CONSTRAINT `fk_kb_article_source_ticket_id`
  FOREIGN KEY (`source_ticket_id`) REFERENCES `ticket` (`id`);
