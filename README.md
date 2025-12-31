# Gourmet Review Service

맛집 리뷰 플랫폼 백엔드 서비스 (Spring Boot 3.x + Java 21 + PostgreSQL)

## 📋 프로젝트 정보

- **버전**: v1.3.2
- **문서 최신화**: 2025-12-31
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.2.5
- **ORM**: Spring Data JPA (Jakarta Persistence)
- **데이터베이스**: PostgreSQL
- **빌드 도구**: Maven

## 🏗️ 아키텍처

레이어드 아키텍처 기반 설계:

```
Controller (API) → Service (비즈니스 로직) → Repository (영속성) → PostgreSQL
```

자세한 내용은 `docs/backend-architecture-and-api-spec.md` 참고

## 🚀 시작하기

## 🧪 테스트

- 기본 테스트 실행(`mvn test`)에서는 느린 시뮬레이션 테스트(`@Tag("slow")`)를 제외합니다.
- 시뮬레이션이 필요할 때는 IDE에서 `RatingSimulationTest`만 단독 실행(또는 Maven에서 특정 테스트 지정)하세요.

```bash
./mvnw test

# (옵션) 시뮬레이션 단독 실행
./mvnw -Dtest=RatingSimulationTest test
```

### 1. 사전 요구사항

- Java 21
- PostgreSQL 15+
- Maven 3.8+

### 2. 데이터베이스 설정

#### (권장) Docker로 PostgreSQL 실행

프로젝트 루트의 `docker-compose.yml`을 사용하면 기본 설정으로 바로 실행됩니다.

```bash
docker compose up -d postgres

# 컨테이너 상태 확인
docker compose ps
```

기본 접속 정보:

- DB: `gourmet_review`
- Username: `postgres`
- Password: `postgres`
- Port: `5432`

#### 수동 설치/기존 PostgreSQL 사용 시

```bash
# PostgreSQL 데이터베이스 생성
createdb gourmet_review

# DDL 스크립트 실행
psql -U postgres -d gourmet_review -f docs/schema/gourmet-review-service-ddl-postgresql.sql
```

### 3. 애플리케이션 설정

`src/main/resources/application.yml` 파일에서 데이터베이스 접속 정보 수정:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/gourmet_review
    username: your_username
    password: your_password
```

또는 환경 변수로 설정:

```bash
# macOS / Linux
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

Windows PowerShell:

```powershell
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
```

Windows CMD:

```cmd
set DB_USERNAME=your_username
set DB_PASSWORD=your_password
```

> `application.yml` 기본값은 `postgres/postgres` 이므로, 로컬 PostgreSQL 비밀번호가 다르면 반드시 위처럼 오버라이드해야 합니다.

### 4. 애플리케이션 실행

```bash
# 빌드 및 실행
./mvnw spring-boot:run

# 또는 JAR 빌드 후 실행
./mvnw clean package
java -jar target/review-1.0.0-SNAPSHOT.jar
```

## 📚 문서

 - `agents.md` - 패키지/레이어 컨벤션
 - `docs/README.md` - docs/ 문서 인덱스(무엇을 먼저 볼지)
 - `docs/review-policy-v1.3.3.md` - 리뷰 정책 및 평점 산정 로직
 - `docs/functional-requirements-v1.0.md` - 기능 요구사항
 - `docs/backend-architecture-and-api-spec.md` - 아키텍처 및 API 명세
 - `docs/dev-smoke-test.md` - dev 부팅 + stores API 스모크 테스트

> 문서 파일명은 버전이 포함되어 있으니 링크 경로를 위 목록 기준으로 사용하세요.

## 🗃️ 데이터베이스 스키마

### 주요 테이블

- **member**: 회원 정보 (5단계 등급제: BRONZE, SILVER, GOLD, GOURMET, BLACK)
- **store**: 가게 정보 (카테고리, 지역, 평점, 블라인드 처리)
- **review**: 리뷰 (다차원 평점, 상태 관리, 검수 시스템)
- **comment**: 댓글 (리뷰/게시글)
- **category**: 카테고리 (계층 구조)
- **region**: 지역 (계층 구조)

자세한 DDL은 `docs/schema/gourmet-review-service-ddl-postgresql.sql` 참고

## 🔑 주요 기능

### 회원 시스템
- 5단계 등급제 (리뷰 수 & 도움됨 수 기반 자동 승급)
- 편차 보정 시스템 (극단적 평점 패턴 감지)
- BCrypt 기반 비밀번호 암호화

### 리뷰 시스템
- **다차원 평점**: 맛(40%) + 가성비(30%) + 분위기(15%) + 서비스(15%)
- **상태 관리**: PENDING → APPROVED → PUBLIC/BLIND_HELD
- **검수 시스템**: 쿨다운(12시간), 관리자 승인/반려
- **시간 감가상각**: 6개월 이상 리뷰 가중치 감소

### 가게 시스템
- **블라인드 정책**: 리뷰 5개 미만 가게 평점 비공개
- **베이지안 평균**: 신뢰도 높은 평점 산정
- **카테고리/지역 계층 구조**

## 🛠️ 개발 환경

### 프로파일

```bash
# 개발 환경
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 운영 환경
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# 테스트 환경 (H2 In-Memory DB)
./mvnw test -Dspring.profiles.active=test
```

PostgreSQL이 준비되지 않은 상태에서 빠르게 실행만 확인하려면:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

> `spring-boot:run`은 별도 JVM으로 실행되어 Maven의 `-Dspring.profiles.active=...`가 전달되지 않을 수 있습니다.  
> Windows PowerShell에서는 아래처럼 `spring-boot.run.profiles` 옵션을 **따옴표로 감싸서** 넘기는 방법을 권장합니다:
>
> ```powershell
> .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=test"
> ```
>
> 또는 환경변수로 지정해도 됩니다:
>
> ```powershell
> $env:SPRING_PROFILES_ACTIVE="test"
> .\mvnw.cmd spring-boot:run
> ```

### 빌드

```bash
# 컴파일
./mvnw clean compile

# 테스트
./mvnw test

# 패키징
./mvnw clean package
```

#### 테스트 참고
- 기본 `mvn test`에서는 느린 시뮬레이션 테스트(`@Tag("slow")`)를 제외합니다.
  - 필요 시에만 별도로 실행하도록 분리되어 있습니다.
- test 프로파일에서는 배치 스케줄러가 동작하지 않도록 구성되어 있어(create-drop 타이밍 플래키 방지)
  테스트가 안정적으로 동작합니다.

## 📝 변경 이력

### v1.3.2 (2025-12-11)
- **Entity 패치**: Review, Member, MemberTier, Comment 업데이트
  - Review: 필드명 변경 (scoreAmbiance, scoreValue), 가중치 정책 반영
  - Member: password 필드 추가 (BCrypt)
  - MemberTier: SILVER 등급 조건 변경 (5개)
  - Comment: validateTarget() 검증 추가
- **DDL**: PostgreSQL 전환 (MySQL → PostgreSQL)
  - updated_at 자동 갱신 트리거 추가
  - BIGSERIAL, TIMESTAMP 타입 사용

## 📄 라이선스

본 프로젝트는 학습 목적의 개인 프로젝트입니다.

## 👥 기여

이슈 및 PR은 언제든지 환영합니다!
