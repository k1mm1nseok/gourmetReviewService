package com.gourmet.review.support.rating;

import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.support.rating.RatingSimulation.InfluenceConfig;
import com.gourmet.review.support.rating.RatingSimulation.ReviewSample;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시뮬레이션은 "정확한 분포"를 보장하기보다는, 정책 변경의 방향성을 빠르게 확인하는 목적.
 *
 * 출력은 테스트 로그로 확인 가능(개발자가 로컬에서 확인).
 */
@Tag("slow")
@Disabled("로컬에서만 수동 실행하는 느린 시뮬레이션 테스트(기본 mvn test 제외)")
class RatingSimulationTest {

    @Test
    void simulate_comparePolicies_underTwoDistributions() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 29, 12, 0);

        // Proposed 파라미터(초기 추천값 근처)
        InfluenceConfig config = new InfluenceConfig(new BigDecimal("0.35"), new BigDecimal("0.70"), 20);

        BigDecimal priorStrong = RatingSimulation.MIN_REVIEW_WEIGHT; // 30
        BigDecimal priorWeak = new BigDecimal("5.0");

        // 시나리오 A: 후한 문화(3.6~4.1 중심)
        runScenario("A) generous culture (3.6~4.1 중심)", now, config, priorStrong, priorWeak,
                /*stores*/ 200, /*users*/ 400, /*reviews/store*/ 20, 200,
                /*randomSeed*/ 42,
                DistributionProfile.GENEROUS_CULTURE);

        // 시나리오 B: 대부분 4점대 + 소수 1~2점대
        runScenario("B) mostly 4.x, small 1~2.x", now, config, priorStrong, priorWeak,
                /*stores*/ 200, /*users*/ 400, /*reviews/store*/ 20, 200,
                /*randomSeed*/ 43,
                DistributionProfile.MOSTLY_FOURS_WITH_LOW_OUTLIERS);
    }

    private static void runScenario(
            String scenarioName,
            LocalDateTime now,
            InfluenceConfig config,
            BigDecimal priorStrong,
            BigDecimal priorWeak,
            int stores,
            int users,
            int reviewsPerStoreMin,
            int reviewsPerStoreMax,
            long randomSeed,
            DistributionProfile profile
    ) {
        Random random = new Random(randomSeed);

        BigDecimal[] userMean = new BigDecimal[users];
        MemberTier[] userTier = new MemberTier[users];
        boolean[] userDeviationTarget = new boolean[users];

        for (int u = 0; u < users; u++) {
            double p = random.nextDouble();
            double mean;

            if (profile == DistributionProfile.GENEROUS_CULTURE) {
                if (p < 0.08) {
                    mean = 2.6 + random.nextDouble() * 0.4; // 박함 8%
                } else if (p < 0.88) {
                    mean = 3.6 + random.nextDouble() * 0.5; // 일반(후한 문화) 80%
                } else {
                    mean = 4.4 + random.nextDouble() * 0.5; // 매우 후함 12%
                }
            } else {
                // MOSTLY_FOURS_WITH_LOW_OUTLIERS
                if (p < 0.08) {
                    mean = 1.2 + random.nextDouble() * 1.0; // 1.2~2.2 (저점 소수 8%)
                } else {
                    mean = 4.1 + random.nextDouble() * 0.6; // 4.1~4.7 (대부분 92%)
                }
            }

            userMean[u] = new BigDecimal(String.format(java.util.Locale.US, "%.4f", mean))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            double t = random.nextDouble();
            if (t < 0.55) userTier[u] = MemberTier.SILVER;
            else if (t < 0.75) userTier[u] = MemberTier.BRONZE;
            else if (t < 0.93) userTier[u] = MemberTier.GOLD;
            else userTier[u] = MemberTier.GOURMET;

            userDeviationTarget[u] = random.nextDouble() < 0.12;
        }

        List<BigDecimal> currentScores = new ArrayList<>();
        List<BigDecimal> proposedScores = new ArrayList<>();
        List<BigDecimal> currentScoresWeakPrior = new ArrayList<>();
        List<BigDecimal> proposedScoresWeakPrior = new ArrayList<>();

        for (int s = 0; s < stores; s++) {
            int reviewCount = reviewsPerStoreMin + random.nextInt(reviewsPerStoreMax - reviewsPerStoreMin + 1);
            List<ReviewSample> reviews = new ArrayList<>(reviewCount);

            for (int i = 0; i < reviewCount; i++) {
                int u = random.nextInt(users);
                BigDecimal mean = userMean[u];

                // 노이즈(리뷰 변동성)는 시나리오별로 다르게 줘도 되지만, 먼저 동일하게 둔다.
                double noise = (random.nextGaussian()) * 0.75;
                double raw = mean.doubleValue() + noise;
                if (raw < 1.0) raw = 1.0;
                if (raw > 5.0) raw = 5.0;

                BigDecimal score = new BigDecimal(String.format(java.util.Locale.US, "%.4f", raw))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                int monthsAgo = random.nextInt(49);
                LocalDateTime createdAt = now.minusMonths(monthsAgo).minusDays(random.nextInt(30));

                reviews.add(new ReviewSample(
                        score,
                        userTier[u],
                        userDeviationTarget[u],
                        mean,
                        createdAt
                ));
            }

            currentScores.add(RatingSimulation.calculateStoreScoreCurrentWithPrior(reviews, now, priorStrong));
            proposedScores.add(RatingSimulation.calculateStoreScoreProposedWeightOnlyWithPrior(reviews, now, config, priorStrong));
            currentScoresWeakPrior.add(RatingSimulation.calculateStoreScoreCurrentWithPrior(reviews, now, priorWeak));
            proposedScoresWeakPrior.add(RatingSimulation.calculateStoreScoreProposedWeightOnlyWithPrior(reviews, now, config, priorWeak));
        }

        currentScores.sort(Comparator.naturalOrder());
        proposedScores.sort(Comparator.naturalOrder());
        currentScoresWeakPrior.sort(Comparator.naturalOrder());
        proposedScoresWeakPrior.sort(Comparator.naturalOrder());

        // Strong prior(30)
        BigDecimal currentMedian = percentile(currentScores, 0.50);
        BigDecimal proposedMedian = percentile(proposedScores, 0.50);
        BigDecimal currentP95 = percentile(currentScores, 0.95);
        BigDecimal proposedP95 = percentile(proposedScores, 0.95);
        BigDecimal currentP99 = percentile(currentScores, 0.99);
        BigDecimal proposedP99 = percentile(proposedScores, 0.99);

        long currentOver35 = currentScores.stream().filter(x -> x.compareTo(new BigDecimal("3.50")) >= 0).count();
        long proposedOver35 = proposedScores.stream().filter(x -> x.compareTo(new BigDecimal("3.50")) >= 0).count();
        long currentOver40 = currentScores.stream().filter(x -> x.compareTo(new BigDecimal("4.00")) >= 0).count();
        long proposedOver40 = proposedScores.stream().filter(x -> x.compareTo(new BigDecimal("4.00")) >= 0).count();

        // Weak prior(5)
        BigDecimal currentMedianWeak = percentile(currentScoresWeakPrior, 0.50);
        BigDecimal proposedMedianWeak = percentile(proposedScoresWeakPrior, 0.50);
        BigDecimal currentP95Weak = percentile(currentScoresWeakPrior, 0.95);
        BigDecimal proposedP95Weak = percentile(proposedScoresWeakPrior, 0.95);

        long currentOver35Weak = currentScoresWeakPrior.stream().filter(x -> x.compareTo(new BigDecimal("3.50")) >= 0).count();
        long proposedOver35Weak = proposedScoresWeakPrior.stream().filter(x -> x.compareTo(new BigDecimal("3.50")) >= 0).count();

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n=== Rating Simulation Summary ===\n");
        sb.append("Scenario: ").append(scenarioName).append("\n");
        sb.append("stores=").append(stores).append(", users=").append(users).append("\n");
        sb.append("Proposed config: beta=").append(config.beta()).append(", minFactor=").append(config.minFactor()).append("\n");

        sb.append("--- Strong prior(30) ---\n");
        sb.append("Current  median=").append(currentMedian).append(", p95=").append(currentP95).append(", p99=").append(currentP99)
                .append(", >=3.5=").append(ratio(currentOver35, stores))
                .append(", >=4.0=").append(ratio(currentOver40, stores)).append("\n");
        sb.append("Proposed median=").append(proposedMedian).append(", p95=").append(proposedP95).append(", p99=").append(proposedP99)
                .append(", >=3.5=").append(ratio(proposedOver35, stores))
                .append(", >=4.0=").append(ratio(proposedOver40, stores)).append("\n");

        sb.append("--- Weak prior(5) ---\n");
        sb.append("Current  median=").append(currentMedianWeak).append(", p95=").append(currentP95Weak)
                .append(", >=3.5=").append(ratio(currentOver35Weak, stores)).append("\n");
        sb.append("Proposed median=").append(proposedMedianWeak).append(", p95=").append(proposedP95Weak)
                .append(", >=3.5=").append(ratio(proposedOver35Weak, stores)).append("\n");

        System.out.print(sb);

        // 방향성 확인(상단 억제): rich 후기 문화에서 proposed가 상단을 낮추는지
        assertThat(proposedP95.doubleValue()).isLessThanOrEqualTo(currentP95.doubleValue() + 0.01);
        assertThat(proposedMedian.doubleValue()).isLessThanOrEqualTo(currentMedian.doubleValue() + 0.20);
    }

    @Test
    void simulate_gridSearch_betaMinFactor_forTop1to5PercentAt35() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 29, 12, 0);

        double targetMinB = 0.01;
        double targetMaxB = 0.05;
        double targetMinA = 0.01; // A는 0%로 죽지 않게 최소 1%

        int stores = 400;
        int users = 800;
        int reviewsPerStoreMin = 20;
        int reviewsPerStoreMax = 300;

        // Focused: prior는 30 고정(타베로그처럼 4.0 희귀 유지 목적)
        BigDecimal prior = new BigDecimal("30");

        BigDecimal[] decayFloors = {
                new BigDecimal("0.20"), new BigDecimal("0.25"), new BigDecimal("0.30"), new BigDecimal("0.35"),
                new BigDecimal("0.40"), new BigDecimal("0.45"), new BigDecimal("0.50"), new BigDecimal("0.55"), new BigDecimal("0.60")
        };
        BigDecimal[] betas = {
                new BigDecimal("0.60"), new BigDecimal("0.70"), new BigDecimal("0.80"), new BigDecimal("0.90"),
                new BigDecimal("1.00"), new BigDecimal("1.10"), new BigDecimal("1.20")
        };
        BigDecimal[] minFactors = {
                new BigDecimal("0.20"), new BigDecimal("0.25"), new BigDecimal("0.30"), new BigDecimal("0.35"),
                new BigDecimal("0.40"), new BigDecimal("0.45"), new BigDecimal("0.50"), new BigDecimal("0.60")
        };

        ScenarioData scenarioA = buildScenario(now, stores, users, reviewsPerStoreMin, reviewsPerStoreMax, 42L, DistributionProfile.GENEROUS_CULTURE);
        ScenarioData scenarioB = buildScenario(now, stores, users, reviewsPerStoreMin, reviewsPerStoreMax, 43L, DistributionProfile.MOSTLY_FOURS_WITH_LOW_OUTLIERS);

        StringBuilder report = new StringBuilder();
        report.append("=== Rating policy focused search (prior=30 fixed) ===\n");
        report.append("Constraints:\n");
        report.append("- Scenario B: Proposed >=3.5 ratio in [1%, 5%]\n");
        report.append("- Scenario A: Proposed >=3.5 ratio >= 1% (avoid dead upper tail)\n");
        report.append("stores=").append(stores).append(", users=").append(users)
                .append(", reviewsPerStore=").append(reviewsPerStoreMin).append("~").append(reviewsPerStoreMax).append("\n\n");

        Metrics curA = evaluateCurrent(scenarioA, now, prior, new BigDecimal("0.10"));
        Metrics curB = evaluateCurrent(scenarioB, now, prior, new BigDecimal("0.10"));
        report.append("-- Current baseline (prior=30, floor=0.10) --\n");
        report.append("A: ").append(curA.oneLine()).append("\n");
        report.append("B: ").append(curB.oneLine()).append("\n\n");

        record Candidate(BigDecimal floor, BigDecimal beta, BigDecimal minFactor, Metrics a, Metrics b, double minA, double bCenter) {
            double score() {
                double bDist = Math.abs(b.over35Ratio() - bCenter);
                double aDist = Math.max(0.0, minA - a.over35Ratio());
                return bDist + aDist * 2.0;
            }
        }

        List<Candidate> matches = new ArrayList<>();

        for (BigDecimal floor : decayFloors) {
            for (BigDecimal beta : betas) {
                for (BigDecimal minFactor : minFactors) {
                    InfluenceConfig cfg = new InfluenceConfig(beta, minFactor, 20);
                    Metrics propA = evaluateProposed(scenarioA, now, prior, floor, cfg);
                    Metrics propB = evaluateProposed(scenarioB, now, prior, floor, cfg);

                    double aOver35 = propA.over35Ratio();
                    double bOver35 = propB.over35Ratio();

                    if (aOver35 >= targetMinA && bOver35 >= targetMinB && bOver35 <= targetMaxB) {
                        matches.add(new Candidate(floor, beta, minFactor, propA, propB, targetMinA, 0.03));
                    }
                }
            }
        }

        matches.sort(Comparator.comparingDouble(Candidate::score));

        report.append("-- Matches (sorted, top 20) --\n");
        int limit = Math.min(20, matches.size());
        for (int i = 0; i < limit; i++) {
            Candidate c = matches.get(i);
            report.append(String.format(java.util.Locale.US,
                    "#%02d floor=%s beta=%s minFactor=%s | A: %s | B: %s\n",
                    i + 1,
                    c.floor().toPlainString(), c.beta().toPlainString(), c.minFactor().toPlainString(),
                    c.a().oneLine(), c.b().oneLine()));
        }
        if (matches.isEmpty()) {
            report.append("(no matches) -> either relax Scenario A constraint or expand beta/minFactor further.\n");
        }

        // 전체 매칭도 별도 섹션으로 남김(파일 저장용)
        report.append("\n-- All matches --\n");
        for (Candidate c : matches) {
            report.append(String.format(java.util.Locale.US,
                    "floor=%s beta=%s minFactor=%s | A>=3.5=%.2f%% | B>=3.5=%.2f%% | A.p95=%s B.p95=%s\n",
                    c.floor().toPlainString(), c.beta().toPlainString(), c.minFactor().toPlainString(),
                    c.a().over35Ratio() * 100, c.b().over35Ratio() * 100,
                    c.a().p95, c.b().p95));
        }

        writeReport(report);

        // Sanity: 기본 세팅은 상단 억제 방향
        InfluenceConfig defaultCfg = new InfluenceConfig(new BigDecimal("0.35"), new BigDecimal("0.70"), 20);
        Metrics curBDefault = evaluateCurrent(scenarioB, now, RatingSimulation.MIN_REVIEW_WEIGHT, new BigDecimal("0.10"));
        Metrics propBDefault = evaluateProposed(scenarioB, now, RatingSimulation.MIN_REVIEW_WEIGHT, new BigDecimal("0.10"), defaultCfg);
        assertThat(propBDefault.p95.doubleValue()).isLessThanOrEqualTo(curBDefault.p95.doubleValue() + 0.01);
    }

    @Test
    void simulate_highReviewCount_checkOver40Emergence() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 29, 12, 0);

        int stores = 200;
        int users = 1200;
        int reviewsPerStoreMin = 5;
        int reviewsPerStoreMax = 800;

        BigDecimal prior = new BigDecimal("30");
        BigDecimal floor = new BigDecimal("0.30");
        InfluenceConfig cfg = new InfluenceConfig(new BigDecimal("0.60"), new BigDecimal("0.20"), 20);

        // quality 없는 기본 분포로 stress test
        ScenarioData scenarioA = buildScenario(now, stores, users, reviewsPerStoreMin, reviewsPerStoreMax, 142L, DistributionProfile.GENEROUS_CULTURE);
        ScenarioData scenarioB = buildScenario(now, stores, users, reviewsPerStoreMin, reviewsPerStoreMax, 143L, DistributionProfile.MOSTLY_FOURS_WITH_LOW_OUTLIERS);

        // 점수와 함께 리뷰 수 추적
        var curA = evaluateWithReviewCountsCurrent(scenarioA, now, prior, floor);
        var curB = evaluateWithReviewCountsCurrent(scenarioB, now, prior, floor);
        var propA = evaluateWithReviewCountsProposed(scenarioA, now, prior, floor, cfg);
        var propB = evaluateWithReviewCountsProposed(scenarioB, now, prior, floor, cfg);

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n=== High review-count stress test ===\n");
        sb.append("stores=").append(stores).append(", users=").append(users)
                .append(", reviews/store=").append(reviewsPerStoreMin).append("~").append(reviewsPerStoreMax).append("\n");
        sb.append("params: prior=").append(prior).append(", floor=").append(floor)
                .append(", beta=").append(cfg.beta()).append(", minFactor=").append(cfg.minFactor()).append("\n");

        sb.append("Current A: ").append(curA.metrics().oneLine()).append("\n");
        sb.append("Current B: ").append(curB.metrics().oneLine()).append("\n");
        sb.append("Prop    A: ").append(propA.metrics().oneLine()).append("\n");
        sb.append("Prop    B: ").append(propB.metrics().oneLine()).append("\n\n");

        sb.append("-- Over 4.0 stores (review counts) --\n");
        sb.append("Current B (>=4.0): ").append(summarizeOver40(curB)).append("\n");
        sb.append("Current A (>=4.0): ").append(summarizeOver40(curA)).append("\n");
        sb.append("Proposed B (>=4.0): ").append(summarizeOver40(propB)).append("\n");
        sb.append("Proposed A (>=4.0): ").append(summarizeOver40(propA)).append("\n");

        System.out.print(sb);

        // B 환경에서는 리뷰가 충분히 많아지면 Current 기준 4.0 이상이 일부 나타나는지 관찰(없어도 테스트 실패는 안 시킴)
        assertThat(curB.metrics().over40Count).isGreaterThanOrEqualTo(0);
    }

    private record EvaluationWithCounts(Metrics metrics, List<Integer> reviewCountsPerStore, List<BigDecimal> scoresPerStoreSorted, List<Integer> countsAlignedToSortedScores) {}

    private static EvaluationWithCounts evaluateWithReviewCountsCurrent(ScenarioData scenario, LocalDateTime now, BigDecimal prior, BigDecimal decayFloor) {
        List<BigDecimal> scores = new ArrayList<>(scenario.storeReviews().size());
        List<Integer> counts = new ArrayList<>(scenario.storeReviews().size());

        for (List<ReviewSample> store : scenario.storeReviews()) {
            scores.add(RatingSimulation.calculateStoreScoreCurrentWithPriorAndDecayFloor(store, now, prior, decayFloor));
            counts.add(store.size());
        }

        // scores 기준 정렬하면서 counts도 같이 정렬
        List<Integer> idx = new ArrayList<>(scores.size());
        for (int i = 0; i < scores.size(); i++) idx.add(i);
        idx.sort(Comparator.comparing(scores::get));

        List<BigDecimal> sortedScores = new ArrayList<>(scores.size());
        List<Integer> sortedCounts = new ArrayList<>(scores.size());
        for (int i : idx) {
            sortedScores.add(scores.get(i));
            sortedCounts.add(counts.get(i));
        }

        Metrics m = toMetrics(sortedScores);
        return new EvaluationWithCounts(m, counts, sortedScores, sortedCounts);
    }

    private static EvaluationWithCounts evaluateWithReviewCountsProposed(ScenarioData scenario, LocalDateTime now, BigDecimal prior, BigDecimal decayFloor, InfluenceConfig cfg) {
        List<BigDecimal> scores = new ArrayList<>(scenario.storeReviews().size());
        List<Integer> counts = new ArrayList<>(scenario.storeReviews().size());

        for (List<ReviewSample> store : scenario.storeReviews()) {
            scores.add(RatingSimulation.calculateStoreScoreProposedWeightOnlyWithPriorAndDecayFloor(store, now, cfg, prior, decayFloor));
            counts.add(store.size());
        }

        List<Integer> idx = new ArrayList<>(scores.size());
        for (int i = 0; i < scores.size(); i++) idx.add(i);
        idx.sort(Comparator.comparing(scores::get));

        List<BigDecimal> sortedScores = new ArrayList<>(scores.size());
        List<Integer> sortedCounts = new ArrayList<>(scores.size());
        for (int i : idx) {
            sortedScores.add(scores.get(i));
            sortedCounts.add(counts.get(i));
        }

        Metrics m = toMetrics(sortedScores);
        return new EvaluationWithCounts(m, counts, sortedScores, sortedCounts);
    }

    private static String summarizeOver40(EvaluationWithCounts eval) {
        int stores = eval.scoresPerStoreSorted().size();
        if (stores == 0) return "n/a";

        List<Integer> overCounts = new ArrayList<>();
        for (int i = 0; i < stores; i++) {
            if (eval.scoresPerStoreSorted().get(i).compareTo(new BigDecimal("4.00")) >= 0) {
                overCounts.add(eval.countsAlignedToSortedScores().get(i));
            }
        }

        if (overCounts.isEmpty()) {
            return "0 stores";
        }

        overCounts.sort(Integer::compareTo);
        int min = overCounts.getFirst();
        int max = overCounts.getLast();
        int median = overCounts.get(overCounts.size() / 2);

        StringBuilder sb = new StringBuilder();
        sb.append(overCounts.size()).append(" stores; reviewCount min/median/max=")
                .append(min).append("/").append(median).append("/").append(max);

        // 샘플로 상위 10개(리뷰수 많은 순)도 같이 출력
        List<Integer> top = new ArrayList<>(overCounts);
        top.sort(Comparator.reverseOrder());
        int limit = Math.min(10, top.size());
        sb.append("; topCounts=");
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(",");
            sb.append(top.get(i));
        }

        return sb.toString();
    }

    // ====== 아래는 테스트 내부 유틸 (DB 없이 재사용) ======

    private enum DistributionProfile {
        GENEROUS_CULTURE,
        MOSTLY_FOURS_WITH_LOW_OUTLIERS
    }

    private record ScenarioData(List<List<ReviewSample>> storeReviews) {}

    private static ScenarioData buildScenario(
            LocalDateTime now,
            int stores,
            int users,
            int reviewsPerStoreMin,
            int reviewsPerStoreMax,
            long seed,
            DistributionProfile profile
    ) {
        Random random = new Random(seed);

        BigDecimal[] userMean = new BigDecimal[users];
        MemberTier[] userTier = new MemberTier[users];
        boolean[] userDeviationTarget = new boolean[users];

        for (int u = 0; u < users; u++) {
            double p = random.nextDouble();
            double mean;

            if (profile == DistributionProfile.GENEROUS_CULTURE) {
                if (p < 0.08) mean = 2.6 + random.nextDouble() * 0.4;
                else if (p < 0.88) mean = 3.6 + random.nextDouble() * 0.5;
                else mean = 4.4 + random.nextDouble() * 0.5;
            } else {
                if (p < 0.08) mean = 1.2 + random.nextDouble(); // 1.2~2.2
                else mean = 4.1 + random.nextDouble() * 0.6; // 4.1~4.7
            }

            userMean[u] = new BigDecimal(String.format(java.util.Locale.US, "%.4f", mean))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            double t = random.nextDouble();
            if (t < 0.55) userTier[u] = MemberTier.SILVER;
            else if (t < 0.75) userTier[u] = MemberTier.BRONZE;
            else if (t < 0.93) userTier[u] = MemberTier.GOLD;
            else userTier[u] = MemberTier.GOURMET;

            userDeviationTarget[u] = random.nextDouble() < 0.12;
        }

        List<List<ReviewSample>> allStores = new ArrayList<>(stores);

        for (int s = 0; s < stores; s++) {
            int reviewCount = reviewsPerStoreMin + random.nextInt(reviewsPerStoreMax - reviewsPerStoreMin + 1);
            List<ReviewSample> reviews = new ArrayList<>(reviewCount);

            for (int i = 0; i < reviewCount; i++) {
                int u = random.nextInt(users);
                BigDecimal mean = userMean[u];

                double noise = random.nextGaussian() * 0.75;
                double raw = mean.doubleValue() + noise;
                if (raw < 1.0) raw = 1.0;
                if (raw > 5.0) raw = 5.0;

                BigDecimal score = new BigDecimal(String.format(java.util.Locale.US, "%.4f", raw))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                int monthsAgo = random.nextInt(49);
                LocalDateTime createdAt = now.minusMonths(monthsAgo).minusDays(random.nextInt(30));

                reviews.add(new ReviewSample(score, userTier[u], userDeviationTarget[u], mean, createdAt));
            }

            allStores.add(reviews);
        }

        return new ScenarioData(allStores);
    }

    private record Metrics(
            BigDecimal median,
            BigDecimal p95,
            BigDecimal p99,
            long over35Count,
            long over40Count,
            int stores
    ) {
        double over35Ratio() { return stores == 0 ? 0.0 : ((double) over35Count) / stores; }
        double over40Ratio() { return stores == 0 ? 0.0 : ((double) over40Count) / stores; }

        String oneLine() {
            return String.format(java.util.Locale.US,
                    "median=%s, p95=%s, p99=%s, >=3.5=%s, >=4.0=%s",
                    median, p95, p99,
                    String.format(java.util.Locale.US, "%.2f%%", over35Ratio() * 100),
                    String.format(java.util.Locale.US, "%.2f%%", over40Ratio() * 100));
        }
    }

    private static Metrics evaluateCurrent(ScenarioData scenario, LocalDateTime now, BigDecimal prior, BigDecimal decayFloor) {
        List<BigDecimal> scores = new ArrayList<>(scenario.storeReviews().size());
        for (List<ReviewSample> store : scenario.storeReviews()) {
            scores.add(RatingSimulation.calculateStoreScoreCurrentWithPriorAndDecayFloor(store, now, prior, decayFloor));
        }
        scores.sort(Comparator.naturalOrder());
        return toMetrics(scores);
    }

    private static Metrics evaluateProposed(ScenarioData scenario, LocalDateTime now, BigDecimal prior, BigDecimal decayFloor, InfluenceConfig cfg) {
        List<BigDecimal> scores = new ArrayList<>(scenario.storeReviews().size());
        for (List<ReviewSample> store : scenario.storeReviews()) {
            scores.add(RatingSimulation.calculateStoreScoreProposedWeightOnlyWithPriorAndDecayFloor(store, now, cfg, prior, decayFloor));
        }
        scores.sort(Comparator.naturalOrder());
        return toMetrics(scores);
    }

    private static Metrics toMetrics(List<BigDecimal> sortedScores) {
        int stores = sortedScores.size();
        BigDecimal median = percentile(sortedScores, 0.50);
        BigDecimal p95 = percentile(sortedScores, 0.95);
        BigDecimal p99 = percentile(sortedScores, 0.99);

        long over35 = sortedScores.stream().filter(x -> x.compareTo(new BigDecimal("3.50")) >= 0).count();
        long over40 = sortedScores.stream().filter(x -> x.compareTo(new BigDecimal("4.00")) >= 0).count();

        return new Metrics(median, p95, p99, over35, over40, stores);
    }

    private static BigDecimal percentile(List<BigDecimal> sorted, double p) {
        if (sorted.isEmpty()) return BigDecimal.ZERO;
        int idx = (int) Math.floor(p * (sorted.size() - 1));
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    private static String ratio(long count, long total) {
        double r = total == 0 ? 0.0 : ((double) count) / ((double) total);
        return String.format(java.util.Locale.US, "%.2f%%", r * 100);
    }

    private static void writeReport(StringBuilder report) {
        try {
            Path dir = Path.of("target", "simulation");
            Files.createDirectories(dir);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path out = dir.resolve("rating-grid-search-" + ts + ".txt");
            Files.writeString(out, report.toString(), StandardCharsets.UTF_8);

            System.out.println("[rating-sim] wrote report: " + out.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[rating-sim] failed to write report: " + e.getMessage());
        }
    }

    @Test
    void simulate_focusedSearch_storeQualityModel_targetTabelogTail() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 29, 12, 0);

        // 목표(타베로그 타겟)
        double bOver35Min = 0.03;
        double bOver35Max = 0.05;
        double bOver40Min = 0.001; // 0.1%
        double bOver40Max = 0.002; // 0.2%

        int stores = 5000;
        int users = 6000;

        int reviewsPerStoreMin = 5;
        int baseReviewsPerStoreMax = 250;
        int premiumReviewsPerStoreMax = 900;

        BigDecimal prior = new BigDecimal("30");

        BigDecimal[] decayFloors = {
                new BigDecimal("0.05"), new BigDecimal("0.10"), new BigDecimal("0.20"), new BigDecimal("0.30"),
                new BigDecimal("0.40"), new BigDecimal("0.50")
        };

        // 좋은 가게 풀(상위 극소수)
        StoreQualityConfig qCfgA = new StoreQualityConfig(0.003, new BigDecimal("0.05"), new BigDecimal("0.20"));
        StoreQualityConfig qCfgB = new StoreQualityConfig(0.003, new BigDecimal("0.45"), new BigDecimal("0.85"));

        ScenarioData scenarioA = buildScenarioWithStoreQuality(
                now, stores, users,
                reviewsPerStoreMin, baseReviewsPerStoreMax, premiumReviewsPerStoreMax,
                4242L, DistributionProfile.GENEROUS_CULTURE, qCfgA
        );
        ScenarioData scenarioB = buildScenarioWithStoreQuality(
                now, stores, users,
                reviewsPerStoreMin, baseReviewsPerStoreMax, premiumReviewsPerStoreMax,
                4343L, DistributionProfile.MOSTLY_FOURS_WITH_LOW_OUTLIERS, qCfgB
        );

        record Candidate(BigDecimal floor, BigDecimal beta, BigDecimal minFactor,
                         double aOver35, double bOver35, double bOver40,
                         String bOver40CountsSummary,
                         Metrics aMetrics, Metrics bMetrics) {
            double score() {
                // 목표 중심: B.over35 -> 4%, B.over40 -> 0.15%
                double s1 = Math.abs(bOver35 - 0.04);
                double s2 = Math.abs(bOver40 - 0.0015);
                double aPenalty = aOver35 < 0.01 ? (0.01 - aOver35) * 10.0 : 0.0;
                return s1 + s2 * 10.0 + aPenalty;
            }
        }

        // (1) Coarse: 랜덤 샘플링(빠르게 전체 공간을 훑음)
        Random searchRandom = new Random(20251229L);
        int coarseSamples = 300; // 실행 시간과 정밀도 트레이드오프

        List<Candidate> allCoarse = new ArrayList<>();
        List<Candidate> matches = new ArrayList<>();

        for (int i = 0; i < coarseSamples; i++) {
            BigDecimal floor = decayFloors[searchRandom.nextInt(decayFloors.length)];

            // beta는 0~1.2 연속
            double betaD = searchRandom.nextDouble() * 1.2;
            BigDecimal beta = new BigDecimal(String.format(java.util.Locale.US, "%.3f", betaD));

            // minFactor는 0.10~1.00
            double minD = 0.10 + searchRandom.nextDouble() * 0.90;
            BigDecimal minFactor = new BigDecimal(String.format(java.util.Locale.US, "%.3f", minD));

            InfluenceConfig cfg = new InfluenceConfig(beta, minFactor, 20);

            Metrics propA = evaluateProposed(scenarioA, now, prior, floor, cfg);
            Metrics propB = evaluateProposed(scenarioB, now, prior, floor, cfg);

            var evalB = evaluateWithReviewCountsProposed(scenarioB, now, prior, floor, cfg);
            String over40CountsSummary = summarizeOver40(evalB);

            Candidate c = new Candidate(
                    floor, beta, minFactor,
                    propA.over35Ratio(), propB.over35Ratio(), propB.over40Ratio(),
                    over40CountsSummary,
                    propA, propB
            );

            allCoarse.add(c);

            if (c.bOver35() >= bOver35Min && c.bOver35() <= bOver35Max
                    && c.bOver40() >= bOver40Min && c.bOver40() <= bOver40Max
                    && c.aOver35() >= 0.01) {
                matches.add(c);
            }
        }

        allCoarse.sort(Comparator.comparingDouble(Candidate::score));
        matches.sort(Comparator.comparingDouble(Candidate::score));

        // (2) Fine: Coarse 상위 후보 주변을 작은 grid로 정밀 탐색
        int fineSeeds = Math.min(10, allCoarse.size());
        List<Candidate> fineMatches = new ArrayList<>();
        List<Candidate> fineNear = new ArrayList<>();

        double[] betaOffsets = {-0.20, -0.10, -0.05, 0.0, 0.05, 0.10, 0.20};
        double[] minOffsets = {-0.20, -0.10, -0.05, 0.0, 0.05, 0.10, 0.20};

        for (int k = 0; k < fineSeeds; k++) {
            Candidate seed = allCoarse.get(k);
            BigDecimal floor = seed.floor();

            for (double bo : betaOffsets) {
                for (double mo : minOffsets) {
                    double betaD = seed.beta().doubleValue() + bo;
                    if (betaD < 0.0) betaD = 0.0;
                    if (betaD > 1.2) betaD = 1.2;

                    double minD = seed.minFactor().doubleValue() + mo;
                    if (minD < 0.10) minD = 0.10;
                    if (minD > 1.00) minD = 1.00;

                    BigDecimal beta = new BigDecimal(String.format(java.util.Locale.US, "%.3f", betaD));
                    BigDecimal minFactor = new BigDecimal(String.format(java.util.Locale.US, "%.3f", minD));

                    InfluenceConfig cfg = new InfluenceConfig(beta, minFactor, 20);

                    Metrics propA = evaluateProposed(scenarioA, now, prior, floor, cfg);
                    Metrics propB = evaluateProposed(scenarioB, now, prior, floor, cfg);

                    var evalB = evaluateWithReviewCountsProposed(scenarioB, now, prior, floor, cfg);
                    String over40CountsSummary = summarizeOver40(evalB);

                    Candidate c = new Candidate(
                            floor, beta, minFactor,
                            propA.over35Ratio(), propB.over35Ratio(), propB.over40Ratio(),
                            over40CountsSummary,
                            propA, propB
                    );

                    fineNear.add(c);

                    if (c.bOver35() >= bOver35Min && c.bOver35() <= bOver35Max
                            && c.bOver40() >= bOver40Min && c.bOver40() <= bOver40Max
                            && c.aOver35() >= 0.01) {
                        fineMatches.add(c);
                    }
                }
            }
        }

        fineNear.sort(Comparator.comparingDouble(Candidate::score));
        fineMatches.sort(Comparator.comparingDouble(Candidate::score));

        StringBuilder report = new StringBuilder();
        report.append("=== Focused search (storeQuality model, stores=5000; 2-stage search) ===\n");
        report.append("Target(B): >=3.5 3~5%, >=4.0 0.1~0.2%\n");
        report.append("prior=").append(prior).append(", coarseSamples=").append(coarseSamples).append("\n");
        report.append("decayFloors=").append(List.of(decayFloors)).append("\n");
        report.append("reviews/store min=").append(reviewsPerStoreMin)
                .append(", baseMax=").append(baseReviewsPerStoreMax)
                .append(", premiumMax=").append(premiumReviewsPerStoreMax)
                .append("\n");
        report.append("StoreQuality(B): premiumRatio=").append(qCfgB.premiumRatio())
                .append(", boost=").append(qCfgB.boostMin()).append("~").append(qCfgB.boostMax()).append("\n\n");

        // Baseline 진단
        report.append("-- Baseline (beta=0, minFactor=1.0) by decayFloor --\n");
        InfluenceConfig baselineCfg = new InfluenceConfig(new BigDecimal("0.00"), new BigDecimal("1.00"), 20);
        for (BigDecimal floor : decayFloors) {
            Metrics b = evaluateProposed(scenarioB, now, prior, floor, baselineCfg);
            report.append("floor=").append(floor).append(" => ").append(b.oneLine()).append("\n");
        }
        report.append("\n");

        report.append("-- Coarse best (top 20) --\n");
        for (int i = 0; i < Math.min(20, allCoarse.size()); i++) {
            Candidate c = allCoarse.get(i);
            report.append(String.format(java.util.Locale.US,
                    "#%02d floor=%s beta=%s minFactor=%s | B>=3.5=%.2f%% B>=4.0=%.3f%% | score=%.6f\n",
                    i + 1, c.floor(), c.beta(), c.minFactor(), c.bOver35() * 100, c.bOver40() * 100, c.score()));
        }
        report.append("\n");

        report.append("-- Coarse matches (top 20) --\n");
        for (int i = 0; i < Math.min(20, matches.size()); i++) {
            Candidate c = matches.get(i);
            report.append(String.format(java.util.Locale.US,
                    "#%02d floor=%s beta=%s minFactor=%s | A>=3.5=%.2f%% | B>=3.5=%.2f%% B>=4.0=%.3f%% | B>=4.0 reviewCounts: %s\n",
                    i + 1, c.floor(), c.beta(), c.minFactor(), c.aOver35() * 100, c.bOver35() * 100, c.bOver40() * 100, c.bOver40CountsSummary()));
        }
        report.append("\n");

        report.append("-- Fine best (top 20) --\n");
        for (int i = 0; i < Math.min(20, fineNear.size()); i++) {
            Candidate c = fineNear.get(i);
            report.append(String.format(java.util.Locale.US,
                    "#%02d floor=%s beta=%s minFactor=%s | B>=3.5=%.2f%% B>=4.0=%.3f%% | score=%.6f\n",
                    i + 1, c.floor(), c.beta(), c.minFactor(), c.bOver35() * 100, c.bOver40() * 100, c.score()));
        }
        report.append("\n");

        report.append("-- Fine matches (top 30) --\n");
        for (int i = 0; i < Math.min(30, fineMatches.size()); i++) {
            Candidate c = fineMatches.get(i);
            report.append(String.format(java.util.Locale.US,
                    "#%02d floor=%s beta=%s minFactor=%s | A>=3.5=%.2f%%(p95=%s) | B>=3.5=%.2f%% B>=4.0=%.3f%% (p95=%s p99=%s) | B>=4.0 reviewCounts: %s\n",
                    i + 1,
                    c.floor(), c.beta(), c.minFactor(),
                    c.aOver35() * 100, c.aMetrics().p95,
                    c.bOver35() * 100, c.bOver40() * 100,
                    c.bMetrics().p95, c.bMetrics().p99,
                    c.bOver40CountsSummary()));
        }
        if (fineMatches.isEmpty()) {
            report.append("(no fine matches) -> quality 모델에서라도 4.0 꼬리가 너무 많이 눌리거나, 3.5가 너무 높/낮을 수 있습니다. Baseline과 best candidates를 보고 premiumRatio/boost를 조정하세요.\n");
        }

        writeReport(report);

        assertThat(true).isTrue();
    }

    // NOTE: 기존 simulate_focusedSearch_withOver40TailInHighReviewStores()는 유지(quality 없는 모델). 혼동을 줄이기 위해 이 테스트는 별도 메서드로 분리한다.

    private record StoreQualityConfig(double premiumRatio, BigDecimal boostMin, BigDecimal boostMax) {}

    private static ScenarioData buildScenarioWithStoreQuality(
            LocalDateTime now,
            int stores,
            int users,
            int reviewsPerStoreMin,
            int baseReviewsPerStoreMax,
            int premiumReviewsPerStoreMax,
            long seed,
            DistributionProfile profile,
            StoreQualityConfig qCfg
    ) {
        Random random = new Random(seed);

        // 유저 분포는 기존 buildScenario와 동일
        BigDecimal[] userMean = new BigDecimal[users];
        MemberTier[] userTier = new MemberTier[users];
        boolean[] userDeviationTarget = new boolean[users];

        for (int u = 0; u < users; u++) {
            double p = random.nextDouble();
            double mean;

            if (profile == DistributionProfile.GENEROUS_CULTURE) {
                if (p < 0.08) mean = 2.6 + random.nextDouble() * 0.4;
                else if (p < 0.88) mean = 3.6 + random.nextDouble() * 0.5;
                else mean = 4.4 + random.nextDouble() * 0.5;
            } else {
                if (p < 0.08) mean = 1.2 + random.nextDouble();
                else mean = 4.1 + random.nextDouble() * 0.6;
            }

            userMean[u] = new BigDecimal(String.format(java.util.Locale.US, "%.4f", mean))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            double t = random.nextDouble();
            if (t < 0.55) userTier[u] = MemberTier.SILVER;
            else if (t < 0.75) userTier[u] = MemberTier.BRONZE;
            else if (t < 0.93) userTier[u] = MemberTier.GOLD;
            else userTier[u] = MemberTier.GOURMET;

            userDeviationTarget[u] = random.nextDouble() < 0.12;
        }

        List<List<ReviewSample>> allStores = new ArrayList<>(stores);

        for (int s = 0; s < stores; s++) {
            boolean isPremium = random.nextDouble() < qCfg.premiumRatio();
            BigDecimal storeBoost = BigDecimal.ZERO;
            int maxReviews = baseReviewsPerStoreMax;

            if (isPremium) {
                maxReviews = premiumReviewsPerStoreMax;
                double boost = qCfg.boostMin().doubleValue() + random.nextDouble() * (qCfg.boostMax().doubleValue() - qCfg.boostMin().doubleValue());
                storeBoost = new BigDecimal(String.format(java.util.Locale.US, "%.4f", boost))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }

            int reviewCount = reviewsPerStoreMin + random.nextInt(Math.max(1, maxReviews - reviewsPerStoreMin + 1));
            List<ReviewSample> reviews = new ArrayList<>(reviewCount);

            for (int i = 0; i < reviewCount; i++) {
                int u = random.nextInt(users);
                BigDecimal mean = userMean[u];

                // 좋은 가게에서는 4~5로 몰리게 하려면 노이즈는 조금 줄이는 편이 안정적
                double noiseSigma = isPremium ? 0.55 : 0.75;
                double noise = random.nextGaussian() * noiseSigma;

                double raw = mean.doubleValue() + storeBoost.doubleValue() + noise;
                if (raw < 1.0) raw = 1.0;
                if (raw > 5.0) raw = 5.0;

                BigDecimal score = new BigDecimal(String.format(java.util.Locale.US, "%.4f", raw))
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                int monthsAgo = random.nextInt(49);
                LocalDateTime createdAt = now.minusMonths(monthsAgo).minusDays(random.nextInt(30));

                reviews.add(new ReviewSample(score, userTier[u], userDeviationTarget[u], mean, createdAt));
            }

            allStores.add(reviews);
        }

        return new ScenarioData(allStores);
    }

    // 기존 호출 호환용 오버로드(quality 모델 기본값)
    private static ScenarioData buildScenarioWithStoreQuality(
            LocalDateTime now,
            int stores,
            int users,
            int reviewsPerStoreMin,
            int baseReviewsPerStoreMax,
            int premiumReviewsPerStoreMax,
            long seed,
            DistributionProfile profile
    ) {
        // 기본: premium 없음(qualityBoost 0) -> 사실상 buildScenario와 비슷하지만, 리뷰수 상한만 분리 가능
        StoreQualityConfig defaultCfg = new StoreQualityConfig(0.0, BigDecimal.ZERO, BigDecimal.ZERO);
        return buildScenarioWithStoreQuality(
                now,
                stores,
                users,
                reviewsPerStoreMin,
                baseReviewsPerStoreMax,
                premiumReviewsPerStoreMax,
                seed,
                profile,
                defaultCfg
        );
    }

    @Test
    void simulate_betaSweep_fixedFloor050_minFactor0245() {
        LocalDateTime now = LocalDateTime.of(2025, 12, 29, 12, 0);

        int stores = 5000;
        int users = 6000;

        int reviewsPerStoreMin = 5;
        int baseReviewsPerStoreMax = 250;
        int premiumReviewsPerStoreMax = 900;

        BigDecimal prior = new BigDecimal("30");

        // 요청: floor=0.50 고정
        BigDecimal floor = new BigDecimal("0.50");

        // 요청: minFactor=0.245 고정
        BigDecimal minFactor = new BigDecimal("0.245");

        // beta sweep: 0.55 ~ 0.85 (0.01 step)
        BigDecimal betaMin = new BigDecimal("0.55");
        BigDecimal betaMax = new BigDecimal("0.85");
        BigDecimal betaStep = new BigDecimal("0.01");

        // 좋은 가게 모델은 기존과 동일(재현성 확보)
        StoreQualityConfig qCfgA = new StoreQualityConfig(0.003, new BigDecimal("0.05"), new BigDecimal("0.20"));
        StoreQualityConfig qCfgB = new StoreQualityConfig(0.003, new BigDecimal("0.45"), new BigDecimal("0.85"));

        ScenarioData scenarioA = buildScenarioWithStoreQuality(
                now, stores, users,
                reviewsPerStoreMin, baseReviewsPerStoreMax, premiumReviewsPerStoreMax,
                4242L, DistributionProfile.GENEROUS_CULTURE, qCfgA
        );
        ScenarioData scenarioB = buildScenarioWithStoreQuality(
                now, stores, users,
                reviewsPerStoreMin, baseReviewsPerStoreMax, premiumReviewsPerStoreMax,
                4343L, DistributionProfile.MOSTLY_FOURS_WITH_LOW_OUTLIERS, qCfgB
        );

        record Row(BigDecimal beta, Metrics a, Metrics b) {}
        List<Row> rows = new ArrayList<>();

        for (BigDecimal beta = betaMin; beta.compareTo(betaMax) <= 0; beta = beta.add(betaStep)) {
            InfluenceConfig cfg = new InfluenceConfig(beta, minFactor, 20);
            Metrics a = evaluateProposed(scenarioA, now, prior, floor, cfg);
            Metrics b = evaluateProposed(scenarioB, now, prior, floor, cfg);
            rows.add(new Row(beta, a, b));
        }

        // 정렬 기준(2번 우선순위): B에서 >=3.5가 4%에 가까운 순
        rows.sort(Comparator.comparingDouble(r -> Math.abs(r.b().over35Ratio() - 0.04)));

        StringBuilder report = new StringBuilder();
        report.append("=== Beta sweep (fixed floor=0.50, minFactor=0.245) ===\n");
        report.append("stores=").append(stores).append(", users=").append(users)
                .append(", reviews/store=").append(reviewsPerStoreMin).append("~").append(premiumReviewsPerStoreMax)
                .append(", prior=").append(prior)
                .append("\n");
        report.append("StoreQuality(B): premiumRatio=").append(qCfgB.premiumRatio())
                .append(", boost=").append(qCfgB.boostMin()).append("~").append(qCfgB.boostMax()).append("\n");
        report.append("Fixed: floor=").append(floor).append(", minFactor=").append(minFactor).append("\n\n");

        // 상위 30개(>=3.5 3~5%인 후보에 * 표시)
        report.append("Top 30 by closeness to B>=3.5~=4%\n");
        int limit = Math.min(30, rows.size());
        for (int i = 0; i < limit; i++) {
            Row r = rows.get(i);
            double b35 = r.b().over35Ratio();
            double b40 = r.b().over40Ratio();
            boolean inBand = b35 >= 0.03 && b35 <= 0.05;
            String mark = inBand ? "*" : " ";

            // >=4.0 가게가 생기면, 그 가게들이 리뷰 몇 개에서 나오는지도 같이 확인
            InfluenceConfig cfg = new InfluenceConfig(r.beta(), minFactor, 20);
            var evalB = evaluateWithReviewCountsProposed(scenarioB, now, prior, floor, cfg);
            String over40Counts = summarizeOver40(evalB);

            report.append(String.format(java.util.Locale.US,
                    "%s#%02d beta=%s | A: >=3.5=%.2f%% p95=%s | B: >=3.5=%.2f%% >=4.0=%.3f%% p95=%s p99=%s | B>=4.0 counts: %s\n",
                    mark,
                    i + 1,
                    r.beta().toPlainString(),
                    r.a().over35Ratio() * 100, r.a().p95,
                    b35 * 100, b40 * 100,
                    r.b().p95, r.b().p99,
                    over40Counts
            ));
        }

        // 전체 band(3~5%)가 몇 개인지 요약
        long bandCount = rows.stream().filter(r -> r.b().over35Ratio() >= 0.03 && r.b().over35Ratio() <= 0.05).count();
        report.append("\nBand count where B>=3.5 in [3%,5%]: ").append(bandCount).append(" / ").append(rows.size()).append("\n");

        writeReport(report);

        assertThat(rows).isNotEmpty();
    }
}
