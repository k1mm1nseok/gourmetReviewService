-- ============================================
-- Gourmet Review Service Database Schema v1.3.2
-- PostgreSQL DDL Script
-- ============================================
--
-- 변경 이력:
--   - MySQL → PostgreSQL 문법 변환
--   - Entity 패치 반영: password, score_value, score_ambiance
--   - updated_at 자동 갱신 트리거 포함
-- ============================================

-- ============================================
-- 0. updated_at 자동 갱신 트리거 함수 (공통)
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = CURRENT_TIMESTAMP;
   RETURN NEW;
END;
$$ language 'plpgsql';

COMMENT ON FUNCTION update_updated_at_column() IS 'updated_at 컬럼을 현재 시간으로 자동 갱신하는 트리거 함수';


-- ============================================
-- 1. MEMBER (회원)
-- ============================================

CREATE TABLE member (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,  -- 패치: BCrypt 암호화 비밀번호
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
  helpful_count INTEGER NOT NULL DEFAULT 0,
  review_count INTEGER NOT NULL DEFAULT 0,
  violation_count INTEGER NOT NULL DEFAULT 0,
  last_review_at TIMESTAMP NULL,
  is_deviation_target BOOLEAN NOT NULL DEFAULT FALSE,
  is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_member_email UNIQUE (email),
  CONSTRAINT uk_member_nickname UNIQUE (nickname)
);

CREATE INDEX idx_member_tier ON member (tier);
CREATE INDEX idx_member_last_review_at ON member (last_review_at);
CREATE INDEX idx_member_phone_verified ON member (is_phone_verified);

CREATE TRIGGER update_member_updated_at
  BEFORE UPDATE ON member
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE member IS '회원';
COMMENT ON COLUMN member.id IS '회원 ID';
COMMENT ON COLUMN member.email IS '이메일';
COMMENT ON COLUMN member.nickname IS '닉네임';
COMMENT ON COLUMN member.password IS '비밀번호 (BCrypt 암호화)';
COMMENT ON COLUMN member.role IS '권한 (USER, ADMIN)';
COMMENT ON COLUMN member.tier IS '회원 등급 (BRONZE, SILVER, GOLD, GOURMET, BLACK)';
COMMENT ON COLUMN member.helpful_count IS '누적 도움됨 수';
COMMENT ON COLUMN member.review_count IS '누적 리뷰 수';
COMMENT ON COLUMN member.violation_count IS '누적 위반 횟수';
COMMENT ON COLUMN member.last_review_at IS '마지막 리뷰 작성일시';
COMMENT ON COLUMN member.is_deviation_target IS '편차 보정 대상 여부';
COMMENT ON COLUMN member.is_phone_verified IS '휴대폰 인증 여부 (리뷰 작성 권한)';
COMMENT ON COLUMN member.created_at IS '가입일시';
COMMENT ON COLUMN member.updated_at IS '수정일시';


-- ============================================
-- 2. CATEGORY (카테고리)
-- ============================================

CREATE TABLE category (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  parent_id BIGINT NULL,
  depth INTEGER NOT NULL DEFAULT 0,

  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id)
    REFERENCES category (id) ON DELETE CASCADE
);

CREATE INDEX idx_category_parent ON category (parent_id);
CREATE INDEX idx_category_depth ON category (depth);

COMMENT ON TABLE category IS '카테고리 (계층구조)';
COMMENT ON COLUMN category.id IS '카테고리 ID';
COMMENT ON COLUMN category.name IS '카테고리명';
COMMENT ON COLUMN category.parent_id IS '상위 카테고리 ID';
COMMENT ON COLUMN category.depth IS '계층 깊이';


-- ============================================
-- 3. REGION (지역)
-- ============================================

CREATE TABLE region (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  parent_id BIGINT NULL,
  depth INTEGER NOT NULL DEFAULT 0,

  CONSTRAINT fk_region_parent FOREIGN KEY (parent_id)
    REFERENCES region (id) ON DELETE CASCADE
);

