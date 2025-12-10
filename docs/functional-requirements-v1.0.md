# 기능 명세서 (Functional Requirements) v1.0

**근거 문서:** 리뷰 정책 사양서 v1.3.2
**작성일:** 2025-12-10
**대상:** 백엔드 개발팀, DB 설계자

---

## 1. 개요 (Overview)
본 문서는 '한국형 미식 검증 플랫폼'의 리뷰 시스템 구현을 위한 데이터 요구사항, 상태 전이 로직, 핵심 알고리즘 처리 방식을 정의한다.

---

## 2. 데이터 요구사항 (Data Requirements)

### 2.1 사용자 (MEMBER)
등급 시스템 및 어뷰징 방지 로직을 처리하기 위해 다음 데이터가 관리되어야 한다.

| 필드명 | 타입 | 필수여부 | 설명 및 제약조건 |
|:---|:---|:---|:---|
| **tier** | ENUM | Y | 유저 등급 (`BRONZE`, `SILVER`, `GOLD`, `GOURMET`, `BLACK`).<br>기본값: `BRONZE`. 등급 변경 시 관련 리뷰 가중치 소급 적용 트리거 필요. |
| **helpful_count** | INT | N | 누적 '도움됨' 수신 횟수. `GOURMET` 승급 심사 기준(500회). |
| **review_count** | INT | N | 누적 리뷰 작성 수. `GOURMET` 승급 심사 기준(100회). |
| **violation_count** | INT | N | 경고/신고 누적 횟수. 일정 횟수 초과 시 `BLACK` 등급 자동 전환. |
| **last_review_at** | DATETIME | N | 마지막 리뷰 작성 일시. `GOLD` 이상 등급의 활동성 체크(1년 미작성 강등)에 사용. |
| **is_deviation_target** | BOOLEAN | Y | **[편차 보정]** 대상 여부. `TRUE`일 경우 해당 유저의 평점 반영 시 강제로 **-0.5점** 패널티 적용. |

### 2.2 가게 (STORE)
블라인드 정책 및 베이지안 평점 알고리즘 결과를 저장한다.

| 필드명 | 타입 | 필수여부 | 설명 및 제약조건 |
|:---|:---|:---|:---|
| **score_weighted** | DECIMAL(3,2) | Y | **[최종 평점]** 정책 v1.3.2 알고리즘에 의해 계산된 결과값.<br>조회 성능을 위해 실시간 계산이 아닌 저장된 값을 사용(캐싱). |
| **review_count_valid** | INT | Y | 상태가 `PUBLIC`인 유효 리뷰의 개수. |
| **is_blind** | BOOLEAN | Y | **[블라인드]** `review_count_valid < 5` 일 경우 `TRUE`.<br>`TRUE` 상태에서는 프론트엔드에 평점을 노출하지 않음 ("평가 중"). |

### 2.3 리뷰 (REVIEW)
시간 감가상각 불변성 및 운영 프로세스를 지원한다.

| 필드명 | 타입 | 필수여부 | 설명 및 제약조건 |
|:---|:---|:---|:---|
| **status** | ENUM | Y | 리뷰의 현재 상태. (상세 흐름은 3. 상태 전이 로직 참조) |
| **score_calculated** | DECIMAL(3,2) | Y | **[종합 점수]** 사용자가 입력한 다차원 평점(맛/가성비/분위기/접객)의 가중 합으로 자동 계산된 값. |
| **created_at** | DATETIME | Y | **[불변]** 시간 감가상각의 기준점.<br>**주의:** 리뷰 수정(Update) 시에도 이 값은 절대 변경되지 않아야 함. |
| **updated_at** | DATETIME | N | 리뷰 내용 수정 시 갱신. (단순 표시용) |
| **admin_comment** | TEXT | N | 운영자 반려(`REJECTED`) 시 사유 기록용. |

---

## 3. 상태 전이 및 프로세스 로직 (Process Logic)

### 3.1 리뷰 생명주기 (Review Lifecycle)
`REVIEW.status` 필드는 다음 흐름에 따라 변경된다.

1.  **작성 직후 (`PENDING`)**
    * **쿨다운 체크:** 평점이 1.0 또는 5.0일 경우 → 12시간 동안 노출 보류.
    * **초기 운영 모드:** 서비스 론칭 초기에는 모든 리뷰가 운영자 승인 대기.
2.  **검수 단계**
    * **승인 (`APPROVED`):** 쿨다운 종료 또는 운영자 승인 시.
    * **반려 (`REJECTED`):** 금지어 포함, 어뷰징 탐지, 운영자 반려 시. (노출 불가, 점수 미반영)
