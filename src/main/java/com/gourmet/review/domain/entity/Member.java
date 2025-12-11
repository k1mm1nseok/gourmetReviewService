package com.gourmet.review.domain.entity;

import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원 엔티티
 * v1.3.2: tier 기반 등급제, 활동성 추적, 편차 보정 대상 관리
 */
@Entity
@Table(name = "member", indexes = {
        @Index(name = "idx_member_email", columnList = "email", unique = true),
        @Index(name = "idx_member_nickname", columnList = "nickname", unique = true),
        @Index(name = "idx_member_tier", columnList = "tier"),
        @Index(name = "idx_member_last_review_at", columnList = "last_review_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "nickname", nullable = false, unique = true, length = 50)
    private String nickname;

    /**
     * 비밀번호
     * BCrypt 기반 암호화 사용 권장 (Spring Security BCryptPasswordEncoder)
     * 평문 저장 금지
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    /**
     * 회원 등급 (BRONZE, SILVER, GOLD, GOURMET, BLACK)
     * v1.3.2: level 삭제 후 tier로 대체
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    @Builder.Default
    private MemberTier tier = MemberTier.BRONZE;

    /**
     * 누적 도움됨 수
     * 다른 회원들로부터 받은 "도움이 돼요" 총합
     */
    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    /**
     * 누적 리뷰 수
     * 작성한 모든 리뷰 수 (상태 무관)
     */
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    /**
     * 누적 위반 횟수
     * 허위 리뷰, 부적절한 내용 등으로 인한 제재 이력
     */
    @Column(name = "violation_count", nullable = false)
    @Builder.Default
    private Integer violationCount = 0;

    /**
     * 마지막 리뷰 작성일시
     * 활동성 체크용 (휴면 회원 판단 기준)
     */
    @Column(name = "last_review_at")
    private LocalDateTime lastReviewAt;

    /**
     * 편차 보정 대상 여부
     * 평균 대비 ±2σ 이상 벗어난 평가를 지속하는 회원
     * true인 경우 해당 회원의 리뷰 가중치 감소
     */
    @Column(name = "is_deviation_target", nullable = false)
    @Builder.Default
    private Boolean isDeviationTarget = false;

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 리뷰 작성 시 호출
     */
    public void incrementReviewCount() {
        this.reviewCount++;
        this.lastReviewAt = LocalDateTime.now();
        updateTier();
    }

    /**
     * 도움됨 증가
     */
    public void incrementHelpfulCount() {
        this.helpfulCount++;
        updateTier();
    }

    /**
     * 도움됨 감소
     */
    public void decrementHelpfulCount() {
        if (this.helpfulCount > 0) {
            this.helpfulCount--;
            updateTier();
        }
    }

    /**
     * 위반 횟수 증가
     */
    public void incrementViolationCount() {
        this.violationCount++;
    }

    /**
     * 편차 보정 대상 설정
     */
    public void markAsDeviationTarget(boolean isTarget) {
        this.isDeviationTarget = isTarget;
    }

    /**
     * 등급 자동 업데이트
     * BLACK 등급은 수동으로만 부여 가능하므로 자동 업데이트에서 제외
     */
    private void updateTier() {
        if (this.tier != MemberTier.BLACK) {
            this.tier = MemberTier.calculateTier(this.reviewCount, this.helpfulCount);
        }
    }

    /**
     * 관리자가 BLACK 등급 부여
     */
    public void promoteToBlackTier() {
        this.tier = MemberTier.BLACK;
    }

    /**
     * 활동성 체크 (최근 6개월 내 리뷰 작성 여부)
     */
    public boolean isActive() {
        if (lastReviewAt == null) {
            return false;
        }
        return lastReviewAt.isAfter(LocalDateTime.now().minusMonths(6));
    }
}