CREATE INDEX idx_region_parent ON region (parent_id);
CREATE INDEX idx_region_depth ON region (depth);

COMMENT ON TABLE region IS '지역 (계층구조)';
COMMENT ON COLUMN region.id IS '지역 ID';
COMMENT ON COLUMN region.name IS '지역명';
COMMENT ON COLUMN region.parent_id IS '상위 지역 ID';
COMMENT ON COLUMN region.depth IS '계층 깊이 (0:시/도, 1:구/군, 2:동/읍/면)';


-- ============================================
-- 4. STORE (가게)
-- ============================================

CREATE TABLE store (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL,
  region_id BIGINT NOT NULL,
  address VARCHAR(200) NOT NULL,
  detailed_address VARCHAR(200) NULL,
  latitude DECIMAL(10,8) NOT NULL,
  longitude DECIMAL(11,8) NOT NULL,
  avg_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  score_weighted DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  review_count INTEGER NOT NULL DEFAULT 0,
  review_count_valid INTEGER NOT NULL DEFAULT 0,
  is_blind BOOLEAN NOT NULL DEFAULT TRUE,
  scrap_count INTEGER NOT NULL DEFAULT 0,
  view_count INTEGER NOT NULL DEFAULT 0,
  price_range_lunch VARCHAR(50) NULL,
  price_range_dinner VARCHAR(50) NULL,
  is_parking BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_store_category FOREIGN KEY (category_id) REFERENCES category (id),
  CONSTRAINT fk_store_region FOREIGN KEY (region_id) REFERENCES region (id)
);

CREATE INDEX idx_store_category ON store (category_id);
CREATE INDEX idx_store_region ON store (region_id);
CREATE INDEX idx_store_score_weighted ON store (score_weighted);
CREATE INDEX idx_store_review_count_valid ON store (review_count_valid);
CREATE INDEX idx_store_is_blind ON store (is_blind);

CREATE TRIGGER update_store_updated_at
  BEFORE UPDATE ON store
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE store IS '가게';
COMMENT ON COLUMN store.id IS '가게 ID';
COMMENT ON COLUMN store.name IS '가게명';
COMMENT ON COLUMN store.category_id IS '카테고리 ID';
COMMENT ON COLUMN store.region_id IS '지역 ID';
COMMENT ON COLUMN store.address IS '주소';
COMMENT ON COLUMN store.detailed_address IS '상세주소';
COMMENT ON COLUMN store.latitude IS '위도';
COMMENT ON COLUMN store.longitude IS '경도';
COMMENT ON COLUMN store.avg_rating IS '평균 평점';
COMMENT ON COLUMN store.score_weighted IS '가중 평점';
COMMENT ON COLUMN store.review_count IS '전체 리뷰 수';
COMMENT ON COLUMN store.review_count_valid IS '유효 리뷰 수 (PUBLIC)';
COMMENT ON COLUMN store.is_blind IS '블라인드 여부 (리뷰 5개 미만)';
COMMENT ON COLUMN store.scrap_count IS '스크랩 수';
COMMENT ON COLUMN store.view_count IS '조회수';
COMMENT ON COLUMN store.price_range_lunch IS '점심 가격대';
COMMENT ON COLUMN store.price_range_dinner IS '저녁 가격대';
COMMENT ON COLUMN store.is_parking IS '주차 가능 여부';
COMMENT ON COLUMN store.created_at IS '등록일시';
COMMENT ON COLUMN store.updated_at IS '수정일시';


-- ============================================
-- 5. REVIEW (리뷰)
-- ============================================

