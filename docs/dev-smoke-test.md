# Dev Boot & `/api/stores` Smoke Test Guide

이 문서는 개발(dev) 환경에서 앱 부팅과 `/api/stores` 관련 엔드포인트를 빠르게 확인하기 위한 스모크 테스트 절차입니다.

**문서 최신화:** 2025-12-31  
**정책 기준:** review-policy v1.3.3

---

## 0. 사전 준비

- Java 21
- Docker / Docker Compose

---

## 1. Postgres 기동

프로젝트 루트에서:

```bash
docker compose up -d postgres
docker compose ps
```

---

## 2. dev 프로파일로 앱 부팅

### Windows PowerShell

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

---

## 3. 인증(Basic Auth)

dev 스모크 테스트용 기본 계정(인메모리):

- Username: `admin`
- Password: `admin123`

---

## 4. Store API 스모크 테스트

PowerShell에서는 `curl`이 별칭이라 옵션이 깨질 수 있어 **`curl.exe` 사용을 권장**합니다.

### 4.1 등록 POST `/api/stores`

```powershell
@'
{"name":"파스타하우스","categoryId":1,"regionId":1,"address":"서울 ...","latitude":37.1,"longitude":127.1}
'@ | Set-Content -Encoding utf8 -NoNewline store.json

curl.exe -v -u admin:admin123 `
  -H "Content-Type: application/json" `
  --data-binary "@store.json" `
  "http://localhost:8080/api/stores"
```

### 4.2 상세 GET `/api/stores/{id}`

```powershell
curl.exe -v -u admin:admin123 "http://localhost:8080/api/stores/<id>"
```

체크 포인트:
- `isBlind=true`인 경우
  - `scoreWeighted`는 `null`
  - `blindMessage`가 내려옴
  - `reviewCountValid`는 "수집된 리뷰 수(APPROVED/BLIND_HELD/PUBLIC 합계)"로 내려옴
  - `recentReviews`는 내려오되, **점수 필드(`score*`, `scoreCalculated`)는 `null`** 로 내려옵니다(텍스트/이미지 등만 노출)
- `isBlind=false`인 경우
  - `scoreWeighted`, `avgRating`가 내려옴
  - `recentReviews` 최대 3개가 내려옴(이미지 포함)

### 4.3 검색 GET `/api/stores/search`

```powershell
$kw = [uri]::EscapeDataString("파스타")
curl.exe -v -u admin:admin123 `
  "http://localhost:8080/api/stores/search?keyword=$kw&page=0&size=20"
```

체크 포인트:
- 목록 응답(`StoreResponse`)에서 `isBlind=true`인 가게는 `scoreWeighted=null`로 내려옴

### 4.4 가게 리뷰 목록 GET `/api/stores/{storeId}/reviews`

```powershell
curl.exe -v -u admin:admin123 "http://localhost:8080/api/stores/<storeId>/reviews?page=0&size=20"
```

체크 포인트:
- **블라인드 가게(`isBlind=true`)**: 리뷰의 `content`(텍스트)는 내려오지만, `score*` 및 `scoreCalculated`는 `null`로 내려옵니다.
- **블라인드 해제 가게(`isBlind=false`)**: 점수 필드들도 정상 노출됩니다.

---

## 5. Review API 스모크 테스트(추천)

### 5.1 리뷰 작성 POST `/api/reviews`

```powershell
@'
{
  "storeId": 1,
  "title": "좋았어요",
  "content": "정말 맛있었습니다",
  "partySize": 2,
  "scoreTaste": 4.5,
  "scoreValue": 4.0,
  "scoreAmbiance": 4.0,
  "scoreService": 4.5,
  "visitDate": "2025-12-30",
  "images": []
}
'@ | Set-Content -Encoding utf8 -NoNewline review.json

curl.exe -v -u admin:admin123 `
  -H "Content-Type: application/json" `
  --data-binary "@review.json" `
  "http://localhost:8080/api/reviews"
```

### 5.2 리뷰 도움이 됨 토글

```powershell
curl.exe -v -u admin:admin123 "http://localhost:8080/api/reviews/<reviewId>/helpful"
curl.exe -v -u admin:admin123 -X DELETE "http://localhost:8080/api/reviews/<reviewId>/helpful"
```

체크 포인트:
- 리뷰 상세(`GET /api/reviews/{reviewId}`) 또는 가게 리뷰 목록(`GET /api/stores/{storeId}/reviews`가 있다면)에서
  `isHelpfulByMe` 필드가 내려오며,
  현재 로그인한 사용자가 도움이 됨을 눌렀는지 여부를 나타냅니다.
  - 로그인 상태: `true/false`
  - 비로그인 상태: `null`

---

## 6. Admin(검수) 스모크 테스트(추천)

> 보안/인가 정책은 별도 작업 범위지만, 기능 플로우 확인 용도로 엔드포인트 호출은 가능해야 합니다.

- pending 목록 조회: `GET /admin/reviews/pending`
- 승인: `POST /admin/reviews/{reviewId}/approve`
- 반려: `POST /admin/reviews/{reviewId}/reject`
- 회원 tier 변경: `PATCH /admin/members/{memberId}/tier`

승인 시 체크 포인트:
- store별 (APPROVED/BLIND_HELD/PUBLIC) 합계가 5 이상이 되는 순간 자동으로 PUBLIC 전환
- PUBLIC 전환 시 방문횟수(`visitCount`)가 증가하여 리뷰 응답에 반영

---

## 7. Category / Region 데이터 확인

```powershell
curl.exe -u admin:admin123 "http://localhost:8080/api/categories"
curl.exe -u admin:admin123 "http://localhost:8080/api/regions"
```
