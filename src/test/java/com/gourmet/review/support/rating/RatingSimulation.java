package com.gourmet.review.support.rating;

import com.gourmet.review.domain.enums.MemberTier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * DB 없이 점수 정책을 몬테카를로로 비교하기 위한 경량 시뮬레이터.
 * - Current: 기존 ReviewScoreServiceImpl 방식(티어 가중치 + 시간감가 + deviationTarget 점수보정 + 베이지안 prior)
 * - Proposed: 점수는 그대로, 유저의 후한 성향(userMean>3.0)에 따라 영향력(weight)을 낮추는 방식
 */
public final class RatingSimulation {

    private RatingSimulation() {}

    public record ReviewSample(
            BigDecimal score, // review.scoreCalculated
            MemberTier tier,
            boolean deviationTarget,
            BigDecimal userMean,
            LocalDateTime createdAt
    ) {}

    // Current 정책 상수(ReviewScoreServiceImpl과 동일)
    public static final BigDecimal BASELINE_SCORE = new BigDecimal("3.0");
    public static final BigDecimal MIN_REVIEW_WEIGHT = new BigDecimal("30.0");
    public static final BigDecimal DEVIATION_ADJUSTMENT = new BigDecimal("0.5");
    public static final BigDecimal MIN_SCORE = new BigDecimal("1.0");
    public static final BigDecimal MAX_SCORE = new BigDecimal("5.0");

    public static final BigDecimal WEIGHT_BRONZE = new BigDecimal("0.5");
    public static final BigDecimal WEIGHT_SILVER = new BigDecimal("1.0");
    public static final BigDecimal WEIGHT_GOLD = new BigDecimal("1.5");
    public static final BigDecimal WEIGHT_GOURMET = new BigDecimal("2.0");
    public static final BigDecimal WEIGHT_BLACK = new BigDecimal("0.0");

    // Proposed(유저 편향 기반 영향력 감쇠) 기본 파라미터
    public record InfluenceConfig(
            BigDecimal beta,      // 후한 정도(bias=userMean-3.0)에 따른 감쇠 강도
            BigDecimal minFactor, // 영향력 하한
            int minReviewsForUserMean // userMean을 신뢰하기 위한 최소 리뷰수(시뮬레이션에서는 외부에서 필터링)
    ) {}

