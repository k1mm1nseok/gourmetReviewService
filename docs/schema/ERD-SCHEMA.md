# Gourmet Review Service - Database Schema

## 개요
한국형 미식 검증 플랫폼의 최종 데이터베이스 스키마 정의
- 기능/정책 문서 v1.3.3 반영
- Spring Boot JPA/Hibernate 기반 설계

---

## 1. MEMBER (사용자)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 회원 ID |
| email | VARCHAR(100) | UNIQUE, NOT NULL | 이메일 |
| nickname | VARCHAR(50) | UNIQUE, NOT NULL | 닉네임 |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | 권한 (USER, ADMIN) |
| tier | VARCHAR(20) | NOT NULL, DEFAULT 'BRONZE' | 회원 등급 (BRONZE, SILVER, GOLD, GOURMET, BLACK) |
| helpful_count | INT | NOT NULL, DEFAULT 0 | 누적 도움됨 수 |
| review_count | INT | NOT NULL, DEFAULT 0 | 누적 리뷰 수 |
| violation_count | INT | NOT NULL, DEFAULT 0 | 누적 위반 횟수 |
| last_review_at | DATETIME | NULL | 마지막 리뷰 작성일시 (활동성 체크용) |
| is_deviation_target | BOOLEAN | NOT NULL, DEFAULT false | 편차 보정 대상 여부 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 가입일시 |

**Indexes:**
- UNIQUE INDEX: email, nickname
- INDEX: tier, last_review_at

---

## 2. STORE (가게)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 가게 ID |
| name | VARCHAR(100) | NOT NULL | 가게명 |
| category_id | BIGINT | FK(CATEGORY), NOT NULL | 카테고리 ID |
| region_id | BIGINT | FK(REGION), NOT NULL | 지역 ID |
| address | VARCHAR(200) | NOT NULL | 주소 |
| detailed_address | VARCHAR(200) | NULL | 상세주소 |
| latitude | DECIMAL(10,8) | NOT NULL | 위도 |
| longitude | DECIMAL(11,8) | NOT NULL | 경도 |
| avg_rating | DECIMAL(3,2) | NOT NULL, DEFAULT 0.00 | 평균 평점 |
| score_weighted | DECIMAL(3,2) | NOT NULL, DEFAULT 0.00 | 가중 평점 |
| review_count | INT | NOT NULL, DEFAULT 0 | 전체 리뷰 수 |
| review_count_valid | INT | NOT NULL, DEFAULT 0 | 유효 리뷰 수 (PUBLIC 상태) |
| is_blind | BOOLEAN | NOT NULL, DEFAULT true | 블라인드 여부 (리뷰 5개 미만) |
| scrap_count | INT | NOT NULL, DEFAULT 0 | 스크랩 수 |
| view_count | INT | NOT NULL, DEFAULT 0 | 조회수 |
| price_range_lunch | VARCHAR(50) | NULL | 점심 가격대 |
| price_range_dinner | VARCHAR(50) | NULL | 저녁 가격대 |
| is_parking | BOOLEAN | NOT NULL, DEFAULT false | 주차 가능 여부 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 등록일시 |

**Indexes:**
- INDEX: category_id, region_id
- INDEX: score_weighted, review_count_valid
- INDEX: is_blind
- SPATIAL INDEX: (latitude, longitude)

---

