package com.gourmet.review.review.dto;

import com.gourmet.review.domain.enums.MemberTier;
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
public class ReviewModerationResponse {

    private Long id;
    private String storeName;
    private String memberNickname;
    private MemberTier memberTier;
    private BigDecimal scoreCalculated;
    private String content;
    private ReviewStatus status;
    private LocalDateTime createdAt;
}
