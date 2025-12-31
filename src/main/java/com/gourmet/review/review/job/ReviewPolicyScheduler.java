package com.gourmet.review.review.job;

import com.gourmet.review.review.service.ReviewPolicyJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 문서(Functional Requirements v1.0) 기준 배치 스케줄러.
 *
 * 주의: 운영에서는 락/중복 실행 방지(ShedLock 등) 적용 권장.
 */
@Profile("!test")
@Component
@RequiredArgsConstructor
public class ReviewPolicyScheduler {

    private final ReviewPolicyJobService policyJobService;

    /**
     * 00:00 - 시간감가 반영을 위한 점수 재계산
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void recalculateStoresForTimeDecay() {
        policyJobService.recalculateStoresForTimeDecay();
    }

    /**
     * 02:00 - 편차 보정 대상 산정
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void refreshDeviationTargets() {
        policyJobService.refreshDeviationTargets();
    }

    /**
     * 04:00 - 등급 승급/강등 심사
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void runTierEvaluation() {
        policyJobService.runTierEvaluation();
    }

    /**
     * 10분마다 - 쿨다운 만료 처리
     * (요구사항은 12시간 후 자동 승인이라, 분 단위로 폴링)
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void processCooldownExpirations() {
        policyJobService.processCooldownExpirations();
    }
}
