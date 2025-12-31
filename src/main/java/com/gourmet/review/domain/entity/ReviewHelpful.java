package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 리뷰 좋아요 엔티티
 * 회원이 리뷰에 "도움이 돼요" 표시
 */
@Entity
@Table(name = "review_helpful",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_review_helpful", columnNames = {"review_id", "member_id"})
       },
       indexes = {
           @Index(name = "idx_review_helpful_review", columnList = "review_id"),
           @Index(name = "idx_review_helpful_member", columnList = "member_id")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewHelpful extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
