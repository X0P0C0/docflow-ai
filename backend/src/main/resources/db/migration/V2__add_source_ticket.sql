-- V2: Add source ticket reference to knowledge articles
-- Allows tracking which ticket a knowledge article was created from

ALTER TABLE kb_article
  ADD COLUMN IF NOT EXISTS source_ticket_id BIGINT DEFAULT NULL COMMENT '来源工单ID' AFTER category_id,
  ADD INDEX IF NOT EXISTS idx_source_ticket_id (source_ticket_id);
