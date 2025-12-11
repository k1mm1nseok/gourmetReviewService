# Agent Guide – Gourmet Review Service

이 문서는 이 프로젝트에서 **사람과 AI 도구(LLM)**가 일관성 있게 작업하기 위한 공통 규칙을 정의합니다.  
모든 작업(설계, 구현, 테스트, 문서화)은 이 문서를 기본 전제로 합니다.

---

## 1. 프로젝트 개요

- 프로젝트 이름: **Gourmet Review Service** (타베로그 스타일 음식 리뷰/평점 서비스)
- 목적:
  - 초기 평점이 인위적으로 높게 형성되지 않도록 **편차를 제어한 리뷰 시스템**을 구축한다.
  - 신뢰도 높은 리뷰와 평점을 제공하여, 사용자가 실제 방문 전 의사결정에 참고할 수 있도록 한다.
- 주요 기능(초기 버전 기준):
  - 회원가입/로그인, 권한/등급(티어) 관리
  - 음식점/매장 정보 조회
  - 리뷰 작성/수정/삭제 (텍스트 및 사진)
  - 평점 산정 및 노출 로직 (초기 리뷰 편차 완화)
  - 신고/위반 관리, 운영자 검수

---

## 2. 도메인 / 비즈니스 규칙

### 2.1 리뷰/평점 정책

- GPS 기능: **도입하지 않음**
- 영수증 인증: **도입하지 않음**
- 초기에는 **운영자 검수 방식으로 리뷰 품질 관리**를 수행한다.
- 기본 노출 평점은 가능한 한 **3.0 근처에서 시작**되도록 조정한다.
  - 예: 첫 10개 리뷰가 모두 5점이어도, 요약 평점이 4점대 이상으로 과도하게 치솟지 않도록 보정한다.
- 추후 고려:
  - 리뷰어 신뢰도(도움됨 수, 신고/위반 이력 등)를 반영한 가중치 모델 도입.
  - 리뷰 텍스트 길이·품질에 따른 가중치 또는 필터링.

### 2.2 회원/티어 정책 (예시)

- 기본 티어: `BRONZE`, 그 외 `SILVER`, `GOLD`, `GOURMET`, `BLACK` 등 단계 사용.
- 티어 산정 기준(예시, 실제 기준은 추후 확정):
  - 누적 리뷰 수
  - 누적 도움됨 수
  - 위반/신고 이력
  - 최근 활동(마지막 리뷰 작성 시점 등)

※ 위 기준은 **DB 스키마 및 로직 설계 시 반영**해야 하며, 변경 시 반드시 이 문서를 갱신한다.

---

## 3. 기술 스택 / 버전 정책

### 3.1 백엔드

- 언어: **Java 21 (LTS)**
- 프레임워크: **Spring Boot 3.x**
- 주요 라이브러리:
  - Spring Web / Spring MVC
  - Spring Data JPA
  - Spring Security (로그인 및 권한 관리)
  - Validation: `jakarta.validation` 기반

- Lombok:
  - Lombok **적극 사용 권장**  
    - 예: `@Getter`, `@Setter`(필요 시), `@RequiredArgsConstructor`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` 등.
  - **Entity 클래스에는 `@Data` 사용 금지**  
    - 대신 `@Getter` + 명시적 생성자/도메인 메서드 사용.
  - DTO/요청/응답 모델에는 `@Builder`를 활용하되, **필수 필드 누락**에 주의한다.

### 3.2 데이터베이스

- 메인 DB: **PostgreSQL**
- 버전: 14 이상 권장
- 개발 환경:
  - Docker 컨테이너로 로컬 개발용 PostgreSQL 구동
  - 로컬/테스트/운영 환경별로 **별도 DB 및 스키마** 사용

### 3.3 기타

- 빌드/관리: Maven
- 버전 관리: Git (GitHub 또는 기타 호스팅)
- 인프라: 초기엔 로컬/단일 서버 기준, 추후 컨테이너 기반 배포 고려

---

## 4. 아키텍처 원칙

- 레이어드 아키텍처 기본 원칙:
  - **Controller**: HTTP 요청/응답 처리, DTO 변환, API 응답 포맷 통일
  - **Service**: 비즈니스 로직, 트랜잭션 경계
  - **Repository**: DB 접근(JPA)
  - **Domain/Entity**: 핵심 도메인 모델
- 의존 방향:
  - Controller → Service → Repository → DB
  - 상위 레이어는 하위 레이어의 구현 세부사항에 가능한 한 덜 의존하도록 설계한다.
- 모듈/패키지 구성(예시):
  - `member`, `restaurant`, `review`, `auth`, `admin` 등 도메인 중심 패키지 구조 사용.

### 4.1 패키지/디렉토리 구조 예시

```plaintext
src/main/java/com/gourmet/review
├── common          # 공통 유틸, 에러 핸들링, 응답 래퍼(ApiResponse 등)
├── config          # 설정 파일 (Security, Swagger/OpenAPI 등)
├── domain          # 도메인별 패키지 (핵심)
│   ├── member      # 회원 도메인
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── restaurant  # 식당 도메인
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   └── review      # 리뷰 도메인
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       └── dto
└── infrastructure  # 외부 연동 구현체 (메일, 외부 API, 메시지 큐 등 필요 시)
````

