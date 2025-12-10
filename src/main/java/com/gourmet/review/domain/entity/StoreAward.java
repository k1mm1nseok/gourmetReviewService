package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 가게 수상 이력 엔티티
 * 미슐랭, 블루리본 등 공신력 있는 수상 이력 관리
 */
@Entity
@Table(name = "store_award", indexes = {
        @Index(name = "idx_store_award_store", columnList = "store_id"),
        @Index(name = "idx_store_award_year", columnList = "award_year")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StoreAward extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * 수상명 (예: 미슐랭 가이드, 블루리본 서베이)
     */
    @Column(name = "award_name", nullable = false, length = 100)
    private String awardName;

    /**
     * 수상 등급 (예: 1스타, 2스타, 3스타, 빕구르망)
     */
    @Column(name = "award_grade", length = 50)
    private String awardGrade;

    /**
     * 수상 연도
     */
    @Column(name = "award_year", nullable = false)
    private Integer awardYear;
}
