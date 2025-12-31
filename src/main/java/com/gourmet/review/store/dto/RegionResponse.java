package com.gourmet.review.store.dto;

import com.gourmet.review.domain.entity.Region;
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
public class RegionResponse {

    private Long id;

    private String name;

    private Integer depth;

    private Long parentId;

    @Builder.Default
    private List<RegionResponse> children = new ArrayList<>();

    public static RegionResponse from(Region region) {
        Long parentId = region.getParent() != null ? region.getParent().getId() : null;
        return RegionResponse.builder()
                .id(region.getId())
                .name(region.getName())
                .depth(region.getDepth())
                .parentId(parentId)
                .build();
    }
}