    public static BigDecimal calculateStoreScoreCurrent(Iterable<ReviewSample> reviews, LocalDateTime now) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (ReviewSample r : reviews) {
            BigDecimal score = applyDeviationAdjustment(r.score(), r.deviationTarget());
            BigDecimal weight = tierWeight(r.tier()).multiply(timeDecay(r.createdAt(), now));
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

    public static BigDecimal calculateStoreScoreProposedWeightOnly(
            Iterable<ReviewSample> reviews,
            LocalDateTime now,
            InfluenceConfig config
    ) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (ReviewSample r : reviews) {
            // 점수는 그대로(=scoreCalculated), deviationTarget 점수 보정도 하지 않는 버전(타베로그 느낌)
            BigDecimal score = r.score();

            BigDecimal baseWeight = tierWeight(r.tier()).multiply(timeDecay(r.createdAt(), now));
            BigDecimal factor = influenceFactorPositiveBiasOnly(r.userMean(), config.beta(), config.minFactor());
            BigDecimal weight = baseWeight.multiply(factor);

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

    public static BigDecimal calculateStoreScoreCurrentWithPrior(Iterable<ReviewSample> reviews, LocalDateTime now, BigDecimal priorWeight) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (ReviewSample r : reviews) {
            BigDecimal score = applyDeviationAdjustment(r.score(), r.deviationTarget());
            BigDecimal weight = tierWeight(r.tier()).multiply(timeDecay(r.createdAt(), now));
            weightedSum = weightedSum.add(score.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        BigDecimal average = totalWeight.compareTo(BigDecimal.ZERO) == 0
                ? BASELINE_SCORE
                : weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);

        BigDecimal w = priorWeight == null ? MIN_REVIEW_WEIGHT : priorWeight;
        BigDecimal finalScore = average.multiply(totalWeight)
                .add(BASELINE_SCORE.multiply(w))
                .divide(totalWeight.add(w), 4, RoundingMode.HALF_UP);

        return finalScore.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateStoreScoreProposedWeightOnlyWithPrior(
            Iterable<ReviewSample> reviews,
            LocalDateTime now,
            InfluenceConfig config,
            BigDecimal priorWeight
    ) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (ReviewSample r : reviews) {
            BigDecimal score = r.score();
            BigDecimal baseWeight = tierWeight(r.tier()).multiply(timeDecay(r.createdAt(), now));
            BigDecimal factor = influenceFactorPositiveBiasOnly(r.userMean(), config.beta(), config.minFactor());
            BigDecimal weight = baseWeight.multiply(factor);

            weightedSum = weightedSum.add(score.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        BigDecimal average = totalWeight.compareTo(BigDecimal.ZERO) == 0
                ? BASELINE_SCORE
                : weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);

        BigDecimal w = priorWeight == null ? MIN_REVIEW_WEIGHT : priorWeight;
        BigDecimal finalScore = average.multiply(totalWeight)
                .add(BASELINE_SCORE.multiply(w))
                .divide(totalWeight.add(w), 4, RoundingMode.HALF_UP);

        return finalScore.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateStoreScoreCurrentWithPriorAndDecayFloor(
            Iterable<ReviewSample> reviews,
            LocalDateTime now,
            BigDecimal priorWeight,
            BigDecimal decayFloor
    ) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (ReviewSample r : reviews) {
            BigDecimal score = applyDeviationAdjustment(r.score(), r.deviationTarget());
            BigDecimal weight = tierWeight(r.tier()).multiply(timeDecay(r.createdAt(), now, decayFloor));
            weightedSum = weightedSum.add(score.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        BigDecimal average = totalWeight.compareTo(BigDecimal.ZERO) == 0
                ? BASELINE_SCORE
                : weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);

        BigDecimal w = priorWeight == null ? MIN_REVIEW_WEIGHT : priorWeight;
        BigDecimal finalScore = average.multiply(totalWeight)
                .add(BASELINE_SCORE.multiply(w))
                .divide(totalWeight.add(w), 4, RoundingMode.HALF_UP);

        return finalScore.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateStoreScoreProposedWeightOnlyWithPriorAndDecayFloor(
            Iterable<ReviewSample> reviews,
            LocalDateTime now,
            InfluenceConfig config,
            BigDecimal priorWeight,
            BigDecimal decayFloor
    ) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (ReviewSample r : reviews) {
            BigDecimal score = r.score();
            BigDecimal baseWeight = tierWeight(r.tier()).multiply(timeDecay(r.createdAt(), now, decayFloor));
            BigDecimal factor = influenceFactorPositiveBiasOnly(r.userMean(), config.beta(), config.minFactor());
            BigDecimal weight = baseWeight.multiply(factor);

            weightedSum = weightedSum.add(score.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        BigDecimal average = totalWeight.compareTo(BigDecimal.ZERO) == 0
                ? BASELINE_SCORE
                : weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);

        BigDecimal w = priorWeight == null ? MIN_REVIEW_WEIGHT : priorWeight;
        BigDecimal finalScore = average.multiply(totalWeight)
                .add(BASELINE_SCORE.multiply(w))
                .divide(totalWeight.add(w), 4, RoundingMode.HALF_UP);

        return finalScore.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal applyDeviationAdjustment(BigDecimal score, boolean deviationTarget) {
        if (!deviationTarget) {
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

    private static BigDecimal tierWeight(MemberTier tier) {
        if (tier == null) {
            return WEIGHT_SILVER;
        }
        return switch (tier) {
            case BRONZE -> WEIGHT_BRONZE;
            case SILVER -> WEIGHT_SILVER;
            case GOLD -> WEIGHT_GOLD;
            case GOURMET -> WEIGHT_GOURMET;
            case BLACK -> WEIGHT_BLACK;
        };
    }

    private static BigDecimal influenceFactorPositiveBiasOnly(BigDecimal userMean, BigDecimal beta, BigDecimal minFactor) {
        if (userMean == null) {
            return BigDecimal.ONE;
        }
        BigDecimal bias = userMean.subtract(BASELINE_SCORE);
        if (bias.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        // factor = clamp(1 - beta*bias, minFactor, 1)
        BigDecimal factor = BigDecimal.ONE.subtract(beta.multiply(bias));
        if (factor.compareTo(minFactor) < 0) {
            factor = minFactor;
        }
        if (factor.compareTo(BigDecimal.ONE) > 0) {
            factor = BigDecimal.ONE;
        }
        return factor;
    }

    private static BigDecimal timeDecay(LocalDateTime createdAt, LocalDateTime now) {
        if (createdAt == null) {
            return BigDecimal.ONE;
        }
        if (createdAt.isAfter(now.minusMonths(6))) {
            return new BigDecimal("1.0");
        }
        if (createdAt.isAfter(now.minusYears(1))) {
            return new BigDecimal("0.8");
        }
        if (createdAt.isAfter(now.minusYears(2))) {
            return new BigDecimal("0.5");
        }
        if (createdAt.isAfter(now.minusYears(3))) {
            return new BigDecimal("0.2");
        }
        return new BigDecimal("0.1");
    }

    private static BigDecimal timeDecay(LocalDateTime createdAt, LocalDateTime now, BigDecimal decayFloor) {
        BigDecimal d = timeDecay(createdAt, now);
        if (decayFloor == null) {
            return d;
        }
        // floor 적용: d < floor 이면 floor로 올림
        if (d.compareTo(decayFloor) < 0) {
            return decayFloor;
        }
        return d;
    }
}
