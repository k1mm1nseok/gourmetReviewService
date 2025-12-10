package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 지역 엔티티 (계층 구조)
 * 예: 서울특별시 > 강남구 > 역삼동
 */
@Entity
@Table(name = "region", indexes = {
        @Index(name = "idx_region_parent", columnList = "parent_id"),
        @Index(name = "idx_region_depth", columnList = "depth")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 상위 지역 (자기참조)
     * null이면 최상위 지역 (시/도)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Region parent;

    /**
     * 계층 깊이
     * 0: 시/도 (예: 서울특별시, 경기도)
     * 1: 구/군 (예: 강남구, 수원시)
     * 2: 동/읍/면 (예: 역삼동, 영통동)
     */
    @Column(name = "depth", nullable = false)
    @Builder.Default
    private Integer depth = 0;

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 최상위 지역(시/도)인지 확인
     */
    public boolean isProvince() {
        return parent == null && depth == 0;
    }

    /**
     * 구/군 레벨인지 확인
     */
    public boolean isDistrict() {
        return depth == 1;
    }

    /**
     * 동/읍/면 레벨인지 확인
     */
    public boolean isTown() {
        return depth == 2;
    }
}
