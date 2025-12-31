# Handoff - Review Baseline

## Context
 - Repository: `/mnt/c/users/kim/desktop/gourmetReviewService`
 - Goal: 리뷰 도메인 기본 틀 완성 (리뷰 CRUD, 도움됨, 검수 승인/반려, 스토어 최근 리뷰/평점 연동)
 - Reference docs:
  - `docs/review-policy-v1.3.3.md`
  - `docs/backend-architecture-and-api-spec.md`
  - `docs/functional-requirements-v1.0.md`
 - Note: git worktree is dirty with many unrelated changes; do not reset or revert without user request.

## What Was Implemented
- Review API controllers:
  - `src/main/java/com/gourmet/review/review/controller/ReviewController.java`
  - `src/main/java/com/gourmet/review/review/controller/MyReviewController.java`
  - `src/main/java/com/gourmet/review/review/controller/AdminReviewController.java`
- Review service logic:
  - `src/main/java/com/gourmet/review/review/service/ReviewServiceImpl.java`
  - `src/main/java/com/gourmet/review/review/service/ReviewService.java`
- Review DTOs:
  - `src/main/java/com/gourmet/review/review/dto/ReviewModerationResponse.java`
- Repositories:
  - `src/main/java/com/gourmet/review/review/repository/ReviewRepository.java`
  - `src/main/java/com/gourmet/review/review/repository/ReviewImageRepository.java`
  - `src/main/java/com/gourmet/review/review/repository/ReviewHelpfulRepository.java`
- Entity helpers:
  - `src/main/java/com/gourmet/review/domain/entity/Review.java`
  - `src/main/java/com/gourmet/review/domain/entity/Store.java`
- Store detail recent reviews:
  - `src/main/java/com/gourmet/review/store/service/StoreServiceImpl.java`

## Behavior Summary (Spec-Aligned)
- 리뷰 작성:
  - 상태 `PENDING` 기본.
  - 종합 점수 `scoreCalculated`는 엔티티에서 자동 계산.
  - 가게 `reviewCount` 및 회원 `reviewCount` 증가.
  - 리뷰 이미지 저장.
- 리뷰 상세:
  - 이미지 리스트 포함.
  - 댓글은 현재 범위 제외로 빈 리스트.
- 도움됨:
  - 회원은 리뷰당 1회만 가능 (ReviewHelpful unique).
  - 리뷰 도움됨 카운트 + 작성자 도움됨 카운트 증감.
- 검수 승인:
  - `PENDING`만 승인 가능.
  - 해당 가게의 승인/보류/공개 리뷰 수가 5 미만이면 `BLIND_HELD`.
  - 5 이상이면 `APPROVED`/`BLIND_HELD` 모두 `PUBLIC` 전환, 방문횟수 반영, 평점 재계산.
- 평점 계산:
  - 가중 평균 + 베이지안 보정(m=30, C=3.0).
  - 사용자 티어 가중치 + 시간 감가상각 적용.
  - 편차 보정 대상은 3.0 방향 ±0.5 적용.
- 스토어 상세:
  - 최근 PUBLIC 리뷰 3개, 이미지 포함.
  - 블라인드 상태일 때 “수집된 리뷰 수”는 APPROVED/BLIND_HELD/PUBLIC 합계 기준.

## Open Items / TODO
- [보안] 관리자 권한 제어 미구현 (현재는 SecurityConfig 기본 인증만 존재)
  - NOTE: `/admin/**` 엔드포인트는 컨트롤러/서비스에서 Member.role==ADMIN을 추가로 체크하지만,
    Spring Security 차원의 role 기반 인가(예: hasRole('ADMIN'))는 아직 없음.
- [보안] Basic Auth principal이 숫자 ID가 아니라서 `SecurityUtil` 로직과 충돌 가능

## Implemented Since Last Handoff (P0)
- [쿨다운] 1.0/5.0 리뷰는 BRONZE/SILVER에서 12시간 `PENDING` 유지 후 배치에서 자동 `APPROVED` 처리  
  - `ReviewPolicyJobServiceImpl.processCooldownExpirations()` + `ReviewPolicyScheduler`(10분 폴링)
  - 만료 승인 이후 스토어 블라인드/공개 전환 및 점수 재계산까지 동일 경로로 반영
- [편차보정 배치] `Member.isDeviationTarget` 자동 산정 배치 구현 + 변경 시 관련 스토어 점수 재계산 트리거
  - 최근 PUBLIC 20개 중 scoreCalculated 1.0/5.0 비율 90% 이상이면 deviation target
  - `ReviewPolicyJobServiceImpl.refreshDeviationTargets()`
- [티어 변경 소급] tier 변경 시 해당 회원의 PUBLIC 리뷰가 반영되는 스토어 점수 재계산
  - `ReviewPolicyJobServiceImpl.handleMemberTierChanged()`
- [관리자 수동 변경] 관리자(ADMIN)가 회원 tier/role을 수동 변경하는 API 추가 + tier 변경 시 소급 재계산 트리거
  - API: `PATCH /admin/members/{memberId}/tier`, `PATCH /admin/members/{memberId}/role`
  - `AdminMemberController`, `MemberServiceImpl.adminUpdateMemberTier/adminUpdateMemberRole`
  - 테스트: `AdminMemberControllerTest`에 tier/role endpoint 위임 + validation(400) 케이스 추가

- [블라인드 점수 마스킹 범위 확장] 블라인드 가게일 때 리뷰 상세에서도 점수(score*)를 노출하지 않도록 마스킹
  - `ReviewServiceImpl.getReview()`에서 store.isBlind 체크 후 `ReviewDetailResponse.score* = null`

## Testing
- `./mvnw test` 통과
- 추가: `AdminMemberControllerTest`(관리자 tier 변경 시 `handleMemberTierChanged` 호출 검증)
- 주의: 테스트 환경에서 스케줄러가 동작하면 DB create-drop 타이밍에 따라 에러가 날 수 있어, test 프로파일에서는 스케줄러 비활성화를 권장

## Test Stability Notes (Added)
- **기본 테스트 실행(`mvn test`)에서는 느린 시뮬레이션(@Tag("slow"))을 제외**한다.
  - surefire를 JUnit5 @Tag 방식(`junit.jupiter.tags.exclude=slow`)으로 설정해, 테스트 디스커버리/집계가 정상 동작하도록 수정함.
  - 필요 시에만 실행: `-DincludeTags=junit.jupiter.tags.include=slow` 형태로 configurationParameters를 추가하거나(권장), 별도 프로파일로 분리.

## Next Session Prompt
```
You are continuing the Gourmet Review Service in /mnt/c/users/kim/desktop/gourmetReviewService.
Follow agents.md and review-policy-v1.3.3.md.
Review domain baseline is implemented (controllers, services, repos, store recent reviews).
Focus next on TODOs: cooldown for 1.0/5.0 reviews, admin auth/role guards, deviation target batch,
tier change re-weighting, and any missing tests. Do not reset the git worktree.
```
