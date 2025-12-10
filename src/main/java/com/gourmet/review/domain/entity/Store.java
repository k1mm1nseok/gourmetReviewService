package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 가게 엔티티
 * v1.3.2: score_weighted 네이밍, review_count_valid 추가, 블라인드 처리
 */
@Entity
@Table(name = "store", indexes = {
        @Index(name = "idx_store_category", columnList = "category_id"),
        @Index(name = "idx_store_region", columnList = "region_id"),
        @Index(name = "idx_store_score_weighted", columnList = "score_weighted"),
        @Index(name = "idx_store_review_count_valid", columnList = "review_count_valid"),
        @Index(name = "idx_store_is_blind", columnList = "is_blind")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "detailed_address", length = 200)
    private String detailedAddress;

    /**
     * 위도 (소수점 8자리까지)
     */
    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * 경도 (소수점 8자리까지)
     */
    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    /**
     * 평균 평점 (단순 산술 평균)
     */
    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    /**
     * 가중 평점
     * v1.3.2: weighted_rating → score_weighted 네이밍 변경
     * 시간 감가상각, 회원 등급, 편차 보정 등 적용된 최종 점수
     */
    @Column(name = "score_weighted", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal scoreWeighted = BigDecimal.ZERO;

    /**
     * 전체 리뷰 수 (상태 무관)
     */
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    /**
     * 유효 리뷰 수 (PUBLIC 상태만 카운트)
     * v1.3.2: 신규 추가
     */
    @Column(name = "review_count_valid", nullable = false)
    @Builder.Default
    private Integer reviewCountValid = 0;

    /**
     * 블라인드 여부 (리뷰 5개 미만)
     * v1.3.2: 신규 추가
     * true인 경우 평점 및 리뷰 수 미공개
     */
    @Column(name = "is_blind", nullable = false)
    @Builder.Default
    private Boolean isBlind = true;

    @Column(name = "scrap_count", nullable = false)
    @Builder.Default
    private Integer scrapCount = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "price_range_lunch", length = 50)
    private String priceRangeLunch;

    @Column(name = "price_range_dinner", length = 50)
    private String priceRangeDinner;

    @Column(name = "is_parking", nullable = false)
    @Builder.Default
    private Boolean isParking = false;

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 리뷰 추가 시 호출
     */
    public void incrementReviewCount() {
        this.reviewCount++;
    }

    /**
     * PUBLIC 상태 리뷰 추가 시 호출
     */
    public void incrementReviewCountValid() {
        this.reviewCountValid++;
        updateBlindStatus();
    }

    /**
     * PUBLIC 상태 리뷰 제거 시 호출
     */
    public void decrementReviewCountValid() {
        if (this.reviewCountValid > 0) {
            this.reviewCountValid--;
            updateBlindStatus();
        }
    }

    /**
     * 블라인드 상태 업데이트
     * 유효 리뷰 수가 5개 미만이면 블라인드 처리
     */
    private void updateBlindStatus() {
        this.isBlind = this.reviewCountValid < 5;
    }

    /**
     * 스크랩 수 증가
     */
    public void incrementScrapCount() {
        this.scrapCount++;
    }

    /**
     * 스크랩 수 감소
     */
    public void decrementScrapCount() {
        if (this.scrapCount > 0) {
            this.scrapCount--;
        }
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 평균 평점 업데이트
     * @param newAvgRating 새로운 평균 평점
     */
    public void updateAvgRating(BigDecimal newAvgRating) {
        this.avgRating = newAvgRating;
    }

    /**
     * 가중 평점 업데이트
     * @param newScoreWeighted 새로운 가중 평점
     */
    public void updateScoreWeighted(BigDecimal newScoreWeighted) {
        this.scoreWeighted = newScoreWeighted;
    }

    /**
     * 가게 정보 수정
     */
    public void updateInfo(String name, String address, String detailedAddress,
                          BigDecimal latitude, BigDecimal longitude,
                          String priceRangeLunch, String priceRangeDinner,
                          Boolean isParking) {
        this.name = name;
        this.address = address;
        this.detailedAddress = detailedAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRangeLunch = priceRangeLunch;
        this.priceRangeDinner = priceRangeDinner;
        this.isParking = isParking;
    }
}
