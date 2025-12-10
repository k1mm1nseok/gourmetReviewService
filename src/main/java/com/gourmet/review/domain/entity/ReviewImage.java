package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 리뷰 이미지 엔티티
 * 하나의 리뷰에 여러 이미지 첨부 가능
 */
@Entity
@Table(name = "review_image", indexes = {
        @Index(name = "idx_review_image_review", columnList = "review_id"),
        @Index(name = "idx_review_image_order", columnList = "review_id, display_order")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /**
     * 이미지 URL (S3, CDN 등)
     */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /**
     * 표시 순서 (0부터 시작)
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
