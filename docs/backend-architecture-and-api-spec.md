# Gourmet Review Service - Backend Architecture & API Specification

**Version:** 1.1
**Date:** 2025-12-31
**Based on:** review-policy v1.3.3, functional-requirements v1.0, agents.md

---

## Table of Contents

1. [Architecture](#architecture)
   - [Layered Structure](#layered-structure)
   - [Modules / Packages](#modules--packages)
2. [DB–Entity Mapping Review](#dbentity-mapping-review)
3. [API Specification](#api-specification)
   - [Members](#members)
   - [Restaurants](#restaurants)
   - [Reviews](#reviews)
   - [Admin / Moderation](#admin--moderation)

---

# Architecture

## Layered Structure

본 프로젝트는 **레이어드 아키텍처(Layered Architecture)** 기반으로 설계되었으며, Spring Boot 3.x, Java 21, PostgreSQL을 사용합니다.

### 레이어 구성

```
┌─────────────────────────────────────────────────────┐
│  Presentation Layer (Controller / API)              │
│  - HTTP 요청/응답 처리                                │
│  - DTO 변환 (Entity ↔ DTO)                           │
│  - ApiResponse<T> 래퍼로 통일된 응답 포맷 제공          │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Application Layer (Service / UseCase)              │
│  - 비즈니스 로직 구현                                  │
│  - 트랜잭션 경계 관리 (@Transactional)                │
│  - 도메인 서비스 호출                                  │
│  - 평점 계산, 등급 산정, 정책 적용 로직                │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Domain Layer (Entity / DomainService / Enum)       │
│  - 핵심 도메인 모델 (Entity)                          │
│  - 도메인 비즈니스 규칙 (Entity 내부 메서드)            │
│  - 도메인 상태 (Enum: MemberTier, ReviewStatus 등)    │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Infrastructure Layer (Repository / External)       │
│  - JPA Repository (Spring Data JPA)                 │
│  - DB 접근 및 영속성 관리                              │
│  - 외부 API 연동 (S3, 메일 등 - 추후 도입 시)          │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  Database Layer (PostgreSQL)                        │
│  - 데이터 저장 및 조회                                 │
│  - 인덱스, 제약조건 관리                               │
└─────────────────────────────────────────────────────┘
```

### 의존 방향 원칙

- **Controller → Service → Repository → DB** 단방향 의존
- 상위 레이어는 하위 레이어의 구현 세부사항에 의존하지 않음
- Entity는 직접 Controller에 노출되지 않고, **DTO를 통해 변환**하여 응답

---

## Modules / Packages

### 패키지 구조 (agents.md 기준)

```
src/main/java/com/gourmet/review
├── common/                      # 공통 모듈
│   ├── dto/                     # ApiResponse<T>, ErrorResponse 등
│   ├── exception/               # 커스텀 예외 클래스
│   └── util/                    # 공통 유틸리티
│
├── config/                      # 설정 파일
│   ├── SecurityConfig.java      # Spring Security 설정
│   ├── JpaConfig.java           # JPA Auditing 설정
│   └── SwaggerConfig.java       # API 문서 설정
│
├── domain/                      # 도메인별 패키지 (도메인 중심 구조)
│   ├── entity/                  # 엔티티 클래스
│   │   ├── BaseEntity.java      # 공통 엔티티 (created_at, updated_at)
│   │   ├── Member.java
│   │   ├── Store.java
│   │   ├── Review.java
│   │   ├── Category.java
│   │   ├── Region.java
│   │   ├── ReviewImage.java
│   │   ├── ReviewHelpful.java
│   │   ├── StoreScrap.java
│   │   ├── StoreAward.java
│   │   ├── MemberFollow.java
│   │   ├── Board.java
│   │   └── Comment.java
│   │
│   └── enums/                   # 도메인 Enum
│       ├── MemberRole.java      # 회원 권한 (USER, ADMIN)
│       ├── MemberTier.java      # 회원 등급 (BRONZE, SILVER, GOLD, GOURMET, BLACK)
│       └── ReviewStatus.java    # 리뷰 상태 (PENDING, APPROVED, REJECTED, PUBLIC 등)
│
├── member/                      # 회원 도메인
│   ├── controller/
│   │   └── MemberController.java
│   ├── service/
│   │   └── MemberService.java
│   ├── repository/
│   │   └── MemberRepository.java
│   └── dto/
│       ├── MemberRegisterRequest.java
│       ├── MemberResponse.java
│       └── MemberProfileUpdateRequest.java
│
├── store/                       # 식당 도메인
│   ├── controller/
│   │   └── StoreController.java
│   ├── service/
│   │   ├── StoreService.java
│   │   └── StoreScoreService.java  # 평점 계산 전용 서비스
│   ├── repository/
│   │   └── StoreRepository.java
│   └── dto/
│       ├── StoreRegisterRequest.java
│       ├── StoreResponse.java
│       ├── StoreDetailResponse.java
│       └── StoreSearchCondition.java
│
├── review/                      # 리뷰 도메인
│   ├── controller/
│   │   └── ReviewController.java
│   ├── service/
│   │   ├── ReviewService.java
│   │   └── ReviewModerationService.java  # 검수 전용 서비스
│   ├── repository/
│   │   └── ReviewRepository.java
│   └── dto/
│       ├── ReviewCreateRequest.java
│       ├── ReviewUpdateRequest.java
│       ├── ReviewResponse.java
│       └── ReviewDetailResponse.java
│
└── admin/                       # 관리자 도메인
    ├── controller/
    │   ├── AdminReviewController.java
    │   └── AdminMemberController.java
    ├── service/
    │   └── AdminModerationService.java
    └── dto/
        ├── ReviewModerationRequest.java
        └── MemberTierUpdateRequest.java
```

### 도메인별 책임

| 도메인 | 주요 책임 | 핵심 비즈니스 로직 |
|--------|-----------|-------------------|
| **member** | 회원 가입/로그인/프로필 관리 | 등급 자동 승급, 편차 보정 대상 판정, 활동성 체크 |
| **store** | 가게 정보 관리, 검색, 평점 계산 | 베이지안 평균, 가중 평점 산정, 블라인드 정책 |
| **review** | 리뷰 작성/수정/삭제, 검수 | 다차원 평점 계산, 쿨다운 시스템, 상태 전이 |
| **admin** | 운영자 검수, 제재, 통계 | 리뷰 승인/반려, 회원 등급 수동 조정, 어뷰징 탐지 |

---

# DB–Entity Mapping Review

## 검토 결과 요약

### ✅ 정상 매핑된 항목

1. **테이블 구조**: DDL과 Entity 클래스의 테이블 구조가 전반적으로 일치합니다.
2. **인덱스 전략**: 주요 조회 컬럼(`tier`, `status`, `score_weighted` 등)에 인덱스가 올바르게 설정되었습니다.
3. **연관관계 매핑**: `@ManyToOne`, `@JoinColumn` 설정이 외래키와 일치합니다.
4. **BaseEntity 상속**: `created_at`, `updated_at` 자동 관리가 JPA Auditing으로 구현되었습니다.

---

## ⚠️ 불일치 및 수정 권장 사항

### 1. 다차원 평점 가중치 불일치 (중요도: 높음)

**상태:** ✅ 해결됨

- 정책 문서(review-policy / functional-requirements): 40/30/15/15
- 현재 `Review.java` 구현도 40/30/15/15로 일치함

(과거 버전에서 50/20/15/15로 구현된 이력이 있었으나 현재는 정합화되어 있음)

---

### 2. 컬럼명 용어 불일치 (중요도: 중간)

**문제점:**
정책 문서와 DDL/Entity의 컬럼명이 서로 다릅니다.

| 정책 문서 용어 | DDL 컬럼명 | Entity 필드명 | 비고 |
|---------------|-----------|--------------|------|
| Taste (맛) | `score_taste` | `scoreTaste` | ✅ 일치 |
| **Value (가성비)** | `score_price` | `scorePrice` | ⚠️ 의미 불일치 (가성비 ≠ 가격) |
| **Ambiance (분위기)** | `score_mood` | `scoreMood` | ⚠️ 용어 불일치 (분위기 ≠ 무드) |
| Service (접객) | `score_service` | `scoreService` | ✅ 일치 |

**수정 권장:**

**옵션 1: DDL 컬럼명 변경 (권장)**
```sql
-- 마이그레이션 스크립트
ALTER TABLE review
  RENAME COLUMN score_price TO score_value;

ALTER TABLE review
  RENAME COLUMN score_mood TO score_ambiance;
```

**옵션 2: Entity 필드명만 변경 (하위 호환성 유지)**
```java
// Review.java (예시)
@Column(name = "score_price")
private BigDecimal scoreValue;  // 가성비로 의미 명확화

@Column(name = "score_mood")
private BigDecimal scoreAmbiance;  // 분위기로 용어 통일
```

---

### 3. MemberTier Enum 승급 요건 불일치 (중요도: 높음)

- (문서 작성 시점의) 요구사항과 코드 요건이 다른 부분이 있었으나,
  현재 프로젝트는 **기능 요구사항 v1.0을 기준으로 우선 구현**되었습니다.
- 승급/강등 로직은 배치(`ReviewPolicyJobServiceImpl.runTierEvaluation`)로 일부만 구현되어 있으며,
  "검수 통과 리뷰 수" 같은 정밀 조건은 스키마/집계 필드 부재로 TODO 상태입니다.

**현재 구현 상태(요약)**
- tier 가중치(0.5/1.0/1.5/2.0/0.0)는 정책대로 점수 계산에 반영됨
- tier 변경 시 과거 PUBLIC 리뷰가 반영되는 store 점수는 소급 재계산됨
- 관리자가 회원 tier를 수동 변경하는 API가 존재함: `PATCH /admin/members/{memberId}/tier`

---

## Scoring / Trigger Notes (Implementation)

- Store 점수/카운트 재계산은 `ReviewScoreService`가 담당하며,
  이벤트/배치/정책에서는 storeId만 모은 뒤 `recalculateStoreScoresByStoreIds(...)` 단일 경로로 위임합니다.
- 방문횟수(`review.visitCount`, `member_store_visit.visit_count`)는 리뷰가 `PUBLIC`으로 전환되는 시점에 반영됩니다.
  - 운영자 승인 흐름 및 쿨다운 만료 자동 승인 흐름 모두 동일하게 적용됩니다.

---

### 4. 누락된 컬럼 (중요도: 낮음)

**Member 테이블:**
- DDL에는 존재하지만 functional-requirements에서 언급된 `password` 컬럼이 Entity에 누락되었습니다.
  - **수정 권장:** Member 엔티티에 `@Column(name = "password") private String password;` 추가

**Store 테이블:**
- 모든 필드 매핑 완료 ✅

**Review 테이블:**
- 모든 필드 매핑 완료 ✅

---

### 5. 타입 불일치 검토

| 테이블 | 컬럼명 | DDL 타입 | Entity 타입 | 일치 여부 |
|--------|--------|----------|-------------|----------|
| member | tier | VARCHAR(20) | MemberTier (ENUM → STRING) | ✅ |
| review | status | VARCHAR(20) | ReviewStatus (ENUM → STRING) | ✅ |
| review | score_calculated | DECIMAL(3,2) | BigDecimal | ✅ |
| store | score_weighted | DECIMAL(3,2) | BigDecimal | ✅ |
| store | latitude | DECIMAL(10,8) | BigDecimal | ✅ |
| store | longitude | DECIMAL(11,8) | BigDecimal | ✅ |

**결과:** 타입 매핑 모두 정상 ✅

---

### 6. 제약조건 검증

| 제약조건 | DDL | Entity | 일치 여부 |
|---------|-----|--------|----------|
| UNIQUE (member.email) | ✅ | `@Column(unique=true)` ✅ | ✅ |
| UNIQUE (member.nickname) | ✅ | `@Column(unique=true)` ✅ | ✅ |
| UNIQUE (review_helpful) | ✅ | `@UniqueConstraint` ✅ | ✅ |
| CHECK (comment.target) | ✅ | 코드 레벨 검증 필요 ⚠️ | ⚠️ |
| CHECK (member_follow.self) | ✅ | `isSelfFollow()` 메서드 존재 ✅ | ✅ |

**수정 권장 (Comment 엔티티):**
```java
// Comment.java에 Validation 추가
@PrePersist
@PreUpdate
private void validateTarget() {
    if ((review != null && board != null) || (review == null && board == null)) {
        throw new IllegalStateException("Comment must have exactly one target (review or board)");
    }
}
```

---

### 7. 연관관계 매핑 검증

| 관계 | DDL FK | Entity 매핑 | Fetch 전략 | 일치 여부 |
|------|--------|-------------|-----------|----------|
| Review → Store | `fk_review_store` | `@ManyToOne` | LAZY ✅ | ✅ |
| Review → Member | `fk_review_member` | `@ManyToOne` | LAZY ✅ | ✅ |
| Store → Category | `fk_store_category` | `@ManyToOne` | LAZY ✅ | ✅ |
| Store → Region | `fk_store_region` | `@ManyToOne` | LAZY ✅ | ✅ |
| Category → Parent | `fk_category_parent` | `@ManyToOne` | LAZY ✅ | ✅ |

**결과:** 모든 연관관계 정상 매핑 ✅

---

## 우선순위별 수정 작업 목록

### 🔴 High Priority (즉시 수정 필요)

1. **다차원 평점 가중치 수정** (`Review.java`)
   - 맛: 50% → 40%
   - 가격: 15% → 30% (가성비로 의미 변경)

2. **MemberTier 승급 요건 수정** (`MemberTier.java`)
   - SILVER: 리뷰 10개 → 5개

### 🟡 Medium Priority (다음 스프린트)

3. **컬럼명 의미 통일** (DDL 마이그레이션)
   - `score_price` → `score_value`
   - `score_mood` → `score_ambiance`

4. **Member 엔티티에 password 필드 추가**

### 🟢 Low Priority (추후 개선)

5. **Comment 엔티티에 검증 로직 추가** (@PrePersist)

6. **GOURMET 승급 로직 강화** (운영진 승인 플래그 추가)

---

# API Specification

## Common Response Envelope

모든 API 응답은 다음 공통 포맷을 따른다.

```json
{
  "success": true,
  "message": "optional",
  "data": {}
}
```

### Blind(Store.isBlind) 처리 규칙 (Frontend Contract)

`STORE.is_blind = true`(블라인드)인 경우, **점수는 노출하지 않는다(null)**.
텍스트/이미지/작성자/작성시각 등은 노출할 수 있다.

- 스토어 리스트/검색의 `scoreWeighted`는 `null`
- 스토어 상세의 `scoreWeighted`, `avgRating`는 `null`
- 스토어 상세의 `recentReviews[*].score*`는 `null`
- 스토어별 리뷰 목록의 `ReviewResponse.score*`는 `null`
- 리뷰 상세의 `ReviewDetailResponse.score*`는 `null`

(참고 구현: `StoreServiceImpl#getStoreDetail`, `ReviewServiceImpl#getStoreReviews`, `ReviewServiceImpl#getReview`)

---

## Members

### 1. 회원 가입

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /api/members/register` |
| **기능 요약** | 신규 회원 가입 (이메일 기반) |
| **Request Body** | `MemberRegisterRequest` |
| **Response** | `ApiResponse<MemberResponse>` |

**Request DTO:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "foodlover"
}
```

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "foodlover",
    "tier": "BRONZE",
    "role": "USER",
    "createdAt": "2025-12-11T10:00:00"
  }
}
```

---

### 2. 로그인

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /api/members/login` |
| **기능 요약** | 이메일/비밀번호 기반 로그인 (Session 또는 JWT 발급) |
| **Request Body** | `MemberLoginRequest` |
| **Response** | `ApiResponse<MemberLoginResponse>` |

**Request DTO:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "member": {
      "id": 1,
      "nickname": "foodlover",
      "tier": "SILVER"
    }
  }
}
```

---

### 3. 내 프로필 조회

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/members/me` |
| **기능 요약** | 로그인한 회원의 프로필 정보 조회 |
| **Request** | 인증 헤더 (`Authorization: Bearer {token}`) |
| **Response** | `ApiResponse<MemberProfileResponse>` |

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "프로필 조회 성공",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "foodlover",
    "tier": "SILVER",
    "reviewCount": 12,
    "helpfulCount": 45,
    "violationCount": 0,
    "lastReviewAt": "2025-12-10T15:30:00",
    "isActive": true
  }
}
```

---

### 4. 프로필 수정

| 항목 | 내용 |
|------|------|
| **Method + Path** | `PATCH /api/members/me` |
| **기능 요약** | 닉네임 등 프로필 정보 수정 |
| **Request Body** | `MemberProfileUpdateRequest` |
| **Response** | `ApiResponse<MemberResponse>` |

**Request DTO:**
```json
{
  "nickname": "newNickname"
}
```

---

### 5. 회원 팔로우

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /api/members/{memberId}/follow` |
| **기능 요약** | 특정 회원 팔로우 |
| **Path Parameter** | `memberId`: 팔로우할 회원 ID |
| **Response** | `ApiResponse<Void>` |

---

### 6. 회원 팔로우 취소

| 항목 | 내용 |
|------|------|
| **Method + Path** | `DELETE /api/members/{memberId}/follow` |
| **기능 요약** | 팔로우 취소 |
| **Path Parameter** | `memberId`: 팔로우 취소할 회원 ID |
| **Response** | `ApiResponse<Void>` |

---

### 7. 팔로워 목록 조회

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/members/{memberId}/followers` |
| **기능 요약** | 해당 회원을 팔로우하는 사람 목록 |
| **Path Parameter** | `memberId`: 회원 ID |
| **Query Parameters** | `page`, `size` (페이징) |
| **Response** | `ApiResponse<Page<MemberSimpleResponse>>` |

---

## Restaurants

### 1. 가게 등록 (관리자)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /api/stores` |
| **기능 요약** | 신규 가게 정보 등록 (관리자 전용) |
| **Request Body** | `StoreRegisterRequest` |
| **Response** | `ApiResponse<StoreResponse>` |
| **권한** | ADMIN |

**Request DTO:**
```json
{
  "name": "파스타하우스",
  "categoryId": 5,
  "regionId": 12,
  "address": "서울특별시 강남구 역삼동 123-45",
  "detailedAddress": "2층",
  "latitude": 37.12345678,
  "longitude": 127.12345678,
  "priceRangeLunch": "10000-15000",
  "priceRangeDinner": "20000-30000",
  "isParking": true
}
```

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "가게가 등록되었습니다.",
  "data": {
    "id": 123,
    "name": "파스타하우스",
    "categoryName": "이탈리안",
    "regionName": "역삼동",
    "address": "서울특별시 강남구 역삼동 123-45",
    "scoreWeighted": 0.00,
    "isBlind": true,
    "reviewCountValid": 0,
    "createdAt": "2025-12-11T10:00:00"
  }
}
```

---

### 2. 가게 상세 조회

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/stores/{storeId}` |
| **기능 요약** | 가게 상세 정보 + 리뷰 목록 조회 |
| **Path Parameter** | `storeId`: 가게 ID |
| **Response** | `ApiResponse<StoreDetailResponse>` |

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "가게 조회 성공",
  "data": {
    "id": 123,
    "name": "파스타하우스",
    "categoryName": "이탈리안",
    "regionName": "역삼동",
    "address": "서울특별시 강남구 역삼동 123-45",
    "detailedAddress": "2층",
    "latitude": 37.12345678,
    "longitude": 127.12345678,
    "scoreWeighted": 4.2,
    "avgRating": 4.5,
    "isBlind": false,
    "reviewCount": 150,
    "reviewCountValid": 150,
    "scrapCount": 23,
    "viewCount": 1234,
    "priceRangeLunch": "10000-15000",
    "priceRangeDinner": "20000-30000",
    "isParking": true,
    "awards": [
      {
        "awardName": "미슐랭 가이드",
        "awardGrade": "1스타",
        "awardYear": 2024
      }
    ],
    "recentReviews": [
      {
        "id": 456,
        "memberNickname": "foodlover",
        "memberTier": "GOLD",
        "scoreCalculated": 4.5,
        "scoreTaste": 4.5,
        "scoreValue": 4.0,
        "scoreAmbiance": 4.0,
        "scoreService": 4.5,
        "content": "파스타가 정말 맛있었습니다!",
        "images": ["https://cdn.example.com/image1.jpg"],
        "helpfulCount": 12,
        "createdAt": "2025-12-10T15:00:00"
      }
    ]
  }
}
```

**블라인드 상태 응답 (리뷰 5개 미만):**
```json
{
  "code": "SUCCESS",
  "message": "가게 조회 성공",
  "data": {
    "id": 123,
    "name": "신규 카페",
    "scoreWeighted": null,
    "isBlind": true,
    "blindMessage": "현재 4개의 리뷰가 수집되었습니다. 곧 평점이 공개됩니다.",
    "reviewCountValid": 4
  }
}
```

**블라인드 상태에서 recentReviews 노출 정책:**
- `recentReviews`는 제공되지만, 리뷰 점수(`scoreTaste/Value/Ambiance/Service`, `scoreCalculated`)는 `null`로 내려갑니다.
- 즉, **텍스트(content)/이미지/helpfulCount 등만 공개**됩니다.

---

### 3. 가게 검색 (조건별)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/stores/search` |
| **기능 요약** | 이름, 카테고리, 지역, 평점 범위 등으로 가게 검색 |
| **Query Parameters** | `StoreSearchCondition` |
| **Response** | `ApiResponse<Page<StoreResponse>>` |

**Query Parameters:**
```
?keyword=파스타
&categoryId=5
&regionId=12
&minScore=4.0
&maxScore=5.0
&sortBy=score_weighted  (정렬: score_weighted, review_count, created_at)
&sortDirection=desc
&page=0
&size=20
```

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "검색 결과",
  "data": {
    "content": [
      {
        "id": 123,
        "name": "파스타하우스",
        "categoryName": "이탈리안",
        "regionName": "역삼동",
        "scoreWeighted": 4.2,
        "reviewCountValid": 150,
        "isBlind": false,
        "thumbnailImage": "https://cdn.example.com/thumb.jpg"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 45,
    "totalPages": 3
  }
}
```

---

### 4. 가게 스크랩 (북마크)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /api/stores/{storeId}/scrap` |
| **기능 요약** | 가게를 내 스크랩 목록에 추가 |
| **Path Parameter** | `storeId`: 가게 ID |
| **Response** | `ApiResponse<Void>` |

---

### 5. 가게 스크랩 취소

| 항목 | 내용 |
|------|------|
| **Method + Path** | `DELETE /api/stores/{storeId}/scrap` |
| **기능 요약** | 스크랩 취소 |
| **Path Parameter** | `storeId`: 가게 ID |
| **Response** | `ApiResponse<Void>` |

---

### 6. 내 스크랩 목록 조회

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/members/me/scraps` |
| **기능 요약** | 로그인한 회원이 스크랩한 가게 목록 |
| **Query Parameters** | `page`, `size` |
| **Response** | `ApiResponse<Page<StoreResponse>>` |

---

## Reviews

### 1. 리뷰 작성

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /api/reviews` |
| **기능 요약** | 특정 가게에 리뷰 작성 |
| **Request Body** | `ReviewCreateRequest` |
| **Response** | `ApiResponse<ReviewResponse>` |

**Request DTO:**
```json
{
  "storeId": 123,
  "title": "파스타 맛집 후기",
  "partySize": 2,
  "scoreTaste": 4.5,
  "scoreValue": 4.0,
  "scoreAmbiance": 4.0,
  "scoreService": 4.5,
  "content": "파스타가 정말 맛있었습니다. 분위기도 좋고 재방문 의사 100%입니다!",
  "visitDate": "2025-12-10",
  "images": [
    "https://cdn.example.com/image1.jpg",
    "https://cdn.example.com/image2.jpg"
  ]
}
```
* `title`은 선택 입력입니다.

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "리뷰가 작성되었습니다. 운영자 검수 후 공개됩니다.",
  "data": {
    "id": 456,
    "storeId": 123,
    "storeName": "파스타하우스",
    "scoreCalculated": 4.275,
    "visitCount": 1,
    "status": "PENDING",
    "helpfulCount": 0,
    "isHelpfulByMe": false,
    "createdAt": "2025-12-11T10:00:00"
  }
}
```

**비즈니스 로직:**
1. `scoreCalculated` 자동 계산 (가중 평균: 맛 40%, 가성비 30%, 분위기 15%, 접객 15%)
2. 1점 또는 5점 리뷰는 **쿨다운 12시간 적용** (`PENDING` 상태 유지)
3. Gold 등급 이상은 쿨다운 면제
4. 초기 운영 모드에서는 모든 리뷰가 `PENDING` 상태로 시작

---

### 2. 리뷰 수정

| 항목 | 내용 |
|------|------|
| **Method + Path** | `PATCH /api/reviews/{reviewId}` |
| **기능 요약** | 작성한 리뷰 수정 (본인만 가능) |
| **Path Parameter** | `reviewId`: 리뷰 ID |
| **Request Body** | `ReviewUpdateRequest` |
| **Response** | `ApiResponse<ReviewResponse>` |

**Request DTO:**
```json
{
  "title": "수정된 제목",
  "partySize": 2,
  "scoreTaste": 4.0,
  "scoreValue": 3.5,
  "scoreAmbiance": 4.0,
  "scoreService": 4.0,
  "content": "수정된 리뷰 내용"
}
```
* `title`은 선택 입력입니다.

**중요:** `created_at`은 변경되지 않음 (시간 감가상각 유지)

---

### 3. 리뷰 삭제

| 항목 | 내용 |
|------|------|
| **Method + Path** | `DELETE /api/reviews/{reviewId}` |
| **기능 요약** | 리뷰 삭제 (본인 또는 관리자) |
| **Path Parameter** | `reviewId`: 리뷰 ID |
| **Response** | `ApiResponse<Void>` |

---

### 4. 리뷰 상세 조회

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/reviews/{reviewId}` |
| **기능 요약** | 특정 리뷰 상세 정보 조회 |
| **Path Parameter** | `reviewId`: 리뷰 ID |
| **Response** | `ApiResponse<ReviewDetailResponse>` |

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "리뷰 조회 성공",
  "data": {
    "id": 456,
    "store": {
      "id": 123,
      "name": "파스타하우스"
    },
    "member": {
      "id": 1,
      "nickname": "foodlover",
      "tier": "GOLD"
    },
    "scoreTaste": 4.5,
    "scoreValue": 4.0,
    "scoreAmbiance": 4.0,
    "scoreService": 4.5,
    "scoreCalculated": 4.275,
    "content": "파스타가 정말 맛있었습니다!",
    "visitDate": "2025-12-10",
    "visitCount": 2,
    "helpfulCount": 12,
    "isHelpfulByMe": true,
    "status": "PUBLIC",
    "images": [
      {
        "id": 1,
        "imageUrl": "https://cdn.example.com/image1.jpg",
        "displayOrder": 0
      }
    ],
    "comments": [
      {
        "id": 789,
        "memberNickname": "chef",
        "content": "방문 감사합니다!",
        "createdAt": "2025-12-10T16:00:00"
      }
    ],
    "createdAt": "2025-12-10T15:00:00",
    "updatedAt": "2025-12-10T15:00:00"
  }
}
```

---

### 5. 리뷰 도움됨 (도움이 돼요)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /api/reviews/{reviewId}/helpful` |
| **기능 요약** | 리뷰에 "도움이 돼요" 표시 |
| **Path Parameter** | `reviewId`: 리뷰 ID |
| **Response** | `ApiResponse<Void>` |

