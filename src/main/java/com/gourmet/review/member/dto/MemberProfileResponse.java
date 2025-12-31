package com.gourmet.review.member.dto;

import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private MemberTier tier;
    private MemberRole role;
    private LocalDateTime createdAt;
    private Integer reviewCount;
    private Integer helpfulCount;
    private Integer violationCount;
    private LocalDateTime lastReviewAt;
    private Boolean isActive;
}
