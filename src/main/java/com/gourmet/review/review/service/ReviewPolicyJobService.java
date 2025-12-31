package com.gourmet.review.review.service;

import com.gourmet.review.domain.enums.MemberTier;

/**
 * 정책/배치 작업을 담당하는 서비스.
 * - Security/인가와 무관하게 내부 스케줄러/운영툴에서 호출하기 위한 진입점
 */
public interface ReviewPolicyJobService {

    /**
     * 1점/5점 리뷰 쿨다운 만료 처리: PENDING -> APPROVED (조건 충족 시)
     */
    int processCooldownExpirations();

    /**
     * 편차 보정 대상 산정(최근 20개 PUBLIC 리뷰 중 1/5점 비율 90% 이상)
     */
    int refreshDeviationTargets();

    /**
     * 00:00 시간감가 반영을 위한 스토어 점수 재계산.
     * (단순 구현: PUBLIC 리뷰가 있는 모든 스토어 재계산)
     */
    int recalculateStoresForTimeDecay();

    /**
     * 04:00 등급 승급/강등 배치.
     */
    int runTierEvaluation();

    /**
     * 회원 tier 변경(관리자/배치)에 따른 소급 재계산 및 BLACK 제재 처리.
     */
    void handleMemberTierChanged(Long memberId, MemberTier oldTier, MemberTier newTier);
}

