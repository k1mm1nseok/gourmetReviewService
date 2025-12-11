-- ============================================
-- Gourmet Review Service DDL Migration v1.3.2
-- Entity Mapping 패치 마이그레이션 (PostgreSQL)
-- ============================================
--
-- 목적:
--   1. review 테이블 컬럼명을 정책 문서 용어와 통일
--   2. member 테이블에 password 컬럼 추가
--
-- 적용 전 체크리스트:
--   □ 프로덕션 DB 백업 완료
--   □ 애플리케이션 배포 계획 수립 (다운타임 최소화)
--   □ 롤백 스크립트 준비
-- ============================================

-- ============================================
-- 1. review 테이블 컬럼명 변경
-- ============================================

-- 변경 내역:
--   - score_price → score_value (가성비 점수)
--   - score_mood → score_ambiance (분위기 점수)

-- Step 1: 컬럼명 변경
ALTER TABLE review
    RENAME COLUMN score_price TO score_value;

ALTER TABLE review
    RENAME COLUMN score_mood TO score_ambiance;

-- Step 2: 컬럼 설명 업데이트 (PostgreSQL COMMENT)
COMMENT ON COLUMN review.score_value IS '가성비 점수 (0.00 ~ 5.00)';
COMMENT ON COLUMN review.score_ambiance IS '분위기 점수 (0.00 ~ 5.00)';

-- 검증 쿼리
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'review' AND column_name IN ('score_value', 'score_ambiance')
-- ORDER BY ordinal_position;


-- ============================================
-- 2. member 테이블 password 컬럼 추가
-- ============================================

-- 주의사항:
--   - 기존 회원이 있는 경우, NOT NULL 제약을 즉시 적용하면 오류 발생
--   - 단계별 마이그레이션 권장:
--     1) NULL 허용으로 컬럼 추가
--     2) 기존 회원에 임시 비밀번호 설정
--     3) NOT NULL 제약 추가

-- Option 1: 신규 프로젝트 (기존 데이터 없음)
-- 즉시 NOT NULL 제약 적용
ALTER TABLE member
    ADD COLUMN password VARCHAR(255) NOT NULL;

COMMENT ON COLUMN member.password IS '비밀번호 (BCrypt 암호화)';


-- Option 2: 기존 데이터가 있는 경우 (단계별 마이그레이션)
--
-- Step 1: NULL 허용으로 컬럼 추가
-- ALTER TABLE member
--     ADD COLUMN password VARCHAR(255);
--
-- COMMENT ON COLUMN member.password IS '비밀번호 (BCrypt 암호화)';
--
-- Step 2: 기존 회원에 임시 비밀번호 설정 (BCrypt 해시)
-- -- 예시 임시 비밀번호: "TempPass2025!" → BCrypt 해시
-- UPDATE member
-- SET password = '$2a$10$dXJ3SW6G7P1lWCkrOd.Oi.hCeFO8hKNnvbZp7oVR6CKrn9T7iK0qO'
-- WHERE password IS NULL;
--
-- Step 3: NOT NULL 제약 추가
-- ALTER TABLE member
--     ALTER COLUMN password SET NOT NULL;


-- ============================================
-- 3. 검증 쿼리
-- ============================================

-- review 테이블 컬럼 확인
-- SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale
-- FROM information_schema.columns
-- WHERE table_name = 'review'
-- ORDER BY ordinal_position;

-- member 테이블 컬럼 확인
-- SELECT column_name, data_type, character_maximum_length, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'member'
-- ORDER BY ordinal_position;


-- ============================================
-- 4. 롤백 스크립트 (긴급 상황 시 사용)
-- ============================================

-- review 테이블 컬럼명 복원
-- ALTER TABLE review
--     RENAME COLUMN score_value TO score_price;
--
-- ALTER TABLE review
--     RENAME COLUMN score_ambiance TO score_mood;
--
-- COMMENT ON COLUMN review.score_price IS '가격 점수';
-- COMMENT ON COLUMN review.score_mood IS '분위기 점수';

-- member 테이블 password 컬럼 제거
-- ALTER TABLE member
--     DROP COLUMN password;


-- ============================================
-- 5. 마이그레이션 완료 확인
-- ============================================

-- 1. 컬럼 존재 확인
-- SELECT
--     CASE WHEN EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_name = 'review' AND column_name = 'score_value'
--     ) THEN '✓ review.score_value 존재' ELSE '✗ 누락' END AS check_1,
--
--     CASE WHEN EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_name = 'review' AND column_name = 'score_ambiance'
--     ) THEN '✓ review.score_ambiance 존재' ELSE '✗ 누락' END AS check_2,
--
--     CASE WHEN EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_name = 'member' AND column_name = 'password'
--     ) THEN '✓ member.password 존재' ELSE '✗ 누락' END AS check_3;

-- 2. 기존 컬럼 제거 확인
-- SELECT
--     CASE WHEN NOT EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_name = 'review' AND column_name = 'score_price'
--     ) THEN '✓ score_price 제거됨' ELSE '✗ 여전히 존재' END AS check_4,
--
--     CASE WHEN NOT EXISTS (
--         SELECT 1 FROM information_schema.columns
--         WHERE table_name = 'review' AND column_name = 'score_mood'
--     ) THEN '✓ score_mood 제거됨' ELSE '✗ 여전히 존재' END AS check_5;


-- ============================================
-- 끝
-- ============================================
