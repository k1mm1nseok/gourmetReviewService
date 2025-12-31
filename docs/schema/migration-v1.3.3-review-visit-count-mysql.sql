-- ============================================
-- Gourmet Review Service DDL Migration v1.3.3 (MySQL 8+)
-- Review: title (nullable), party_size (required), visit_count (PUBLIC-only)
-- Add member_store_visit for cumulative visit count
-- ============================================

-- 1. Review columns
ALTER TABLE `review`
    ADD COLUMN `title` TEXT NULL AFTER `member_id`;

ALTER TABLE `review`
    ADD COLUMN `party_size` INT NOT NULL DEFAULT 1 AFTER `content`;

ALTER TABLE `review`
    ADD COLUMN `visit_count` INT NOT NULL DEFAULT 0 AFTER `visit_date`;

ALTER TABLE `review`
    CHANGE COLUMN `like_count` `helpful_count` INT NOT NULL DEFAULT 0;

ALTER TABLE `review`
    DROP COLUMN `is_revisit`;

-- Optional: backfill visit_count for existing PUBLIC reviews
-- UPDATE `review` r
-- JOIN (
--     SELECT id,
--            ROW_NUMBER() OVER (PARTITION BY member_id, store_id ORDER BY created_at, id) AS seq
--     FROM `review`
--     WHERE status = 'PUBLIC'
-- ) ranked ON r.id = ranked.id
-- SET r.visit_count = ranked.seq;

-- 2. member_store_visit (cumulative visits)
CREATE TABLE IF NOT EXISTS `member_store_visit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Visit ID',
  `member_id` BIGINT NOT NULL COMMENT 'Member ID',
  `store_id` BIGINT NOT NULL COMMENT 'Store ID',
  `visit_count` INT NOT NULL DEFAULT 0 COMMENT 'Cumulative visit count (PUBLIC only)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_store_visit` (`member_id`, `store_id`),
  KEY `idx_member_store_visit_member` (`member_id`),
  KEY `idx_member_store_visit_store` (`store_id`),
  CONSTRAINT `fk_member_store_visit_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_member_store_visit_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Member-store cumulative visit count';

-- 3. review_helpful (rename from review_like)
RENAME TABLE `review_like` TO `review_helpful`;

-- Optional: backfill member_store_visit from existing PUBLIC reviews
-- INSERT INTO `member_store_visit` (`member_id`, `store_id`, `visit_count`, `created_at`, `updated_at`)
-- SELECT member_id, store_id, COUNT(*) AS visit_count, MIN(created_at), NOW()
-- FROM `review`
-- WHERE status = 'PUBLIC'
-- GROUP BY member_id, store_id;

-- ============================================
-- End
-- ============================================