**비즈니스 로직:**
1. 리뷰 작성자의 `helpfulCount` 증가
2. 중복 도움됨 방지 (UNIQUE 제약)

---

### 6. 리뷰 도움됨 취소

| 항목 | 내용 |
|------|------|
| **Method + Path** | `DELETE /api/reviews/{reviewId}/helpful` |
| **기능 요약** | 도움됨 취소 |
| **Path Parameter** | `reviewId`: 리뷰 ID |
| **Response** | `ApiResponse<Void>` |

---

### 7. 리뷰 댓글 작성

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /api/reviews/{reviewId}/comments` |
| **기능 요약** | 리뷰에 댓글 작성 (업주 답글 등) |
| **Path Parameter** | `reviewId`: 리뷰 ID |
| **Request Body** | `CommentCreateRequest` |
| **Response** | `ApiResponse<CommentResponse>` |

**Request DTO:**
```json
{
  "content": "방문 감사합니다!"
}
```

---

### 8. 내 리뷰 목록 조회

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/members/me/reviews` |
| **기능 요약** | 로그인한 회원이 작성한 리뷰 목록 |
| **Query Parameters** | `page`, `size`, `status` (선택) |
| **Response** | `ApiResponse<Page<ReviewResponse>>` |

---

## Admin / Moderation

