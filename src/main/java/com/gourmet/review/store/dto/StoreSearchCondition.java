package com.gourmet.review.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSearchCondition {

    private String keyword;

    private Long categoryId;

    private Long regionId;

    private BigDecimal minScore;

    private BigDecimal maxScore;

    private String sortBy;

    private String sortDirection;

    private Integer page;

    private Integer size;
}
