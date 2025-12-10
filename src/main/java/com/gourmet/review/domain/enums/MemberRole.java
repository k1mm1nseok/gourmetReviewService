package com.gourmet.review.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 회원 권한
 */
@Getter
@RequiredArgsConstructor
public enum MemberRole {
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String description;
}