> 보안(Spring Security role 기반 인가)은 추후 강화 예정이며, 현재는 서비스 레벨에서 `Member.role==ADMIN`을 체크한다.

### 1. 리뷰 검수 목록 조회 (관리자)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /admin/reviews/pending` |
| **기능 요약** | 검수 대기 중인 리뷰 목록 조회 (PENDING 상태) |
| **Query Parameters** | `page`, `size` |
| **Response** | `ApiResponse<Page<ReviewModerationResponse>>` |
| **권한** | ADMIN |

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "검수 대기 리뷰 목록",
  "data": {
    "content": [
      {
        "id": 456,
        "storeName": "파스타하우스",
        "memberNickname": "foodlover",
        "memberTier": "BRONZE",
        "scoreCalculated": 5.0,
        "content": "정말 맛있어요!",
        "status": "PENDING",
        "createdAt": "2025-12-11T09:00:00"
      }
    ],
    "totalElements": 23
  }
}
```

---

### 2. 리뷰 승인 (관리자)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /admin/reviews/{reviewId}/approve` |
| **기능 요약** | 리뷰 검수 승인 (PENDING → APPROVED → PUBLIC 또는 BLIND_HELD) |
| **Path Parameter** | `reviewId`: 리뷰 ID |
| **Response** | `ApiResponse<Void>` |
| **권한** | ADMIN |

