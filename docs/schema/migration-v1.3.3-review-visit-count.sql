-- ============================================
-- Gourmet Review Service DDL Migration v1.3.3 (PostgreSQL)
-- Review: title (nullable), party_size (required), visit_count (PUBLIC-only)
-- Add member_store_visit for cumulative visit count
-- ============================================

-- 1. Review columns
ALTER TABLE review
    ADD COLUMN IF NOT EXISTS title TEXT;

ALTER TABLE review
    ADD COLUMN IF NOT EXISTS party_size INTEGER;

UPDATE review
SET party_size = 1
WHERE party_size IS NULL;

ALTER TABLE review
    ALTER COLUMN party_size SET NOT NULL;

ALTER TABLE review
    ADD COLUMN IF NOT EXISTS visit_count INTEGER;

UPDATE review
SET visit_count = 0
WHERE visit_count IS NULL;

ALTER TABLE review
    ALTER COLUMN visit_count SET NOT NULL;

ALTER TABLE review
    ALTER COLUMN visit_count SET DEFAULT 0;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'review' AND column_name = 'like_count'
  ) THEN
    IF NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'review' AND column_name = 'helpful_count'
    ) THEN
      ALTER TABLE review RENAME COLUMN like_count TO helpful_count;
    ELSE
      ALTER TABLE review DROP COLUMN like_count;
    END IF;
  END IF;
END $$;

ALTER TABLE review
    ADD COLUMN IF NOT EXISTS helpful_count INTEGER;

UPDATE review
SET helpful_count = 0
WHERE helpful_count IS NULL;

ALTER TABLE review
    ALTER COLUMN helpful_count SET NOT NULL;

ALTER TABLE review
    ALTER COLUMN helpful_count SET DEFAULT 0;

ALTER TABLE review
    DROP COLUMN IF EXISTS is_revisit;

-- Optional: backfill visit_count for existing PUBLIC reviews
-- WITH ranked AS (
--     SELECT id,
--            ROW_NUMBER() OVER (PARTITION BY member_id, store_id ORDER BY created_at, id) AS seq
--     FROM review
--     WHERE status = 'PUBLIC'
-- )
-- UPDATE review r
-- SET visit_count = ranked.seq
-- FROM ranked
-- WHERE r.id = ranked.id;

-- 2. member_store_visit (cumulative visits)
CREATE TABLE IF NOT EXISTS member_store_visit (
  id BIGSERIAL PRIMARY KEY,
  member_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  visit_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_member_store_visit UNIQUE (member_id, store_id),
  CONSTRAINT fk_member_store_visit_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
  CONSTRAINT fk_member_store_visit_store FOREIGN KEY (store_id) REFERENCES store (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_member_store_visit_member ON member_store_visit (member_id);
CREATE INDEX IF NOT EXISTS idx_member_store_visit_store ON member_store_visit (store_id);

DROP TRIGGER IF EXISTS update_member_store_visit_updated_at ON member_store_visit;
CREATE TRIGGER update_member_store_visit_updated_at
  BEFORE UPDATE ON member_store_visit
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

-- 3. review_helpful (rename from review_like)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_name = 'review_like'
  ) THEN
    ALTER TABLE review_like RENAME TO review_helpful;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS review_helpful (
  id BIGSERIAL PRIMARY KEY,
  review_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_review_helpful UNIQUE (review_id, member_id),
  CONSTRAINT fk_review_helpful_review FOREIGN KEY (review_id) REFERENCES review (id) ON DELETE CASCADE,
  CONSTRAINT fk_review_helpful_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_review_helpful_review ON review_helpful (review_id);
CREATE INDEX IF NOT EXISTS idx_review_helpful_member ON review_helpful (member_id);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_name = 'review_helpful'
  ) THEN
    DROP TRIGGER IF EXISTS update_review_like_updated_at ON review_helpful;
    DROP TRIGGER IF EXISTS update_review_helpful_updated_at ON review_helpful;
    CREATE TRIGGER update_review_helpful_updated_at
      BEFORE UPDATE ON review_helpful
      FOR EACH ROW
      EXECUTE FUNCTION update_updated_at_column();
  END IF;
END $$;

-- Optional: backfill member_store_visit from existing PUBLIC reviews
-- INSERT INTO member_store_visit (member_id, store_id, visit_count, created_at, updated_at)
-- SELECT member_id, store_id, COUNT(*) AS visit_count, MIN(created_at), NOW()
-- FROM review
-- WHERE status = 'PUBLIC'
-- GROUP BY member_id, store_id;

-- ============================================
-- End
-- ============================================
