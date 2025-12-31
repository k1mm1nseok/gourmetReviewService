package com.gourmet.review.review.service;

import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.Review;
import com.gourmet.review.domain.entity.Store;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.domain.enums.ReviewStatus;
import com.gourmet.review.review.repository.ReviewRepository;
import com.gourmet.review.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewScoreServiceImpl implements ReviewScoreService {

    private static final BigDecimal BASELINE_SCORE = new BigDecimal("3.0");
    private static final BigDecimal MIN_REVIEW_WEIGHT = new BigDecimal("30.0");
    private static final BigDecimal DEVIATION_ADJUSTMENT = new BigDecimal("0.5");
    private static final BigDecimal MIN_SCORE = new BigDecimal("1.0");
    private static final BigDecimal MAX_SCORE = new BigDecimal("5.0");

    private static final BigDecimal WEIGHT_BRONZE = new BigDecimal("0.5");
    private static final BigDecimal WEIGHT_SILVER = new BigDecimal("1.0");
    private static final BigDecimal WEIGHT_GOLD = new BigDecimal("1.5");
    private static final BigDecimal WEIGHT_GOURMET = new BigDecimal("2.0");
    private static final BigDecimal WEIGHT_BLACK = new BigDecimal("0.0");

    private static final BigDecimal DECAY_RECENT = new BigDecimal("1.0");
    private static final BigDecimal DECAY_6_TO_12 = new BigDecimal("0.8");
    private static final BigDecimal DECAY_1_TO_2Y = new BigDecimal("0.5");
    private static final BigDecimal DECAY_2_TO_3Y = new BigDecimal("0.2");
    private static final BigDecimal DECAY_3Y_PLUS = new BigDecimal("0.1");

    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void recalculateStoreScores(Store store) {
        List<Review> publicReviews = reviewRepository.findByStoreIdAndStatus(store.getId(), ReviewStatus.PUBLIC);
        store.updateReviewCountValid(publicReviews.size());
        store.updateAvgRating(calculateAverageScore(publicReviews));
        store.updateScoreWeighted(calculateWeightedScore(publicReviews));
    }

    @Override
    @Transactional
    public void recalculateStoreScoresByStoreIds(Collection<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return;
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long id : storeIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        if (unique.isEmpty()) {
            return;
        }
        for (Long storeId : unique) {
            Store store = storeRepository.findById(storeId).orElse(null);
            if (store == null) {
                continue;
            }
            recalculateStoreScores(store);
        }
    }

    private BigDecimal calculateAverageScore(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Review review : reviews) {
            sum = sum.add(review.getScoreCalculated());
        }
        return sum.divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWeightedScore(List<Review> reviews) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Review review : reviews) {
            Member member = review.getMember();
            BigDecimal score = applyDeviationAdjustment(review.getScoreCalculated(), member);
            BigDecimal weight = getTierWeight(member.getTier()).multiply(getTimeDecay(review.getCreatedAt()));
            weightedSum = weightedSum.add(score.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        BigDecimal average = totalWeight.compareTo(BigDecimal.ZERO) == 0
                ? BASELINE_SCORE
                : weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);

        BigDecimal finalScore = average.multiply(totalWeight)
                .add(BASELINE_SCORE.multiply(MIN_REVIEW_WEIGHT))
                .divide(totalWeight.add(MIN_REVIEW_WEIGHT), 4, RoundingMode.HALF_UP);

        return finalScore.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyDeviationAdjustment(BigDecimal score, Member member) {
        if (member == null || !Boolean.TRUE.equals(member.getIsDeviationTarget())) {
            return score;
        }
        int compare = score.compareTo(BASELINE_SCORE);
        BigDecimal adjusted = score;
        if (compare > 0) {
            adjusted = score.subtract(DEVIATION_ADJUSTMENT);
        } else if (compare < 0) {
            adjusted = score.add(DEVIATION_ADJUSTMENT);
        }
        if (adjusted.compareTo(MIN_SCORE) < 0) {
            return MIN_SCORE;
        }
        if (adjusted.compareTo(MAX_SCORE) > 0) {
            return MAX_SCORE;
        }
        return adjusted;
    }

    private BigDecimal getTierWeight(MemberTier tier) {
        return switch (tier) {
            case BRONZE -> WEIGHT_BRONZE;
            case SILVER -> WEIGHT_SILVER;
            case GOLD -> WEIGHT_GOLD;
            case GOURMET -> WEIGHT_GOURMET;
            case BLACK -> WEIGHT_BLACK;
        };
    }

    private BigDecimal getTimeDecay(LocalDateTime createdAt) {
        if (createdAt == null) {
            return DECAY_RECENT;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (createdAt.isAfter(now.minusMonths(6))) {
            return DECAY_RECENT;
        }
        if (createdAt.isAfter(now.minusYears(1))) {
            return DECAY_6_TO_12;
        }
        if (createdAt.isAfter(now.minusYears(2))) {
            return DECAY_1_TO_2Y;
        }
        if (createdAt.isAfter(now.minusYears(3))) {
            return DECAY_2_TO_3Y;
        }
        return DECAY_3Y_PLUS;
    }
}
