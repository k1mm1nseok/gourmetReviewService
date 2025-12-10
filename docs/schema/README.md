# ERD 설계 문서

## 📁 파일 목록

### 1. `gourmet-review-service-ddl.sql` ⭐ **ERDCloud용**
- **용도**: ERDCloud에 import하여 자동으로 ERD 생성
- **형식**: MySQL DDL Script

### 2. `gourmet-review-service.vuerd.json`
- **용도**: vuerd (VSCode Extension) 또는 웹 뷰어
- **형식**: vuerd JSON

### 3. `ERD-SCHEMA.md`
- **용도**: 상세 스키마 문서 (비즈니스 규칙 포함)
- **형식**: Markdown

---

## 🎯 ERDCloud 사용 방법 (추천)

### Step 1: ERDCloud 접속
https://www.erdcloud.com/ 접속

### Step 2: 새 프로젝트 생성
1. "새 다이어그램" 클릭
2. 프로젝트 이름: `gourmet-review-service` 입력

### Step 3: DDL Import
1. 상단 메뉴에서 **"Import"** 클릭
2. **"DDL"** 선택
3. `gourmet-review-service-ddl.sql` 파일 내용 복사 & 붙여넣기
4. **"Import"** 버튼 클릭

### Step 4: 자동 ERD 생성 완료! 🎉
- 12개 테이블이 자동으로 배치됨
- 관계선(FK)도 자동으로 그려짐
- 테이블 위치는 드래그로 조정 가능

---

## 🛠️ vuerd 사용 방법 (개발자용)

### VSCode Extension 설치
```bash
# VSCode Extensions 검색
vuerd
```

### 사용법
1. VSCode에서 `gourmet-review-service.vuerd.json` 파일 열기
2. 우클릭 → "Open with vuerd" 선택
3. 시각적 ERD 확인 및 편집 가능

---

## 📊 스키마 구조 요약

### 핵심 엔티티 (3개)
- **MEMBER**: 회원 (tier 기반 등급제)
- **STORE**: 가게 (블라인드 처리 로직)
- **REVIEW**: 리뷰 (status 기반 워크플로우)

### 계층 구조 (2개)
- **CATEGORY**: 카테고리 (한식 > 찌개/탕 > 김치찌개)
- **REGION**: 지역 (서울 > 강남구 > 역삼동)

### 관계 엔티티 (7개)
- STORE_AWARD (가게 수상 이력)
- REVIEW_IMAGE (리뷰 이미지)
- REVIEW_LIKE (리뷰 좋아요)
- COMMENT (댓글)
- BOARD (게시글)
- STORE_SCRAP (가게 스크랩)
- MEMBER_FOLLOW (회원 팔로우)

---

## 🔑 주요 비즈니스 규칙

### 1. 회원 등급 (tier)
```
BRONZE (기본) → SILVER (리뷰 10개, 도움됨 30개)
               → GOLD (리뷰 30개, 도움됨 100개)
               → GOURMET (리뷰 100개, 도움됨 500개)
               → BLACK (관리자 지정)
```

### 2. 리뷰 상태 흐름
```
PENDING → APPROVED → PUBLIC
            ↓
        REJECTED
            ↓
        BLIND_HELD (가게 리뷰 5개 미만)
```

### 3. 가게 블라인드 처리
- `review_count_valid < 5` → `is_blind = true`
- 블라인드 상태에서는 평점/리뷰 수 미공개

### 4. 시간 감가상각
- `review.created_at` 기준으로 시간 경과에 따라 가중치 감소
- `review.updated_at`은 편집 이력 추적용 (감가상각 영향 없음)

---

## 🗂️ 테이블 관계 요약

```
MEMBER (1) ----< (N) REVIEW
MEMBER (1) ----< (N) REVIEW_LIKE
MEMBER (1) ----< (N) STORE_SCRAP
MEMBER (1) ----< (N) MEMBER_FOLLOW

STORE (1) ----< (N) REVIEW
STORE (N) >---- (1) CATEGORY
STORE (N) >---- (1) REGION

REVIEW (1) ----< (N) REVIEW_IMAGE
REVIEW (1) ----< (N) REVIEW_LIKE
REVIEW (1) ----< (N) COMMENT

CATEGORY (자기참조: parent_id)
REGION (자기참조: parent_id)
```

---

## 📝 다음 단계

1. ✅ **ERDCloud에 DDL Import** (현재 문서 사용)
2. ⬜ **실제 DB에 DDL 실행** (MySQL/MariaDB)
3. ⬜ **초기 데이터 세팅** (Category, Region 마스터 데이터)
4. ⬜ **Repository Layer 구현**
5. ⬜ **Service Layer 구현**

---

## 📞 문의

스키마 관련 질문이나 수정 요청은 이슈로 등록해주세요.
