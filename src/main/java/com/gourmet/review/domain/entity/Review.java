package com.gourmet.review.domain.entity;

import com.gourmet.review.domain.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 함께 방문한 인원 수
     */
    @Column(name = "party_size", nullable = false)
    private Integer partySize;

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
     * Ambiance로 용어 통일 (DDL 컬럼: score_mood)
     */
    @Column(name = "score_ambiance", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreAmbiance;

    /**
     * 가성비 점수 (0.00 ~ 5.00)
     * Value로 의미 명확화 (DDL 컬럼: score_price)
     */
    @Column(name = "score_value", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreValue;

    /**
     * 계산된 종합 점수 (가중합)
     * v1.3.2: total_score → score_calculated 네이밍 변경
     * 다차원 점수의 가중 평균 (맛:40%, 서비스:30%, 분위기:15%, 가격:15%)
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
     * 방문일
     */
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    /**
     * 도움이 됨 수
     */
    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    /**
     * 해당 가게 방문 횟수 (PUBLIC 기준, 본 리뷰 포함)
     */
    @Column(name = "visit_count", nullable = false)
    @Builder.Default
    private Integer visitCount = 0;


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
     * 정책 문서 v1.3.2 기준: 맛 40%, 가성비(Value) 30%, 분위기(Ambiance) 15%, 서비스 15%
     */
    @PrePersist
    @PreUpdate
    public void calculateScore() {
        BigDecimal tasteWeight = new BigDecimal("0.40");      // 40%
        BigDecimal valueWeight = new BigDecimal("0.30");      // 30% (가성비)
        BigDecimal ambianceWeight = new BigDecimal("0.15");   // 15% (분위기)
        BigDecimal serviceWeight = new BigDecimal("0.15");    // 15%

        this.scoreCalculated = scoreTaste.multiply(tasteWeight)
                .add(scoreValue.multiply(valueWeight))
                .add(scoreAmbiance.multiply(ambianceWeight))
                .add(scoreService.multiply(serviceWeight))
                .setScale(2, RoundingMode.HALF_UP);
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
    public boolean publish() {
        if (this.status == ReviewStatus.APPROVED || this.status == ReviewStatus.BLIND_HELD) {
            this.status = ReviewStatus.PUBLIC;
            return true;
        }
        return false;
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
     * 도움이 됨 수 증가
     */
    public void incrementHelpfulCount(){this.helpfulCount++;}

    /**
     * 도움이 됨 수 감소
     */
    public void decrementHelpfulCount(){
        if(this.helpfulCount > 0){
            this.helpfulCount--;
        }
    }


    /**
     * 리뷰 내용 및 점수 수정
     * @param content 새로운 내용
     * @param scoreTaste 맛 점수
     * @param scoreValue 가성비 점수
     * @param scoreAmbiance 분위기 점수
     * @param scoreService 서비스 점수
     */
    public void updateContent(String content, BigDecimal scoreTaste, BigDecimal scoreValue,
                             BigDecimal scoreAmbiance, BigDecimal scoreService) {
        this.content = content;
        this.scoreTaste = scoreTaste;
        this.scoreValue = scoreValue;
        this.scoreAmbiance = scoreAmbiance;
        this.scoreService = scoreService;
        // 종합 점수 재계산은 @PreUpdate에서 자동 호출
    }

    /**
     * 리뷰 내용 및 메타 정보 수정
     */
    public void updateReview(String title, Integer partySize, String content, BigDecimal scoreTaste,
                             BigDecimal scoreValue, BigDecimal scoreAmbiance, BigDecimal scoreService) {
        this.title = title;
        this.partySize = partySize;
        updateContent(content, scoreTaste, scoreValue, scoreAmbiance, scoreService);
    }

    public void updateVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    /**
     * 생성 후 경과 일수 계산
     * 시간 감가상각 계산에 사용
     */
    public long getDaysSinceCreation() {
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now());
    }
}
