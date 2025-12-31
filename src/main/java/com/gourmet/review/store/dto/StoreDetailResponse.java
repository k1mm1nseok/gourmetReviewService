package com.gourmet.review.store.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.gourmet.review.domain.enums.MemberTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDetailResponse {

    private Long id;

    private String name;

    private String categoryName;

    private String regionName;

    private String address;

    private String detailedAddress;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private BigDecimal scoreWeighted;

    private BigDecimal avgRating;

    private Boolean isBlind;

    private String blindMessage;

    private Integer reviewCount;

    private Integer reviewCountValid;

    private Integer scrapCount;

    private Integer viewCount;

    private String priceRangeLunch;

    private String priceRangeDinner;

    private Boolean isParking;

    private List<AwardResponse> awards;

    private List<RecentReviewResponse> recentReviews;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AwardResponse {
        private String awardName;
        private String awardGrade;
        private Integer awardYear;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentReviewResponse {
        private Long id;
        private String memberNickname;
        private MemberTier memberTier;
        private BigDecimal scoreCalculated;
        private BigDecimal scoreTaste;
        private BigDecimal scoreValue;
        private BigDecimal scoreAmbiance;
        private BigDecimal scoreService;
        private String content;
        private List<String> images;
        private Integer helpfulCount;
        private LocalDateTime createdAt;
    }
}
