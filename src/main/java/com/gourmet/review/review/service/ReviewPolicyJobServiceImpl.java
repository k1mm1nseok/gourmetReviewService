package com.gourmet.review.review.service;

import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.Review;
import com.gourmet.review.domain.entity.Store;
import com.gourmet.review.domain.entity.MemberStoreVisit;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.domain.enums.ReviewStatus;
import com.gourmet.review.member.repository.MemberRepository;
import com.gourmet.review.review.repository.ReviewRepository;
import com.gourmet.review.store.repository.StoreRepository;
import com.gourmet.review.review.repository.MemberStoreVisitRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewPolicyJobServiceImpl implements ReviewPolicyJobService {

    private static final BigDecimal EXTREME_MIN = new BigDecimal("1.0");
    private static final BigDecimal EXTREME_MAX = new BigDecimal("5.0");

    private static final int EXTREME_SAMPLE_SIZE = 20;
    private static final BigDecimal EXTREME_RATIO_THRESHOLD = new BigDecimal("0.9");

    private static final int COOLDOWN_HOURS = 12;

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final MemberStoreVisitRepository memberStoreVisitRepository;

    private final ReviewScoreService reviewScoreService;
    private final Clock clock;

    @Override
    @Transactional
    public int processCooldownExpirations() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusHours(COOLDOWN_HOURS);
        List<Review> pending = reviewRepository.findByStatusAndCreatedAtBefore(ReviewStatus.PENDING, cutoff);

        int processed = 0;
        for (Review review : pending) {
            if (isCooldownTarget(review)) {
                review.approve();
                processed++;

                // 쿨다운 만료로 APPROVED가 된 리뷰는, 기존 승인 흐름(블라인드 해제/공개 전환/점수 재계산)에 편입돼야 한다.
                // - store별 5개 이상이면 APPROVED/BLIND_HELD를 PUBLIC으로 전환하고 점수 재계산
                // - 아직 5개 미만이면 BLIND_HELD로 두고 점수는 미반영
                handleApproveSideEffects(review.getStore());
            }
        }
        return processed;
    }

    private void handleApproveSideEffects(Store store) {
        if (store == null) {
            return;
        }

        long approvedCount = reviewRepository.countByStoreIdAndStatusIn(store.getId(),
                List.of(ReviewStatus.APPROVED, ReviewStatus.BLIND_HELD, ReviewStatus.PUBLIC));

        if (approvedCount < 5) {
            // 아직 블라인드 단계: APPROVED 상태 리뷰들을 BLIND_HELD로 유지(점수 미반영)
            List<Review> toHold = reviewRepository.findByStoreIdAndStatusIn(store.getId(),
                    List.of(ReviewStatus.APPROVED));
            for (Review r : toHold) {
                r.holdForBlind();
            }
            return;
        }

        // 5개 이상: APPROVED/BLIND_HELD를 PUBLIC으로 전환(방문횟수 반영은 ReviewServiceImpl approve와 동일 정책)
        List<Review> publishTargets = reviewRepository.findByStoreIdAndStatusIn(store.getId(),
                List.of(ReviewStatus.APPROVED, ReviewStatus.BLIND_HELD));
        for (Review target : publishTargets) {
            if (target.publish()) {
                applyVisitCount(target);
            }
        }
        reviewScoreService.recalculateStoreScores(store);
    }

    private void applyVisitCount(Review review) {
        if (review == null || review.getMember() == null || review.getStore() == null) {
            return;
        }
        MemberStoreVisit visit = memberStoreVisitRepository
                .findByMemberIdAndStoreId(review.getMember().getId(), review.getStore().getId())
                .orElseGet(() -> memberStoreVisitRepository.save(MemberStoreVisit.builder()
                        .member(review.getMember())
                        .store(review.getStore())
                        .build()));
        int visitCount = visit.incrementVisitCount();
        review.updateVisitCount(visitCount);
    }

    private boolean isCooldownTarget(Review review) {
        Member member = review.getMember();
        if (member == null) {
            return false;
        }
        if (member.getTier() != MemberTier.BRONZE && member.getTier() != MemberTier.SILVER) {
            return false;
        }
        BigDecimal score = review.getScoreCalculated();
        if (score == null) {
            return false;
        }
        return score.compareTo(EXTREME_MIN) == 0 || score.compareTo(EXTREME_MAX) == 0;
    }

    @Override
    @Transactional
    public int refreshDeviationTargets() {
        List<Member> members = memberRepository.findAll();
        int updated = 0;

        for (Member member : members) {
            List<Review> recent = reviewRepository.findTop20ByMemberIdAndStatusOrderByCreatedAtDesc(member.getId(), ReviewStatus.PUBLIC);
            boolean changed = false;

            if (recent.size() < EXTREME_SAMPLE_SIZE) {
                if (Boolean.TRUE.equals(member.getIsDeviationTarget())) {
                    member.markAsDeviationTarget(false);
                    updated++;
                    changed = true;
                }
                if (changed) {
                    triggerRecalculationForMemberStores(member.getId());
                }
                continue;
            }

            int extremeCount = 0;
            for (Review r : recent) {
                BigDecimal score = r.getScoreCalculated();
                if (score == null) {
                    continue;
                }
                if (score.compareTo(EXTREME_MIN) == 0 || score.compareTo(EXTREME_MAX) == 0) {
                    extremeCount++;
                }
            }

            BigDecimal ratio = new BigDecimal(extremeCount)
                    .divide(new BigDecimal(EXTREME_SAMPLE_SIZE), 4, java.math.RoundingMode.HALF_UP);
            boolean isTarget = ratio.compareTo(EXTREME_RATIO_THRESHOLD) >= 0;

            if (!Boolean.valueOf(isTarget).equals(member.getIsDeviationTarget())) {
                member.markAsDeviationTarget(isTarget);
                updated++;
                changed = true;
            }

            if (changed) {
                triggerRecalculationForMemberStores(member.getId());
            }
        }

        return updated;
    }

    private void triggerRecalculationForMemberStores(Long memberId) {
        if (memberId == null) {
            return;
        }
        List<Long> storeIds = reviewRepository.findDistinctStoreIdsByMemberIdAndStatus(memberId, ReviewStatus.PUBLIC);
        reviewScoreService.recalculateStoreScoresByStoreIds(storeIds);
    }

    @Override
    @Transactional
    public int recalculateStoresForTimeDecay() {
        // 단순 구현(정확): PUBLIC 리뷰가 하나라도 있는 스토어는 전부 재계산
        // 최적화는 추후(createdAt 구간 바뀐 리뷰가 있는 스토어만) 가능
        List<Long> storeIds = reviewRepository.findDistinctStoreIdsByStatusAndStoreIdIn(
                ReviewStatus.PUBLIC,
                storeRepository.findAll().stream().map(Store::getId).toList()
        );

        reviewScoreService.recalculateStoreScoresByStoreIds(storeIds);
        return storeIds.size();
    }

    @Override
    @Transactional
    public int runTierEvaluation() {
        // 문서 기반 승급/강등 규칙이 상세하지만, 현재 엔티티/필드 스키마로는
        // "검수 통과 리뷰 수" 같은 조건을 정확히 계산하기 어렵다.
        // 따라서 본 구현은 최소한의 '활동성 기반 강등'만 반영한다.
        // - GOLD: lastReviewAt 1년 이상이면 SILVER로 강등
        // - GOURMET: 최근 6개월 내 리뷰 10개 미만이면 GOLD로 강등 -> 현재는 최근 6개월 리뷰 카운트 쿼리가 없어 TODO

        List<Member> members = memberRepository.findAll();
        int changed = 0;
        LocalDateTime now = LocalDateTime.now(clock);

        for (Member member : members) {
            MemberTier oldTier = member.getTier();
            MemberTier newTier = oldTier;

            if (oldTier == MemberTier.GOLD) {
                if (member.getLastReviewAt() != null && member.getLastReviewAt().isBefore(now.minusYears(1))) {
                    newTier = MemberTier.SILVER;
                }
            }

            if (newTier != oldTier) {
                member.forceUpdateTier(newTier);
                handleMemberTierChanged(member.getId(), oldTier, newTier);
                changed++;
            }
        }

        return changed;
    }

    @Override
    @Transactional
    public void handleMemberTierChanged(Long memberId, MemberTier oldTier, MemberTier newTier) {
        if (memberId == null || oldTier == null || newTier == null || oldTier == newTier) {
            return;
        }

        // BLACK 전환: 해당 회원의 PUBLIC 리뷰를 SUSPENDED로 전환
        if (newTier == MemberTier.BLACK) {
            List<Review> publicReviews = reviewRepository.findByMemberId(memberId, org.springframework.data.domain.Pageable.unpaged()).getContent();
            for (Review r : publicReviews) {
                if (r.getStatus() == ReviewStatus.PUBLIC) {
                    r.suspend("BLACK 등급 전환으로 인한 일시정지");
                }
            }
        }

        // 소급 재계산: 해당 회원이 PUBLIC 리뷰를 남긴 store 재계산
        List<Long> storeIds = reviewRepository.findDistinctStoreIdsByMemberIdAndStatus(memberId, ReviewStatus.PUBLIC);
        reviewScoreService.recalculateStoreScoresByStoreIds(storeIds);
    }
}