**비즈니스 로직:**
1. 리뷰 상태를 `APPROVED`로 변경
2. 가게의 `reviewCountValid` 체크
   - 5개 이상 → `PUBLIC`으로 전환, 평점 재계산, `member_store_visit` 누적 방문 횟수 증가 및 리뷰 `visitCount` 기록
   - 5개 미만 → `BLIND_HELD`로 전환 (평점 미반영)

---

### 3. 리뷰 반려 (관리자)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `POST /admin/reviews/{reviewId}/reject` |
| **기능 요약** | 리뷰 검수 반려 (PENDING → REJECTED) |
| **Path Parameter** | `reviewId`: 리뷰 ID |
| **Request Body** | `ReviewRejectRequest` |
| **Response** | `ApiResponse<Void>` |
| **권한** | ADMIN |

**Request DTO:**
```json
{
  "adminComment": "욕설이 포함되어 반려되었습니다."
}
```

---

### 4. 회원 등급 수동 조정 (관리자)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `PATCH /admin/members/{memberId}/tier` |
| **기능 요약** | 회원 등급 강제 변경 |
| **Path Parameter** | `memberId`: 회원 ID |
| **Request Body** | `AdminMemberTierUpdateRequest` |
| **Response** | `ApiResponse<MemberResponse>` |
| **권한** | ADMIN |

