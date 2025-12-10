package com.gourmet.review.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 리뷰 상태
 * 검수 및 공개 프로세스 관리
 */
@Getter
@RequiredArgsConstructor
public enum ReviewStatus {
    PENDING("검수 대기", "관리자 검수 대기 중"),
    APPROVED("승인", "검수 승인됨"),
    REJECTED("반려", "검수 반려됨"),
    BLIND_HELD("블라인드 보류", "가게 리뷰 5개 미만으로 블라인드 처리"),
    PUBLIC("공개", "정상 공개 중"),
    SUSPENDED("일시정지", "위반으로 인한 일시정지");

    private final String description;
    private final String detailDescription;

    /**
     * 공개 가능한 상태인지 확인
     */
    public boolean isPublic() {
        return this == PUBLIC;
    }

    /**
     * 검수 완료 상태인지 확인
     */
    public boolean isReviewed() {
        return this == APPROVED || this == REJECTED;
    }
}