CREATE TABLE review (
  id BIGSERIAL PRIMARY KEY,
  store_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  title TEXT NULL,
  content TEXT NOT NULL,
  party_size INTEGER NOT NULL,
  score_taste DECIMAL(3,2) NOT NULL,
  score_service DECIMAL(3,2) NOT NULL,
  score_ambiance DECIMAL(3,2) NOT NULL,  -- 패치: score_mood → score_ambiance
  score_value DECIMAL(3,2) NOT NULL,     -- 패치: score_price → score_value
  score_calculated DECIMAL(3,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  visit_date DATE NOT NULL,
  visit_count INTEGER NOT NULL DEFAULT 0,
  helpful_count INTEGER NOT NULL DEFAULT 0,
  admin_comment TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_review_store FOREIGN KEY (store_id) REFERENCES store (id) ON DELETE CASCADE,
  CONSTRAINT fk_review_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX idx_review_store ON review (store_id);
CREATE INDEX idx_review_member ON review (member_id);
CREATE INDEX idx_review_status ON review (status);
CREATE INDEX idx_review_created_at ON review (created_at);
CREATE INDEX idx_review_store_status ON review (store_id, status);

CREATE TRIGGER update_review_updated_at
  BEFORE UPDATE ON review
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE review IS '리뷰';
COMMENT ON COLUMN review.id IS '리뷰 ID';
COMMENT ON COLUMN review.store_id IS '가게 ID';
COMMENT ON COLUMN review.member_id IS '회원 ID';
COMMENT ON COLUMN review.title IS '제목';
COMMENT ON COLUMN review.content IS '리뷰 내용';
COMMENT ON COLUMN review.party_size IS '방문 인원 수';
COMMENT ON COLUMN review.score_taste IS '맛 점수 (0.00 ~ 5.00)';
COMMENT ON COLUMN review.score_service IS '서비스 점수 (0.00 ~ 5.00)';
COMMENT ON COLUMN review.score_ambiance IS '분위기 점수 (0.00 ~ 5.00)';
COMMENT ON COLUMN review.score_value IS '가성비 점수 (0.00 ~ 5.00)';
COMMENT ON COLUMN review.score_calculated IS '계산된 종합 점수 (가중합)';
COMMENT ON COLUMN review.status IS '리뷰 상태 (PENDING, APPROVED, REJECTED, BLIND_HELD, PUBLIC, SUSPENDED)';
COMMENT ON COLUMN review.visit_date IS '방문일';
COMMENT ON COLUMN review.visit_count IS '해당 가게 방문 횟수 (PUBLIC 기준)';
COMMENT ON COLUMN review.helpful_count IS '도움이 됨 수';
COMMENT ON COLUMN review.admin_comment IS '관리자 코멘트 (반려 사유 등)';
COMMENT ON COLUMN review.created_at IS '작성일시 (시간 감가상각 기준)';
COMMENT ON COLUMN review.updated_at IS '수정일시';

-- ============================================

CREATE TABLE member_store_visit (
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

CREATE INDEX idx_member_store_visit_member ON member_store_visit (member_id);
CREATE INDEX idx_member_store_visit_store ON member_store_visit (store_id);

CREATE TRIGGER update_member_store_visit_updated_at
  BEFORE UPDATE ON member_store_visit
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE member_store_visit IS '회원-가게별 누적 방문 횟수';
COMMENT ON COLUMN member_store_visit.member_id IS '회원 ID';
COMMENT ON COLUMN member_store_visit.store_id IS '가게 ID';
COMMENT ON COLUMN member_store_visit.visit_count IS '누적 방문 횟수 (PUBLIC 기준)';
COMMENT ON COLUMN member_store_visit.created_at IS '생성일시';
COMMENT ON COLUMN member_store_visit.updated_at IS '수정일시';


-- ============================================
-- 6. STORE_AWARD (가게 수상 이력)
-- ============================================

CREATE TABLE store_award (
  id BIGSERIAL PRIMARY KEY,
  store_id BIGINT NOT NULL,
  award_name VARCHAR(100) NOT NULL,
  award_grade VARCHAR(50) NULL,
  award_year INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_store_award_store FOREIGN KEY (store_id) REFERENCES store (id) ON DELETE CASCADE
);

CREATE INDEX idx_store_award_store ON store_award (store_id);
CREATE INDEX idx_store_award_year ON store_award (award_year);

CREATE TRIGGER update_store_award_updated_at
  BEFORE UPDATE ON store_award
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE store_award IS '가게 수상 이력';
COMMENT ON COLUMN store_award.id IS '수상 ID';
COMMENT ON COLUMN store_award.store_id IS '가게 ID';
COMMENT ON COLUMN store_award.award_name IS '수상명 (예: 미슐랭, 블루리본)';
COMMENT ON COLUMN store_award.award_grade IS '수상 등급 (예: 1스타, 2스타)';
COMMENT ON COLUMN store_award.award_year IS '수상 연도';
COMMENT ON COLUMN store_award.created_at IS '등록일시';
COMMENT ON COLUMN store_award.updated_at IS '수정일시';


-- ============================================
-- 7. REVIEW_IMAGE (리뷰 이미지)
-- ============================================

CREATE TABLE review_image (
  id BIGSERIAL PRIMARY KEY,
  review_id BIGINT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  display_order INTEGER NOT NULL DEFAULT 0,

  CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES review (id) ON DELETE CASCADE
);

CREATE INDEX idx_review_image_review ON review_image (review_id);
CREATE INDEX idx_review_image_order ON review_image (review_id, display_order);

COMMENT ON TABLE review_image IS '리뷰 이미지';
COMMENT ON COLUMN review_image.id IS '이미지 ID';
COMMENT ON COLUMN review_image.review_id IS '리뷰 ID';
COMMENT ON COLUMN review_image.image_url IS '이미지 URL';
COMMENT ON COLUMN review_image.display_order IS '표시 순서';


-- ============================================
-- 8. REVIEW_HELPFUL (리뷰 도움됨)
-- ============================================

CREATE TABLE review_helpful (
  id BIGSERIAL PRIMARY KEY,
  review_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_review_helpful UNIQUE (review_id, member_id),
  CONSTRAINT fk_review_helpful_review FOREIGN KEY (review_id) REFERENCES review (id) ON DELETE CASCADE,
  CONSTRAINT fk_review_helpful_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX idx_review_helpful_review ON review_helpful (review_id);
CREATE INDEX idx_review_helpful_member ON review_helpful (member_id);

CREATE TRIGGER update_review_helpful_updated_at
  BEFORE UPDATE ON review_helpful
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE review_helpful IS '리뷰 도움됨';
COMMENT ON COLUMN review_helpful.id IS '도움됨 ID';
COMMENT ON COLUMN review_helpful.review_id IS '리뷰 ID';
COMMENT ON COLUMN review_helpful.member_id IS '회원 ID';
COMMENT ON COLUMN review_helpful.created_at IS '도움됨 일시';
COMMENT ON COLUMN review_helpful.updated_at IS '수정일시';


-- ============================================
-- 9. BOARD (게시글)
-- ============================================

CREATE TABLE board (
  id BIGSERIAL PRIMARY KEY,
  member_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  type VARCHAR(50) NOT NULL,
  view_count INTEGER NOT NULL DEFAULT 0,
  like_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_board_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX idx_board_member ON board (member_id);
CREATE INDEX idx_board_type ON board (type);
CREATE INDEX idx_board_created_at ON board (created_at);

CREATE TRIGGER update_board_updated_at
  BEFORE UPDATE ON board
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE board IS '게시글';
COMMENT ON COLUMN board.id IS '게시글 ID';
COMMENT ON COLUMN board.member_id IS '회원 ID';
COMMENT ON COLUMN board.title IS '제목';
COMMENT ON COLUMN board.content IS '내용';
COMMENT ON COLUMN board.type IS '게시글 유형 (NOTICE, FAQ, REVIEW_GUIDE, EVENT)';
COMMENT ON COLUMN board.view_count IS '조회수';
COMMENT ON COLUMN board.like_count IS '좋아요 수';
COMMENT ON COLUMN board.created_at IS '작성일시';
COMMENT ON COLUMN board.updated_at IS '수정일시';


-- ============================================
-- 10. COMMENT (댓글)
-- ============================================

CREATE TABLE comment (
  id BIGSERIAL PRIMARY KEY,
  member_id BIGINT NOT NULL,
  review_id BIGINT NULL,
  board_id BIGINT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_comment_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_review FOREIGN KEY (review_id) REFERENCES review (id) ON DELETE CASCADE,
  CONSTRAINT fk_comment_board FOREIGN KEY (board_id) REFERENCES board (id) ON DELETE CASCADE,
  CONSTRAINT chk_comment_target CHECK (
    (review_id IS NOT NULL AND board_id IS NULL) OR
    (review_id IS NULL AND board_id IS NOT NULL)
  )
);

CREATE INDEX idx_comment_review ON comment (review_id);
CREATE INDEX idx_comment_board ON comment (board_id);
CREATE INDEX idx_comment_member ON comment (member_id);

CREATE TRIGGER update_comment_updated_at
  BEFORE UPDATE ON comment
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE comment IS '댓글';
COMMENT ON COLUMN comment.id IS '댓글 ID';
COMMENT ON COLUMN comment.member_id IS '회원 ID';
COMMENT ON COLUMN comment.review_id IS '리뷰 ID (리뷰 댓글인 경우)';
COMMENT ON COLUMN comment.board_id IS '게시글 ID (게시글 댓글인 경우)';
COMMENT ON COLUMN comment.content IS '댓글 내용';
COMMENT ON COLUMN comment.created_at IS '작성일시';
COMMENT ON COLUMN comment.updated_at IS '수정일시';


-- ============================================
-- 11. STORE_SCRAP (가게 스크랩)
-- ============================================

CREATE TABLE store_scrap (
  id BIGSERIAL PRIMARY KEY,
  store_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_store_scrap UNIQUE (store_id, member_id),
  CONSTRAINT fk_store_scrap_store FOREIGN KEY (store_id) REFERENCES store (id) ON DELETE CASCADE,
  CONSTRAINT fk_store_scrap_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

CREATE INDEX idx_store_scrap_store ON store_scrap (store_id);
CREATE INDEX idx_store_scrap_member ON store_scrap (member_id);

CREATE TRIGGER update_store_scrap_updated_at
  BEFORE UPDATE ON store_scrap
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE store_scrap IS '가게 스크랩';
COMMENT ON COLUMN store_scrap.id IS '스크랩 ID';
COMMENT ON COLUMN store_scrap.store_id IS '가게 ID';
COMMENT ON COLUMN store_scrap.member_id IS '회원 ID';
COMMENT ON COLUMN store_scrap.created_at IS '스크랩 일시';
COMMENT ON COLUMN store_scrap.updated_at IS '수정일시';


-- ============================================
-- 12. MEMBER_FOLLOW (회원 팔로우)
-- ============================================

CREATE TABLE member_follow (
  id BIGSERIAL PRIMARY KEY,
  follower_id BIGINT NOT NULL,
  following_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT uk_member_follow UNIQUE (follower_id, following_id),
  CONSTRAINT fk_member_follow_follower FOREIGN KEY (follower_id) REFERENCES member (id) ON DELETE CASCADE,
  CONSTRAINT fk_member_follow_following FOREIGN KEY (following_id) REFERENCES member (id) ON DELETE CASCADE,
  CONSTRAINT chk_member_follow_self CHECK (follower_id != following_id)
);

CREATE INDEX idx_member_follow_follower ON member_follow (follower_id);
CREATE INDEX idx_member_follow_following ON member_follow (following_id);

CREATE TRIGGER update_member_follow_updated_at
  BEFORE UPDATE ON member_follow
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE member_follow IS '회원 팔로우';
COMMENT ON COLUMN member_follow.id IS '팔로우 ID';
COMMENT ON COLUMN member_follow.follower_id IS '팔로워 ID (팔로우 하는 사람)';
COMMENT ON COLUMN member_follow.following_id IS '팔로잉 ID (팔로우 받는 사람)';
COMMENT ON COLUMN member_follow.created_at IS '팔로우 일시';
COMMENT ON COLUMN member_follow.updated_at IS '수정일시';


-- ============================================
-- 끝
-- ============================================