**Request DTO:**
```json
{
  "tier": "BLACK"
}
```

**비즈니스 로직:**
1. 회원 tier 강제 변경
2. tier가 실제로 변경되면, 과거 PUBLIC 리뷰가 반영된 스토어 점수를 소급 재계산하기 위해 `ReviewPolicyJobService.handleMemberTierChanged(memberId, oldTier, newTier)` 호출

---

### 4-1. 회원 권한(Role) 수동 조정 (관리자)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `PATCH /admin/members/{memberId}/role` |
| **기능 요약** | 회원 role(USER/ADMIN) 강제 변경 |
| **Path Parameter** | `memberId`: 회원 ID |
| **Request Body** | `AdminMemberRoleUpdateRequest` |
| **Response** | `ApiResponse<MemberResponse>` |
| **권한** | ADMIN |

**Request DTO:**
```json
{
  "role": "ADMIN"
}
```

**주의:** 자기 자신의 role 변경은 금지됩니다.

---

### 5. 통계 대시보드 (관리자)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /admin/dashboard` |
| **기능 요약** | 운영 통계 조회 (회원 수, 리뷰 수, 검수 대기 수 등) |
| **Response** | `ApiResponse<AdminDashboardResponse>` |
| **권한** | ADMIN |

**Response DTO:**
```json
{
  "code": "SUCCESS",
  "message": "통계 조회 성공",
  "data": {
    "totalMembers": 1234,
    "totalStores": 567,
    "totalReviews": 8901,
    "pendingReviews": 23,
    "blackMembers": 5,
    "todayNewMembers": 12,
    "todayNewReviews": 45
  }
}
```

