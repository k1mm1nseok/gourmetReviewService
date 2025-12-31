package com.gourmet.review.review.dto;

import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.domain.enums.ReviewStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailResponse {

    private Long id;
    private StoreSummary store;
    private MemberSummary member;
    private BigDecimal scoreTaste;
    private BigDecimal scoreValue;
    private BigDecimal scoreAmbiance;
    private BigDecimal scoreService;
    private BigDecimal scoreCalculated;
    private String content;
    private LocalDate visitDate;
    private Integer visitCount;
    private Integer helpfulCount;
    private Boolean isHelpfulByMe;
    private ReviewStatus status;
    private List<ImageResponse> images;
    private List<CommentResponse> comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreSummary {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSummary {
        private Long id;
        private String nickname;
        private MemberTier tier;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResponse {
        private Long id;
        private String imageUrl;
        private Integer displayOrder;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {
        private Long id;
        private String memberNickname;
        private String content;
        private LocalDateTime createdAt;
    }
}