3.  **공개 단계**
    * **블라인드 대기 (`BLIND_HELD`):** 승인되었으나 가게의 리뷰 수가 5개 미만인 경우. (내용은 저장되나 점수/리스트 미노출)
    * **공개 (`PUBLIC`):** 가게 리뷰 수가 5개 이상 충족 시 자동 전환. (점수 반영, 리스트 노출)
4.  **제재 단계**
    * **정지 (`SUSPENDED`):** 작성자가 `BLACK` 등급이 된 경우. (리뷰는 남겨두되 점수 계산에서 제외 - 가중치 0 처리)

### 3.2 시간 감가상각 불변성 (Time Decay Immutability)
* **규칙:** 사용자가 과거에 작성한 리뷰를 수정하더라도, 점수의 영향력(가중치)이 최신으로 초기화되어서는 안 된다.
* **구현:** `Time Decay Factor` 계산 시 반드시 `NOW()`와 **`created_at`**의 차이를 이용한다. (`updated_at` 사용 금지)

---

## 4. 핵심 비즈니스 로직 (Core Business Logic)

### 4.1 최종 점수 계산 (Scoring Algorithm)
가게의 `score_weighted`는 다음 트리거 발생 시 비동기(Async) 또는 배치로 재계산한다.
* **공식:** `(R × v + 3.0 × 30) / (v + 30)` (베이지안 평균)
    * `R` (가중 평균): `Σ(리뷰점수 × 유저가중치 × 시간가중치) / Σ(유저가중치 × 시간가중치)`
    * `v` (유효 가중치 총합): `Σ(유저가중치 × 시간가중치)`
* **예외 처리:** `v = 0` 일 경우 `R = 3.0` (Division by Zero 방지).

### 4.2 편차 보정 (Deviation Adjustment)
* **방식:** 실시간 계산 부하를 줄이기 위해 **일일 배치(Daily Batch)**로 처리한다.
* **로직:**
    1.  최근 리뷰 20개 중 1점/5점 비율이 90% 이상인 유저 식별.
    2.  해당 유저의 `MEMBER.is_deviation_target` = `TRUE` 업데이트.
    3.  이후 평점 계산 시 해당 유저의 점수는 중앙값(3.0) 방향으로 **0.5점 보정**하여 반영.

### 4.3 등급 변경에 따른 소급 적용 (Retroactive Weight Application)
* **트리거:** `MEMBER.tier` 변경 시 (예: GOLD → GOURMET).
* **액션:** 해당 유저가 작성한 모든 `PUBLIC` 상태 리뷰를 조회하여, 관련 가게들의 `score_weighted`를 전면 재계산한다.

---

## 5. 시스템 트리거 및 스케줄링 (Triggers & Schedules)

| 구분 | 주기/시점 | 작업 내용 |
|:---|:---|:---|
| **Real-time** | 리뷰 작성/수정 시 | `score_calculated` 자동 계산, 금지어 체크, `status` 결정 (`PENDING` 등). |
| **Batch** | 매일 00:00 | **시간 감가상각 갱신:** 모든 리뷰의 시간 가중치가 변하므로 가게 평점 재계산 필요 가능성 있음 (최적화 필요). |
| **Batch** | 매일 02:00 | **편차 보정 분석:** 유저들의 최근 리뷰 패턴 분석 및 `is_deviation_target` 갱신. |
| **Batch** | 매일 04:00 | **등급 승급 심사:** `GOLD` 등급 요건 충족 유저 자동 승급 처리. |
| **Event** | 등급 변경 시 | **평점 재계산:** 해당 유저가 리뷰를 남긴 모든 가게의 평점 갱신 (비동기 큐 처리 권장). |

---

## 6. 구현 가이드라인 (Implementation Guidelines)

### 6.1 다차원 평점 계산
리뷰 작성 시 사용자가 입력하는 4가지 항목을 자동으로 가중 합산하여 `score_calculated`를 산출한다.

**가중치:**
- 맛 (Taste): 40%
- 가성비 (Value): 30%
- 분위기 (Ambiance): 15%
- 접객 (Service): 15%

**공식:**
```
score_calculated = (taste × 0.4) + (value × 0.3) + (ambiance × 0.15) + (service × 0.15)
```

### 6.2 시간 감가상각 계수 (Time Decay Factor)

| 리뷰 작성 시점 | 가중치 계수 |
|--------------|-----------|
| 최근 6개월 이내 | 1.0 (100%) |
| 6개월 ~ 1년 | 0.8 (80%) |
| 1년 ~ 2년 | 0.5 (50%) |
| 2년 ~ 3년 | 0.2 (20%) |
| 3년 이상 | 0.1 (10%, 기록 보존용) |

### 6.3 유저 등급별 가중치

