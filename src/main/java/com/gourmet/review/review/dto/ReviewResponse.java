package com.gourmet.review.review.dto;

import com.gourmet.review.domain.enums.ReviewStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long storeId;
    private String storeName;
    private BigDecimal scoreTaste;
    private BigDecimal scoreService;
    private BigDecimal scoreAmbiance;
    private BigDecimal scoreValue;
    private BigDecimal scoreCalculated;
    private String content;
    private Integer visitCount;
    private ReviewStatus status;
    private Integer helpfulCount;
    private Boolean isHelpfulByMe;
    private LocalDateTime createdAt;
    private Integer partySize;

}
