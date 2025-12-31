package com.gourmet.review.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {

    private Long id;

    private String name;

    private String categoryName;

    private String regionName;

    private String address;

    private BigDecimal scoreWeighted;

    private Boolean isBlind;

    private Integer reviewCountValid;

    private Integer scrapCount;

    private String thumbnailImage;

    private LocalDateTime createdAt;
}