| 등급 | 가중치 | 설명 |
|-----|-------|------|
| **BRONZE** | 0.5 | 기본 등급 |
| **SILVER** | 1.0 | 검증된 리뷰어 |
| **GOLD** | 1.5 | 활동적인 고급 리뷰어 |
| **GOURMET** | 2.0 | 최상위 전문 리뷰어 |
| **BLACK** | 0.0 | 제재 대상 (점수 반영 제외) |

### 6.4 리뷰 상태 (Status) 정의

| 상태 | 설명 | 점수 반영 여부 |
|-----|------|-------------|
| **PENDING** | 작성 직후 대기 (쿨다운 또는 검수 대기) | ✗ |
| **APPROVED** | 운영자 승인 완료 | ✗ (BLIND_HELD 또는 PUBLIC 전환 대기) |
| **REJECTED** | 운영자 반려 | ✗ |
| **BLIND_HELD** | 승인되었으나 가게 리뷰 수 부족 (5개 미만) | ✗ |
| **PUBLIC** | 공개 및 점수 반영 중 | ✓ |
| **SUSPENDED** | 작성자 BLACK 등급으로 인한 정지 | ✗ |

### 6.5 블라인드 정책 (Blind Initial Phase)

**조건:**
- 가게의 `review_count_valid < 5`

**처리:**
- `STORE.is_blind = TRUE`
- 프론트엔드에서 평점 대신 "평가 중 (Under Review)" 표시
- 리뷰 5개 이상 수집 시 자동으로 `is_blind = FALSE` 전환 및 평점 공개

### 6.6 쿨다운 시스템 (Cooldown)

**적용 대상:**
- Bronze, Silver 등급 유저
- 1점 또는 5점 리뷰 작성 시

**처리:**
- 리뷰를 `PENDING` 상태로 12시간 유지
- 12시간 후 자동으로 `APPROVED` 전환 (운영자 검수 없이)
- Gold 이상 등급은 쿨다운 면제

---

## 7. 배치 작업 상세 (Batch Job Details)

### 7.1 시간 감가상각 갱신 (00:00)
**목적:** 모든 리뷰의 시간 가중치 업데이트

**처리 방식:**
1. 모든 `PUBLIC` 상태 리뷰의 `created_at` 재평가
2. 시간 가중치가 변경된 리뷰가 포함된 가게 목록 추출
3. 해당 가게들의 `score_weighted` 재계산 (비동기 큐)

**최적화:**
- 6개월 이내 리뷰는 가중치 1.0으로 고정되므로 재계산 불필요
- 6개월~3년 사이 리뷰만 대상으로 처리

### 7.2 편차 보정 분석 (02:00)
**목적:** 극단적 평점 패턴 유저 식별

**로직:**
```sql
-- 최근 20개 리뷰 중 1점 또는 5점 비율 계산
SELECT member_id
FROM (
    SELECT member_id,
           COUNT(*) as total_reviews,
           SUM(CASE WHEN score_calculated IN (1.0, 5.0) THEN 1 ELSE 0 END) as extreme_count
    FROM review
    WHERE status = 'PUBLIC'
    GROUP BY member_id
    HAVING total_reviews >= 20
) sub
WHERE (extreme_count / total_reviews) >= 0.9
```

**액션:**
- 해당 유저의 `is_deviation_target = TRUE` 업데이트
- 관련 가게 평점 재계산 트리거

### 7.3 등급 승급 심사 (04:00)
**목적:** 자동 승급 조건 충족 유저 처리

**처리 대상:**
1. **Bronze → Silver**
   - 100자 이상 리뷰 5개
   - 운영자 검수 통과 리뷰 3개 이상

2. **Silver → Gold**
   - 누적 리뷰 30개
   - '도움됨' 100회 이상

3. **Gold 강등 체크**
   - `last_review_at`이 1년 이상 경과한 경우 Silver로 강등

4. **Gourmet 강등 체크**
   - 6개월 내 리뷰 10개 미만 시 Gold로 강등

**소급 처리:**
- 등급 변경 시 해당 유저의 모든 `PUBLIC` 리뷰가 포함된 가게 평점 재계산

---

## 8. API 요구사항 (API Requirements)

### 8.1 리뷰 작성 API
**Endpoint:** `POST /api/reviews`

**Request Body:**
```json
{
  "store_id": "string",
  "taste": 4.5,
  "value": 4.0,
  "ambiance": 4.0,
  "service": 4.5,
  "content": "string",
  "photos": ["url1", "url2"]
}
```

**자동 처리:**
1. `score_calculated` 계산 (다차원 평점 가중 합)
2. 쿨다운 체크 (1점 또는 5점 시)
3. `status = PENDING` 설정
4. 금지어 필터링

### 8.2 가게 상세 조회 API
**Endpoint:** `GET /api/stores/{store_id}`

