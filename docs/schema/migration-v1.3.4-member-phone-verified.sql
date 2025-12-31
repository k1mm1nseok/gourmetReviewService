-- ============================================
-- Migration v1.3.4: member.is_phone_verified
-- ============================================
-- 목적:
-- - 리뷰 작성 권한(전화번호 인증 완료자만 작성 가능) 도입을 위한 필드 추가
-- - 기존 데이터는 기본값(false)로 세팅
--
-- 주의:
-- - 본 프로젝트는 운영 DB 마이그레이션 도구(Flyway/Liquibase)를 아직 사용하지 않으므로,
--   실제 운영 반영 시에는 적용 순서/락/다운타임을 고려해 수동 적용 필요

ALTER TABLE member
    ADD COLUMN IF NOT EXISTS is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_member_phone_verified ON member (is_phone_verified);

