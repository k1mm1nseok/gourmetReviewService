package com.gourmet.review.store.dto;

import com.gourmet.review.domain.entity.Category;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;

    private String name;

    private Integer depth;

    private Long parentId;

    @Builder.Default
    private List<CategoryResponse> children = new ArrayList<>();

    public static CategoryResponse from(Category category) {
        Long parentId = category.getParent() != null ? category.getParent().getId() : null;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .depth(category.getDepth())
                .parentId(parentId)
                .build();
    }
}