## 3. REVIEW (리뷰)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 리뷰 ID |
| store_id | BIGINT | FK(STORE), NOT NULL | 가게 ID |
| member_id | BIGINT | FK(MEMBER), NOT NULL | 회원 ID |
| title | TEXT | NULL | 제목 |
| content | TEXT | NOT NULL | 리뷰 내용 |
| party_size | INT | NOT NULL | 방문 인원 수 |
| score_taste | DECIMAL(3,2) | NOT NULL | 맛 점수 (0.00~5.00) |
| score_service | DECIMAL(3,2) | NOT NULL | 서비스 점수 (0.00~5.00) |
| score_mood | DECIMAL(3,2) | NOT NULL | 분위기 점수 (0.00~5.00) |
| score_price | DECIMAL(3,2) | NOT NULL | 가격 점수 (0.00~5.00) |
| score_calculated | DECIMAL(3,2) | NOT NULL | 계산된 종합 점수 (가중합) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 리뷰 상태 (PENDING, APPROVED, REJECTED, BLIND_HELD, PUBLIC, SUSPENDED) |
| visit_date | DATE | NOT NULL | 방문일 |
| visit_count | INT | NOT NULL, DEFAULT 0 | 해당 가게 방문 횟수 (PUBLIC 기준) |
| helpful_count | INT | NOT NULL, DEFAULT 0 | 도움이 됨 수 |
| admin_comment | TEXT | NULL | 관리자 코멘트 (반려 사유 등) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 작성일시 (시간 감가상각 기준, updatable=false) |
| updated_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**Indexes:**
- INDEX: store_id, member_id
- INDEX: status, created_at
- COMPOSITE INDEX: (store_id, status)

---

## 3-1. MEMBER_STORE_VISIT (회원-가게 방문 횟수)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 방문 ID |
| member_id | BIGINT | FK(MEMBER), NOT NULL | 회원 ID |
| store_id | BIGINT | FK(STORE), NOT NULL | 가게 ID |
| visit_count | INT | NOT NULL, DEFAULT 0 | 누적 방문 횟수 (PUBLIC 기준) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**Indexes:**
- UNIQUE INDEX: (member_id, store_id)
- INDEX: member_id, store_id

---

## 4. CATEGORY (카테고리)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 카테고리 ID |
| name | VARCHAR(50) | NOT NULL | 카테고리명 |
| parent_id | BIGINT | FK(CATEGORY), NULL | 상위 카테고리 ID (자기참조) |
| depth | INT | NOT NULL, DEFAULT 0 | 계층 깊이 (0: 최상위) |

**Indexes:**
- INDEX: parent_id, depth

---

## 5. REGION (지역)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 지역 ID |
| name | VARCHAR(50) | NOT NULL | 지역명 |
| parent_id | BIGINT | FK(REGION), NULL | 상위 지역 ID (자기참조) |
| depth | INT | NOT NULL, DEFAULT 0 | 계층 깊이 (0: 시/도, 1: 구/군, 2: 동/읍/면) |

**Indexes:**
- INDEX: parent_id, depth

---

## 6. STORE_AWARD (가게 수상 이력)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 수상 ID |
| store_id | BIGINT | FK(STORE), NOT NULL | 가게 ID |
| award_name | VARCHAR(100) | NOT NULL | 수상명 (예: 미슐랭, 블루리본) |
| award_grade | VARCHAR(50) | NULL | 수상 등급 (예: 1스타, 2스타) |
| award_year | INT | NOT NULL | 수상 연도 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 등록일시 |

**Indexes:**
- INDEX: store_id, award_year

---

## 7. REVIEW_IMAGE (리뷰 이미지)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 이미지 ID |
| review_id | BIGINT | FK(REVIEW), NOT NULL | 리뷰 ID |
| image_url | VARCHAR(500) | NOT NULL | 이미지 URL |
| display_order | INT | NOT NULL, DEFAULT 0 | 표시 순서 |

**Indexes:**
- INDEX: review_id, display_order

---

## 8. REVIEW_HELPFUL (리뷰 도움됨)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 도움됨 ID |
| review_id | BIGINT | FK(REVIEW), NOT NULL | 리뷰 ID |
| member_id | BIGINT | FK(MEMBER), NOT NULL | 회원 ID |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 도움됨 일시 |

**Indexes:**
- UNIQUE INDEX: (review_id, member_id) - 중복 방지
- INDEX: member_id

---

## 9. COMMENT (댓글)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 댓글 ID |
| member_id | BIGINT | FK(MEMBER), NOT NULL | 회원 ID |
| review_id | BIGINT | FK(REVIEW), NULL | 리뷰 ID (리뷰 댓글인 경우) |
| board_id | BIGINT | FK(BOARD), NULL | 게시글 ID (게시글 댓글인 경우) |
| content | TEXT | NOT NULL | 댓글 내용 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 작성일시 |

**Constraints:**
- CHECK: (review_id IS NOT NULL) XOR (board_id IS NOT NULL) - 둘 중 하나만 존재