**Response:**
```json
{
  "store_id": "string",
  "name": "string",
  "score_weighted": 4.2,
  "is_blind": false,
  "review_count_valid": 150,
  "reviews": [
    {
      "review_id": "string",
      "member_tier": "GOLD",
      "score_calculated": 4.5,
      "taste": 4.5,
      "value": 4.0,
      "ambiance": 4.0,
      "service": 4.5,
      "content": "string",
      "created_at": "2025-12-10T10:00:00Z"
    }
  ]
}
```

**조건부 처리:**
- `is_blind = true` 일 경우 `score_weighted` 대신 "평가 중" 메시지 반환
- 리뷰 목록은 `status = PUBLIC`인 것만 반환

---

## 9. 예외 처리 및 엣지 케이스 (Exception Handling)

### 9.1 Division by Zero
**상황:** 가게에 유효한 리뷰가 없을 때 (`v = 0`)

**처리:** `R = 3.0` (플랫폼 평균)으로 설정

### 9.2 편차 보정 적용
**방향:**
- 1점 리뷰 → 1.5점으로 보정 (중앙값 방향 +0.5)
- 5점 리뷰 → 4.5점으로 보정 (중앙값 방향 -0.5)

### 9.3 등급 변경 중 리뷰 작성
**시나리오:** 유저가 Bronze일 때 리뷰 작성 후, 같은 날 Silver로 승급

**처리:**
- 배치 작업에서 소급 적용
- 해당 리뷰의 가중치가 0.5 → 1.0으로 변경
- 관련 가게 평점 재계산

### 9.4 BLACK 등급 전환
**시나리오:** Gold 유저가 어뷰징으로 BLACK 전환

**처리:**
1. `MEMBER.tier = BLACK` 업데이트
2. 해당 유저의 모든 `PUBLIC` 리뷰를 `SUSPENDED`로 변경
3. 관련 가게들의 평점 재계산 (해당 유저 리뷰 가중치 0으로 처리)

---

## 10. 성능 최적화 고려사항 (Performance Considerations)

### 10.1 평점 계산 캐싱
- `STORE.score_weighted`를 DB에 저장하여 실시간 계산 부하 방지
- 트리거 발생 시에만 재계산

### 10.2 배치 작업 분산
- 대량 평점 재계산은 비동기 큐(예: RabbitMQ, Kafka) 활용
- 가게별 독립 작업으로 병렬 처리 가능

### 10.3 인덱스 전략
- `REVIEW.status`, `REVIEW.created_at` 복합 인덱스
- `MEMBER.tier`, `MEMBER.is_deviation_target` 인덱스
- `STORE.is_blind` 인덱스

### 10.4 시간 감가상각 최적화
- 6개월 이내 리뷰는 가중치 변경 없음 → 재계산 스킵
- 3년 이상 오래된 리뷰는 가중치 0.1 고정 → 재계산 빈도 낮춤

---

## 11. 보안 및 컴플라이언스 (Security & Compliance)

### 11.1 개인정보 보호
- 리뷰 작성자 실명 비공개 (닉네임만 표시)
- 업주는 작성자 연락처 조회 불가
- 법원 명령 시에만 정보 제공

### 11.2 어뷰징 탐지
- IP, Device UUID 기반 다중 계정 탐지
- 템플릿 리뷰 자동 필터링
- 속도 제한 (Rate Limiting)

### 11.3 데이터 무결성
- `score_calculated`는 자동 계산만 허용 (수동 입력 금지)
- `created_at`은 INSERT 시에만 설정 (UPDATE 시 변경 금지)
- 등급 변경 시 트랜잭션 처리로 일관성 보장

---

## 12. 테스트 시나리오 (Test Scenarios)

### 12.1 단위 테스트
- [ ] 다차원 평점 계산 정확성
- [ ] 시간 감가상각 계수 산출
- [ ] 베이지안 평균 공식 검증
- [ ] 편차 보정 로직

### 12.2 통합 테스트
- [ ] 리뷰 작성 → 평점 반영 플로우
- [ ] 등급 변경 → 소급 적용
- [ ] 블라인드 정책 (5개 미만 → 5개 이상)
- [ ] 쿨다운 시스템 (12시간 대기)

### 12.3 부하 테스트
- [ ] 1000명 동시 리뷰 작성
- [ ] 배치 작업 시 DB 부하 측정
- [ ] 평점 재계산 큐 처리 속도

### 12.4 엣지 케이스
- [ ] 리뷰 0개 가게 조회
- [ ] BLACK 등급 전환 시 평점 변화
- [ ] 편차 보정 대상이 5점 리뷰 작성 시

---

## 13. 변경 이력 (Change Log)

| 버전 | 날짜 | 변경 내용 | 작성자 |
|-----|------|----------|-------|
| 1.0 | 2025-12-10 | 최초 작성 (리뷰 정책 v1.3.2 기반) | System |

---

**문서 끝**
