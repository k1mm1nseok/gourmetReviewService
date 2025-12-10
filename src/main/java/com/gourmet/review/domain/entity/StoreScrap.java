package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 가게 스크랩 엔티티
 * 회원이 관심 가게를 스크랩(북마크)
 */
@Entity
@Table(name = "store_scrap",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_store_scrap", columnNames = {"store_id", "member_id"})
       },
       indexes = {
           @Index(name = "idx_store_scrap_store", columnList = "store_id"),
           @Index(name = "idx_store_scrap_member", columnList = "member_id")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StoreScrap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
