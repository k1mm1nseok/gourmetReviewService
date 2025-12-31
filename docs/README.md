# docs/

이 폴더는 **설계/명세/운영 가이드 문서**를 모아둔 곳입니다. (코드와 분리해 관리)

## 빠른 링크

- 아키텍처/핵심 API 스펙: `backend-architecture-and-api-spec.md`
- 기능 요구사항: `functional-requirements-v1.0.md`
- 리뷰 정책: `review-policy-v1.3.2.md`
- 스모크 테스트 가이드: `dev-smoke-test.md`
- 인수인계 메모: `handoff.md`

## schema/

DB 스키마/DDL/마이그레이션 관련 자료는 `docs/schema/` 하위에 있습니다.

- ERD/스키마 설명: `schema/ERD-SCHEMA.md`
- DDL (PostgreSQL): `schema/gourmet-review-service-ddl-postgresql.sql`
- DDL (MySQL): `schema/gourmet-review-service-ddl.sql`
- 마이그레이션 스크립트: `schema/migration-*.sql`
- 스키마 문서 안내: `schema/README.md`

## 문서 버전 관리 메모

- 파일명에 버전이 붙어있는 문서는(예: `review-policy-v1.3.2.md`) **버전 고정 문서**입니다.
- 최신 단일 파일로 유지해야 하는 문서는 파일명에 버전을 붙이지 않고, 변경 이력(Changelog)을 본문에 남기는 방식 권장.

