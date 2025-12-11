package com.gourmet.review.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 등급
 * 리뷰 수와 도움됨 수에 따라 자동 승급
 */
@Getter
@RequiredArgsConstructor
public enum MemberTier {
    BRONZE("브론즈", 0, 0),
    SILVER("실버", 5, 0),        // 리뷰 5개 (검수 통과는 별도 검증 필요)
    GOLD("골드", 30, 100),
    GOURMET("구르메", 100, 500),  // 운영진 승인은 Member 엔티티의 별도 플래그(isGourmetApproved)로 처리
    BLACK("블랙", 0, 0);          // 관리자 지정 등급

    private final String description;
    private final int requiredReviewCount;
    private final int requiredHelpfulCount;

    /**
     * 리뷰 수와 도움됨 수에 따라 적절한 등급 반환
     * BLACK 등급은 수동으로만 부여 가능
     */
    public static MemberTier calculateTier(int reviewCount, int helpfulCount) {
        if (reviewCount >= GOURMET.requiredReviewCount && helpfulCount >= GOURMET.requiredHelpfulCount) {
            return GOURMET;
        } else if (reviewCount >= GOLD.requiredReviewCount && helpfulCount >= GOLD.requiredHelpfulCount) {
            return GOLD;
        } else if (reviewCount >= SILVER.requiredReviewCount && helpfulCount >= SILVER.requiredHelpfulCount) {
            return SILVER;
        }
        return BRONZE;
    }
}
