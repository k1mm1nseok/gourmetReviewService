package com.gourmet.review.domain.entity;

import com.gourmet.review.domain.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 리뷰 엔티티
 * v1.3.2: status 기반 상태 관리, score_calculated, admin_comment 추가
 * BaseEntity 미상속: created_at updatable=false 처리 필요
 */
@Entity
@Table(name = "review", indexes = {
        @Index(name = "idx_review_store", columnList = "store_id"),
        @Index(name = "idx_review_member", columnList = "member_id"),
        @Index(name = "idx_review_status", columnList = "status"),
        @Index(name = "idx_review_created_at", columnList = "created_at"),
        @Index(name = "idx_review_store_status", columnList = "store_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 맛 점수 (0.00 ~ 5.00)
     */
    @Column(name = "score_taste", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreTaste;

    /**
     * 서비스 점수 (0.00 ~ 5.00)
     */
    @Column(name = "score_service", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreService;

    /**
     * 분위기 점수 (0.00 ~ 5.00)
     */
    @Column(name = "score_mood", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreMood;

    /**
     * 가격 점수 (0.00 ~ 5.00)
     */
    @Column(name = "score_price", nullable = false, precision = 3, scale = 2)
    private BigDecimal scorePrice;

    /**
     * 계산된 종합 점수 (가중합)
     * v1.3.2: total_score → score_calculated 네이밍 변경
     * 다차원 점수의 가중 평균 (맛:50%, 서비스:20%, 분위기:15%, 가격:15%)
     */
    @Column(name = "score_calculated", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreCalculated;

    /**
     * 리뷰 상태
     * v1.3.2: is_verified 삭제 후 status로 대체
     * PENDING, APPROVED, REJECTED, BLIND_HELD, PUBLIC, SUSPENDED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    /**
     * 재방문 여부
     */
    @Column(name = "is_revisit", nullable = false)
    @Builder.Default
    private Boolean isRevisit = false;

    /**
     * 방문일
     */
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    /**
     * 좋아요 수
     */
    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * 관리자 코멘트 (반려 사유 등)
     * v1.3.2: 신규 추가
     */
    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    /**
     * 작성일시 (시간 감가상각 기준)
     * updatable = false로 설정하여 수정 불가
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시
     * v1.3.2: 신규 추가
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 종합 점수 계산
     * 맛:50%, 서비스:20%, 분위기:15%, 가격:15%
     */
    @PrePersist
    @PreUpdate
    public void calculateScore() {
        BigDecimal tasteWeight = new BigDecimal("0.50");
        BigDecimal serviceWeight = new BigDecimal("0.20");
        BigDecimal moodWeight = new BigDecimal("0.15");
        BigDecimal priceWeight = new BigDecimal("0.15");

        this.scoreCalculated = scoreTaste.multiply(tasteWeight)
                .add(scoreService.multiply(serviceWeight))
                .add(scoreMood.multiply(moodWeight))
                .add(scorePrice.multiply(priceWeight))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 리뷰 승인
     */
    public void approve() {
        this.status = ReviewStatus.APPROVED;
    }

    /**
     * 리뷰 반려
     * @param adminComment 반려 사유
     */
    public void reject(String adminComment) {
        this.status = ReviewStatus.REJECTED;
        this.adminComment = adminComment;
    }

    /**
     * 리뷰 공개
     */
    public void publish() {
        if (this.status == ReviewStatus.APPROVED) {
            this.status = ReviewStatus.PUBLIC;
        }
    }

    /**
     * 리뷰 일시정지
     * @param adminComment 정지 사유
     */
    public void suspend(String adminComment) {
        this.status = ReviewStatus.SUSPENDED;
        this.adminComment = adminComment;
    }

    /**
     * 블라인드 보류 처리
     * 가게의 리뷰 수가 5개 미만일 때
     */
    public void holdForBlind() {
        this.status = ReviewStatus.BLIND_HELD;
    }

    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 리뷰 내용 수정
     * @param content 새로운 내용
     * @param scoreTaste 맛 점수
     * @param scoreService 서비스 점수
     * @param scoreMood 분위기 점수
     * @param scorePrice 가격 점수
     * @param isRevisit 재방문 여부
     */
    public void updateContent(String content, BigDecimal scoreTaste, BigDecimal scoreService,
                             BigDecimal scoreMood, BigDecimal scorePrice, Boolean isRevisit) {
        this.content = content;
        this.scoreTaste = scoreTaste;
        this.scoreService = scoreService;
        this.scoreMood = scoreMood;
        this.scorePrice = scorePrice;
        this.isRevisit = isRevisit;
        // calculateScore는 @PreUpdate에서 자동 호출됨
    }

    /**
     * 생성 후 경과 일수 계산
     * 시간 감가상각 계산에 사용
     */
    public long getDaysSinceCreation() {
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now());
    }
}
