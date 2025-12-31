package com.gourmet.review.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원-가게별 누적 방문 횟수
 * PUBLIC 리뷰 전환 시점에만 증가하며, 삭제/비공개로 감소하지 않는다.
 */
@Entity
@Table(name = "member_store_visit",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_store_visit", columnNames = {"member_id", "store_id"})
        },
        indexes = {
                @Index(name = "idx_member_store_visit_member", columnList = "member_id"),
                @Index(name = "idx_member_store_visit_store", columnList = "store_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberStoreVisit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "visit_count", nullable = false)
    @Builder.Default
    private Integer visitCount = 0;

    public int incrementVisitCount() {
        this.visitCount++;
        return this.visitCount;
    }
}