* 신규 기능/도메인은 가능한 한 `domain` 하위에 **도메인 단위로 추가**한다.
* Controller/Service/Repository/Entity/DTO는 **도메인 기준으로 묶는 구조**를 기본으로 한다.

---

## 5. 코드 스타일 / 규칙 (Java & Spring)

### 5.1 일반 규칙

* 언어:

  * 코드 주석과 문서화는 **한국어 사용 가능**,
    클래스/메서드 JavaDoc은 가능하면 간결한 영어와 병행 사용을 권장한다.
* 네이밍:

  * 클래스: `PascalCase`
  * 메서드/변수: `camelCase`
  * 상수: `UPPER_SNAKE_CASE`
* 의미 없는 축약 사용 금지 (`tmp`, `a1`, `b2` 등).

### 5.2 Spring / JPA 규칙

* Entity ↔ DTO 분리:

  * Controller 레벨에서는 **Entity를 직접 노출하지 않는다.**
  * 요청/응답은 반드시 DTO를 사용한다.

* DTO 변환:

  * Entity ↔ DTO 변환은 다음 중 하나의 패턴을 사용한다.

    * DTO 내부에 `from(Entity entity)`, `toEntity()`와 같은 **정적 메서드** 또는 생성자 제공
    * 복잡한 매핑(여러 엔티티 결합 등)이 필요한 경우 전용 `*Mapper` 클래스 사용
  * Controller 내부에서 필드를 하나씩 임의로 옮겨 담는 로직은 최소화한다.
  * Service는 가급적 DTO를 반환하거나, Entity를 반환하더라도 Controller에서 DTO 변환 규칙을 일관되게 적용한다.

* API 응답 통일:

  * 모든 HTTP API 응답은 `ApiResponse<T>` 래퍼로 감싸서 반환한다.

    * 성공 응답:

      * `ApiResponse.success(T data)` 형태 사용을 기본으로 한다.
      * 필드 예시:

        * `code`: "SUCCESS" 등 도메인 정의 코드
        * `message`: 기본 성공 메시지 또는 상황별 메시지
        * `data`: 실제 응답 데이터
    * 실패 응답:

      * `GlobalExceptionHandler`에서 처리한 **통일된 에러 포맷** 반환
      * HTTP Status 코드 + `ApiResponse` 형태의 에러 바디 (`code`, `message`, `data = null`)
  * 개별 컨트롤러에서 임의의 응답 포맷(Map, 문자열 등)을 새로 정의하지 않는다.
  * 필요 시 `ResponseEntity<ApiResponse<T>>` 형태로 상태코드를 함께 제어한다.

* 트랜잭션:

  * **읽기 전용 조회**에는 `@Transactional(readOnly = true)` 명시.
  * 변경이 발생하는 서비스 메서드에만 일반 `@Transactional` 사용.

* Lazy 로딩과 N+1:

  * 조회용 쿼리에 대해서는 필요한 경우 JPQL fetch join 또는 전용 read model 사용.
  * 대량 리스트 API에서 N+1 문제가 발생하지 않도록 주의한다.

---

## 6. DB 설계 원칙 (PostgreSQL)

* 테이블/컬럼 네이밍:

  * 스네이크 케이스 사용: `member`, `review`, `restaurant`, `created_at`, `updated_at`
* 공통 컬럼:

  * 모든 주요 엔티티에 `id`, `created_at`, `updated_at` 기본 포함.
  * soft delete가 필요한 경우 `deleted_at` 또는 `is_deleted` 사용 (프로젝트 정책에 따름).
