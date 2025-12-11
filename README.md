# Gourmet Review Service

맛집 리뷰 플랫폼 백엔드 서비스 (Spring Boot 3.x + Java 21 + PostgreSQL)

## 📋 프로젝트 정보

- **버전**: v1.3.2
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

### 1. 사전 요구사항

- Java 21
- PostgreSQL 15+
- Maven 3.8+

### 2. 데이터베이스 설정

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
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

### 4. 애플리케이션 실행

```bash
# 빌드 및 실행
./mvnw spring-boot:run

# 또는 JAR 빌드 후 실행
./mvnw clean package
java -jar target/review-1.0.0-SNAPSHOT.jar
```

## 📚 문서

- [agents.md](docs/agents.md) - 개발 가이드 및 프로젝트 구조
- [review-policy.md](docs/review-policy.md) - 리뷰 정책 및 평점 산정 로직
- [functional-requirements.md](docs/functional-requirements.md) - 기능 요구사항
- [backend-architecture-and-api-spec.md](docs/backend-architecture-and-api-spec.md) - 아키텍처 및 API 명세

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

### 빌드

```bash
# 컴파일
./mvnw clean compile

# 테스트
./mvnw test

# 패키징
./mvnw clean package
```

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
