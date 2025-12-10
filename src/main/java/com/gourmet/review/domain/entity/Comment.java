package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 댓글 엔티티
 * 리뷰 또는 게시글에 달 수 있음 (둘 중 하나만 존재)
 */
@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_comment_review", columnList = "review_id"),
        @Index(name = "idx_comment_board", columnList = "board_id"),
        @Index(name = "idx_comment_member", columnList = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 리뷰 ID (리뷰 댓글인 경우)
     * review_id와 board_id 중 하나만 존재
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    /**
     * 게시글 ID (게시글 댓글인 경우)
     * review_id와 board_id 중 하나만 존재
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 리뷰 댓글인지 확인
     */
    public boolean isReviewComment() {
        return review != null && board == null;
    }

    /**
     * 게시글 댓글인지 확인
     */
    public boolean isBoardComment() {
        return board != null && review == null;
    }

    /**
     * 댓글 내용 수정
     */
    public void updateContent(String content) {
        this.content = content;
    }
}
