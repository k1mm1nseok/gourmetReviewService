-- ============================================
-- Gourmet Review Service Database Schema v1.3.2
-- ERDCloud Import용 DDL Script
-- ============================================

-- 1. MEMBER (회원)
CREATE TABLE `member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
  `email` VARCHAR(100) NOT NULL COMMENT '이메일',
  `nickname` VARCHAR(50) NOT NULL COMMENT '닉네임',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '권한 (USER, ADMIN)',
  `tier` VARCHAR(20) NOT NULL DEFAULT 'BRONZE' COMMENT '회원 등급 (BRONZE, SILVER, GOLD, GOURMET, BLACK)',
  `helpful_count` INT NOT NULL DEFAULT 0 COMMENT '누적 도움됨 수',
  `review_count` INT NOT NULL DEFAULT 0 COMMENT '누적 리뷰 수',
  `violation_count` INT NOT NULL DEFAULT 0 COMMENT '누적 위반 횟수',
  `last_review_at` DATETIME NULL COMMENT '마지막 리뷰 작성일시',
  `is_deviation_target` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '편차 보정 대상 여부',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_email` (`email`),
  UNIQUE KEY `uk_member_nickname` (`nickname`),
  KEY `idx_member_tier` (`tier`),
  KEY `idx_member_last_review_at` (`last_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원';

-- 2. CATEGORY (카테고리)
CREATE TABLE `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '카테고리명',
  `parent_id` BIGINT NULL COMMENT '상위 카테고리 ID',
  `depth` INT NOT NULL DEFAULT 0 COMMENT '계층 깊이',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent` (`parent_id`),
  KEY `idx_category_depth` (`depth`),
  CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='카테고리 (계층구조)';

-- 3. REGION (지역)
CREATE TABLE `region` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '지역 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '지역명',
  `parent_id` BIGINT NULL COMMENT '상위 지역 ID',
  `depth` INT NOT NULL DEFAULT 0 COMMENT '계층 깊이 (0:시/도, 1:구/군, 2:동/읍/면)',
  PRIMARY KEY (`id`),
  KEY `idx_region_parent` (`parent_id`),
  KEY `idx_region_depth` (`depth`),
  CONSTRAINT `fk_region_parent` FOREIGN KEY (`parent_id`) REFERENCES `region` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='지역 (계층구조)';

-- 4. STORE (가게)
CREATE TABLE `store` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '가게 ID',
  `name` VARCHAR(100) NOT NULL COMMENT '가게명',
  `category_id` BIGINT NOT NULL COMMENT '카테고리 ID',
  `region_id` BIGINT NOT NULL COMMENT '지역 ID',
  `address` VARCHAR(200) NOT NULL COMMENT '주소',
  `detailed_address` VARCHAR(200) NULL COMMENT '상세주소',
  `latitude` DECIMAL(10,8) NOT NULL COMMENT '위도',
  `longitude` DECIMAL(11,8) NOT NULL COMMENT '경도',
  `avg_rating` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '평균 평점',
  `score_weighted` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '가중 평점',
  `review_count` INT NOT NULL DEFAULT 0 COMMENT '전체 리뷰 수',
  `review_count_valid` INT NOT NULL DEFAULT 0 COMMENT '유효 리뷰 수 (PUBLIC)',
  `is_blind` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '블라인드 여부 (리뷰 5개 미만)',
  `scrap_count` INT NOT NULL DEFAULT 0 COMMENT '스크랩 수',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '조회수',
  `price_range_lunch` VARCHAR(50) NULL COMMENT '점심 가격대',
  `price_range_dinner` VARCHAR(50) NULL COMMENT '저녁 가격대',
  `is_parking` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '주차 가능 여부',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  KEY `idx_store_category` (`category_id`),
  KEY `idx_store_region` (`region_id`),
  KEY `idx_store_score_weighted` (`score_weighted`),
  KEY `idx_store_review_count_valid` (`review_count_valid`),
  KEY `idx_store_is_blind` (`is_blind`),
  CONSTRAINT `fk_store_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
  CONSTRAINT `fk_store_region` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='가게';

-- 5. REVIEW (리뷰)
CREATE TABLE `review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '리뷰 ID',
  `store_id` BIGINT NOT NULL COMMENT '가게 ID',
  `member_id` BIGINT NOT NULL COMMENT '회원 ID',
  `content` TEXT NOT NULL COMMENT '리뷰 내용',
  `score_taste` DECIMAL(3,2) NOT NULL COMMENT '맛 점수',
  `score_service` DECIMAL(3,2) NOT NULL COMMENT '서비스 점수',
  `score_mood` DECIMAL(3,2) NOT NULL COMMENT '분위기 점수',
  `score_price` DECIMAL(3,2) NOT NULL COMMENT '가격 점수',
  `score_calculated` DECIMAL(3,2) NOT NULL COMMENT '계산된 종합 점수 (가중합)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '리뷰 상태 (PENDING, APPROVED, REJECTED, BLIND_HELD, PUBLIC, SUSPENDED)',
  `is_revisit` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '재방문 여부',
  `visit_date` DATE NOT NULL COMMENT '방문일',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '좋아요 수',
  `admin_comment` TEXT NULL COMMENT '관리자 코멘트 (반려 사유 등)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일시 (시간 감가상각 기준)',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  KEY `idx_review_store` (`store_id`),
  KEY `idx_review_member` (`member_id`),
  KEY `idx_review_status` (`status`),
  KEY `idx_review_created_at` (`created_at`),
  KEY `idx_review_store_status` (`store_id`, `status`),
  CONSTRAINT `fk_review_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리뷰';

-- 6. STORE_AWARD (가게 수상 이력)
CREATE TABLE `store_award` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '수상 ID',
  `store_id` BIGINT NOT NULL COMMENT '가게 ID',
  `award_name` VARCHAR(100) NOT NULL COMMENT '수상명 (예: 미슐랭, 블루리본)',
  `award_grade` VARCHAR(50) NULL COMMENT '수상 등급 (예: 1스타, 2스타)',
  `award_year` INT NOT NULL COMMENT '수상 연도',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  KEY `idx_store_award_store` (`store_id`),
  KEY `idx_store_award_year` (`award_year`),
  CONSTRAINT `fk_store_award_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='가게 수상 이력';

-- 7. REVIEW_IMAGE (리뷰 이미지)
CREATE TABLE `review_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '이미지 ID',
  `review_id` BIGINT NOT NULL COMMENT '리뷰 ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '이미지 URL',
  `display_order` INT NOT NULL DEFAULT 0 COMMENT '표시 순서',
  PRIMARY KEY (`id`),
  KEY `idx_review_image_review` (`review_id`),
  KEY `idx_review_image_order` (`review_id`, `display_order`),
  CONSTRAINT `fk_review_image_review` FOREIGN KEY (`review_id`) REFERENCES `review` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리뷰 이미지';

-- 8. REVIEW_LIKE (리뷰 좋아요)
CREATE TABLE `review_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '좋아요 ID',
  `review_id` BIGINT NOT NULL COMMENT '리뷰 ID',
  `member_id` BIGINT NOT NULL COMMENT '회원 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '좋아요 일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_like` (`review_id`, `member_id`),
  KEY `idx_review_like_review` (`review_id`),
  KEY `idx_review_like_member` (`member_id`),
  CONSTRAINT `fk_review_like_review` FOREIGN KEY (`review_id`) REFERENCES `review` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_like_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='리뷰 좋아요';

-- 9. BOARD (게시글)
CREATE TABLE `board` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '게시글 ID',
  `member_id` BIGINT NOT NULL COMMENT '회원 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '제목',
  `content` TEXT NOT NULL COMMENT '내용',
  `type` VARCHAR(50) NOT NULL COMMENT '게시글 유형 (NOTICE, FAQ, REVIEW_GUIDE, EVENT)',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '조회수',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '좋아요 수',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  KEY `idx_board_member` (`member_id`),
  KEY `idx_board_type` (`type`),
  KEY `idx_board_created_at` (`created_at`),
  CONSTRAINT `fk_board_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='게시글';

-- 10. COMMENT (댓글)
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '댓글 ID',
  `member_id` BIGINT NOT NULL COMMENT '회원 ID',
  `review_id` BIGINT NULL COMMENT '리뷰 ID (리뷰 댓글인 경우)',
  `board_id` BIGINT NULL COMMENT '게시글 ID (게시글 댓글인 경우)',
  `content` TEXT NOT NULL COMMENT '댓글 내용',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  KEY `idx_comment_review` (`review_id`),
  KEY `idx_comment_board` (`board_id`),
  KEY `idx_comment_member` (`member_id`),
  CONSTRAINT `fk_comment_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_review` FOREIGN KEY (`review_id`) REFERENCES `review` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_board` FOREIGN KEY (`board_id`) REFERENCES `board` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_comment_target` CHECK (
    (`review_id` IS NOT NULL AND `board_id` IS NULL) OR
    (`review_id` IS NULL AND `board_id` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='댓글';

-- 11. STORE_SCRAP (가게 스크랩)
CREATE TABLE `store_scrap` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '스크랩 ID',
  `store_id` BIGINT NOT NULL COMMENT '가게 ID',
  `member_id` BIGINT NOT NULL COMMENT '회원 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '스크랩 일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_scrap` (`store_id`, `member_id`),
  KEY `idx_store_scrap_store` (`store_id`),
  KEY `idx_store_scrap_member` (`member_id`),
  CONSTRAINT `fk_store_scrap_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_store_scrap_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='가게 스크랩';

-- 12. MEMBER_FOLLOW (회원 팔로우)
CREATE TABLE `member_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '팔로우 ID',
  `follower_id` BIGINT NOT NULL COMMENT '팔로워 ID (팔로우 하는 사람)',
  `following_id` BIGINT NOT NULL COMMENT '팔로잉 ID (팔로우 받는 사람)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '팔로우 일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_follow` (`follower_id`, `following_id`),
  KEY `idx_member_follow_follower` (`follower_id`),
  KEY `idx_member_follow_following` (`following_id`),
  CONSTRAINT `fk_member_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `member` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_member_follow_following` FOREIGN KEY (`following_id`) REFERENCES `member` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_member_follow_self` CHECK (`follower_id` != `following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 팔로우';
