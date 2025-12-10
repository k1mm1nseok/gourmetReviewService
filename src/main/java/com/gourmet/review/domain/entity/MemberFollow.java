package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 회원 팔로우 엔티티
 * 회원 간 팔로우 관계
 */
@Entity
@Table(name = "member_follow",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_member_follow", columnNames = {"follower_id", "following_id"})
       },
       indexes = {
           @Index(name = "idx_member_follow_follower", columnList = "follower_id"),
           @Index(name = "idx_member_follow_following", columnList = "following_id")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberFollow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 팔로워 (팔로우를 하는 사람)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private Member follower;

    /**
     * 팔로잉 (팔로우를 받는 사람)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private Member following;

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 자기 자신을 팔로우하는지 검증
     */
    public boolean isSelfFollow() {
        return follower.getId().equals(following.getId());
    }
}
