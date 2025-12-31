package com.gourmet.review.common.util;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * SecurityContext에서 현재 회원 정보를 추출하기 위한 유틸리티
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 현재 인증된 회원 ID를 반환한다.
     * TODO: 프로젝트의 인증 방식(JWT/세션)에 맞게 principal 파싱 로직을 확정한다.
     */
    public static Optional<Long> getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return Optional.of(id);
        }
        if (principal instanceof UserDetails userDetails) {
            return parseLong(userDetails.getUsername());
        }
        if (principal instanceof String principalStr) {
            return parseLong(principalStr);
        }
        return Optional.empty();
    }

    private static Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
