package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 엔티티
 * 공지사항, FAQ, 리뷰 가이드 등
 */
@Entity
@Table(name = "board", indexes = {
        @Index(name = "idx_board_member", columnList = "member_id"),
        @Index(name = "idx_board_type", columnList = "type"),
        @Index(name = "idx_board_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 게시글 유형
     * NOTICE: 공지사항
     * FAQ: 자주 묻는 질문
     * REVIEW_GUIDE: 리뷰 작성 가이드
     * EVENT: 이벤트
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
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
     * 게시글 수정
     */
    public void updateBoard(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