**Indexes:**
- INDEX: review_id, board_id, member_id

---

## 10. BOARD (게시글)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 게시글 ID |
| member_id | BIGINT | FK(MEMBER), NOT NULL | 회원 ID |
| title | VARCHAR(200) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| type | VARCHAR(50) | NOT NULL | 게시글 유형 (NOTICE, FAQ, REVIEW_GUIDE 등) |
| view_count | INT | NOT NULL, DEFAULT 0 | 조회수 |
| like_count | INT | NOT NULL, DEFAULT 0 | 좋아요 수 |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 작성일시 |

**Indexes:**
- INDEX: member_id, type
- INDEX: created_at

---

## 11. STORE_SCRAP (가게 스크랩)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 스크랩 ID |
| store_id | BIGINT | FK(STORE), NOT NULL | 가게 ID |
| member_id | BIGINT | FK(MEMBER), NOT NULL | 회원 ID |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 스크랩 일시 |

**Indexes:**
- UNIQUE INDEX: (store_id, member_id) - 중복 방지
- INDEX: member_id

---

## 12. MEMBER_FOLLOW (회원 팔로우)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 팔로우 ID |
| follower_id | BIGINT | FK(MEMBER), NOT NULL | 팔로워 ID (팔로우 하는 사람) |
| following_id | BIGINT | FK(MEMBER), NOT NULL | 팔로잉 ID (팔로우 받는 사람) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 팔로우 일시 |

**Constraints:**
- CHECK: follower_id != following_id - 자기 자신 팔로우 방지

**Indexes:**
- UNIQUE INDEX: (follower_id, following_id) - 중복 방지
- INDEX: following_id

---

## ERD 관계 정의

```
MEMBER (1) ----< (N) REVIEW
MEMBER (1) ----< (N) REVIEW_HELPFUL
MEMBER (1) ----< (N) COMMENT
MEMBER (1) ----< (N) BOARD
MEMBER (1) ----< (N) STORE_SCRAP
MEMBER (1) ----< (N) MEMBER_FOLLOW (as follower)
MEMBER (1) ----< (N) MEMBER_FOLLOW (as following)

STORE (1) ----< (N) REVIEW
STORE (1) ----< (N) STORE_AWARD
STORE (1) ----< (N) STORE_SCRAP
STORE (N) ----< (1) CATEGORY
STORE (N) ----< (1) REGION

CATEGORY (1) ----< (N) CATEGORY (자기참조, parent-child)

REGION (1) ----< (N) REGION (자기참조, parent-child)

REVIEW (1) ----< (N) REVIEW_IMAGE
REVIEW (1) ----< (N) REVIEW_HELPFUL
REVIEW (1) ----< (N) COMMENT

BOARD (1) ----< (N) COMMENT
```

---

## 주요 비즈니스 규칙

### 1. 회원 등급 (tier) 산정
- BRONZE: 기본 등급 (가입 시)
- SILVER: review_count >= 10, helpful_count >= 30
- GOLD: review_count >= 30, helpful_count >= 100
- GOURMET: review_count >= 100, helpful_count >= 500
- BLACK: 관리자 지정 (professional reviewer)

### 2. 가게 블라인드 처리
- `is_blind = true`: `review_count_valid < 5`
- 블라인드 상태에서는 평점 및 리뷰 수 미공개

### 3. 리뷰 상태 흐름
```
PENDING (검수 대기)
  ↓
APPROVED (승인) → PUBLIC (공개) ⇄ SUSPENDED (일시정지)
  ↓                              ↑
REJECTED (반려) ← - - - - - - -┘
  ↓
BLIND_HELD (블라인드 보류, review_count < 5)
```

### 4. 리뷰 시간 감가상각
- `created_at` 기준으로 시간 경과에 따라 가중치 감소
- `updated_at`은 편집 이력 추적용으로만 사용 (감가상각 영향 없음)

### 5. 편차 보정 대상
- `is_deviation_target = true`: 평균 대비 ±2σ 이상 벗어난 리뷰 작성자
- 해당 회원의 리뷰는 가중치 감소 적용