---

### 6. 편차 보정 대상 회원 조회 (관리자)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /admin/members/deviation-targets` |
| **기능 요약** | 편차 보정이 적용된 회원 목록 조회 |
| **Query Parameters** | `page`, `size` |
| **Response** | `ApiResponse<Page<MemberResponse>>` |
| **권한** | ADMIN |

---

## 추가 엔드포인트 (선택 구현)

### 카테고리 / 지역 조회

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/categories` |
| **기능 요약** | 카테고리 트리 구조 조회 (한식 > 찌개/탕 > 김치찌개) |
| **Response** | `ApiResponse<List<CategoryResponse>>` |

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/regions` |
| **기능 요약** | 지역 트리 구조 조회 (서울 > 강남구 > 역삼동) |
| **Response** | `ApiResponse<List<RegionResponse>>` |

---

### 게시판 (공지사항, FAQ 등)

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/boards` |
| **기능 요약** | 게시판 목록 조회 (type: NOTICE, FAQ, REVIEW_GUIDE, EVENT) |
| **Query Parameters** | `type`, `page`, `size` |
| **Response** | `ApiResponse<Page<BoardResponse>>` |

| 항목 | 내용 |
|------|------|
| **Method + Path** | `GET /api/boards/{boardId}` |
| **기능 요약** | 게시글 상세 조회 |
| **Response** | `ApiResponse<BoardDetailResponse>` |

---

# 다음 단계 (구현 가이드)

## 1단계: 핵심 도메인 구현 (우선순위 높음)

1. **Member 도메인**
   - 회원가입/로그인 API
   - 등급 자동 승급 로직
   - Spring Security 설정

2. **Store 도메인**
   - 가게 등록/조회 API
   - 평점 계산 서비스 (`StoreScoreService`)
   - 베이지안 평균 알고리즘 구현

3. **Review 도메인**
   - 리뷰 작성/수정/삭제 API
   - 다차원 평점 계산 로직
   - 상태 전이 처리 (PENDING → APPROVED → PUBLIC)

## 2단계: 정책 적용 (우선순위 중간)

4. **시간 감가상각 배치**
   - 매일 자정 배치 작업 (Spring Batch 또는 @Scheduled)
   - 6개월 이상 리뷰의 가중치 재계산

5. **편차 보정 배치**
   - 매일 새벽 2시 배치 작업
   - 극단적 평점 패턴 유저 식별 및 `isDeviationTarget` 업데이트

6. **블라인드 정책**
   - 리뷰 5개 미만 가게의 평점 비공개 처리
   - 프론트엔드에 "평가 중" 메시지 표시

## 3단계: 운영 기능 (우선순위 낮음)

7. **관리자 검수 시스템**
   - 관리자 대시보드 API
   - 리뷰 승인/반려 워크플로우

8. **어뷰징 탐지**
   - 속도 위반 (1시간 5개 이상) 자동 탐지
   - 템플릿 리뷰 필터링
   - 다중 계정 탐지 (IP, Device UUID)

9. **부가 기능**
   - 스크랩, 팔로우, 댓글 기능
   - 게시판 (공지사항, FAQ)

---

# 부록: DTO 네이밍 컨벤션

| 용도 | 네이밍 패턴 | 예시 |
|-----|-----------|------|
| **요청 DTO (생성)** | `{Entity}CreateRequest` | `ReviewCreateRequest` |
| **요청 DTO (수정)** | `{Entity}UpdateRequest` | `MemberProfileUpdateRequest` |
| **응답 DTO (단순)** | `{Entity}Response` | `StoreResponse` |
| **응답 DTO (상세)** | `{Entity}DetailResponse` | `StoreDetailResponse` |
| **검색 조건 DTO** | `{Entity}SearchCondition` | `StoreSearchCondition` |

---

**문서 끝**
