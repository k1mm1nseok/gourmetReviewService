package com.gourmet.review.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 카테고리 엔티티 (계층 구조)
 * 예: 한식 > 찌개/탕 > 김치찌개
 */
@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_category_parent", columnList = "parent_id"),
        @Index(name = "idx_category_depth", columnList = "depth")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 상위 카테고리 (자기참조)
     * null이면 최상위 카테고리
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> children = new ArrayList<>();

    /**
     * 계층 깊이
     * 0: 최상위 (예: 한식, 중식, 일식)
     * 1: 중간 (예: 찌개/탕, 구이)
     * 2: 하위 (예: 김치찌개, 된장찌개)
     */
    @Column(name = "depth", nullable = false)
    @Builder.Default
    private Integer depth = 0;

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 최상위 카테고리인지 확인
     */
    public boolean isRoot() {
        return parent == null && depth == 0;
    }
}