* 인덱스:

  * 자주 조회되는 컬럼(예: `restaurant_id`, `member_id`, `rating`, `created_at`)에 적절한 인덱스 추가.
* 리뷰/평점 관련:

  * 개별 리뷰 테이블과 별도로, 매장별 **집계/보정용 테이블** 또는 컬럼을 도입할 수 있다.
  * 평점 집계/보정 로직은 쿼리 내부에만 숨기지 말고, 별도 서비스/설계 문서로 명시한다.

---

## 7. 테스트 / 품질

* 단위 테스트:

  * Service 레이어와 핵심 비즈니스 로직에 대해 단위 테스트 작성 필수.
* 통합 테스트:

  * 주요 API에 대해 Spring Boot Test + Testcontainers(PostgreSQL) 또는 H2(전략 선택)에 기반한 통합 테스트 작성.
* 코드 변경 시 원칙:

  * 기존 테스트를 깨뜨리는 변경은 반드시 이유를 명확히 문서화하고, 필요 시 테스트를 수정한다.

---

## 8. 보안 / 개인정보

* 비밀번호:

  * 반드시 **단방향 해시(BCrypt 등)** 사용.
* 인증/인가:

  * 초기에는 Session 기반 또는 간단한 JWT 기반 인증 중 하나를 선택하여 일관되게 적용한다.
* 로그:

  * 로그인 실패, 권한 오류, 중요 비즈니스 이벤트(신고, 리뷰 삭제 등)는 로그로 남긴다.
  * 개인정보(이메일, IP 등)는 필요 이상으로 로그에 남기지 않는다.

---

## 9. 작업 방식 / Git 전략

* 기본 브랜치:

  * `main`: 배포 가능한 안정 버전
  * `develop`: 일상 개발용 (또는 trunk 기반 전략 선택 시 `main`만 사용)
* 작업 브랜치:

  * 기능 단위 브랜치 사용: `feature/review-api`, `feature/member-tier` 등
* 커밋 메시지 규칙(예시):

  * `feat:`, `fix:`, `refactor:`, `test:`, `docs:` 등의 prefix 사용

---

## 10. AI 도구(LLM) 사용 원칙

※ 이 섹션은 **여러 LLM을 사용할 때 공통적으로 지켜야 할 원칙**을 정의한다.
(각 도구별 역할은 개별 프롬프트/환경 설정에서 분리 정의)

* 모든 LLM은 아래를 기본 전제로 작업해야 한다.

  1. 이 `agent.md`에 정의된 **도메인 규칙, 기술 스택, 코드 스타일, 아키텍처 원칙**을 우선적으로 따른다.
  2. 프레임워크/라이브러리 버전과 맞지 않는 코드는 제안하지 않는다.
  3. 새로운 파일/구조를 제안할 때는:

     * 파일 경로
     * 책임/역할
     * 기존 구조와의 관계
       를 먼저 설명한 뒤, 코드를 제안한다.
  4. API 설계/DB 스키마를 변경할 때는:

     * 변경 이유
     * 기존과의 호환성
     * 마이그레이션 방안(필요 시)
       를 간단히 함께 설명한다.
  5. 응답 포맷은 반드시 `ApiResponse<T>` 기준을 따른다.
     예외적인 응답 형식을 사용해야 할 경우, 그 이유를 먼저 설명한다.

* 코드 자동 생성 시 유의사항:

  * 컴파일/빌드 에러가 나지 않도록, **실제 사용 버전(Java 21, Spring Boot 3.x, PostgreSQL)**에 맞춘 코드를 작성한다.
  * 예시 코드와 실제 운영 코드의 경계를 명확히 구분하여 설명한다.

---

## 11. 문서 관리

* 이 `agent.md`는 다음 변경이 발생할 때마다 업데이트한다.

  * 언어/프레임워크/DB 버전 변경
  * 주요 도메인 규칙(리뷰 정책, 평점 산정 방식) 변경
  * 아키텍처/패키지 구조 변경
* 변경 시:

  * Git 커밋 메시지에 `docs(agent): ...` 형태로 명시
  * 관련 이슈/PR 링크를 문서 상단 또는 하단에 추가 (선택)

---

## 12. TODO / 미정 사항

아직 확정되지 않았지만 향후 정의해야 할 항목들을 적어 둔다.
