package com.gourmet.review.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreRegisterRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long regionId;

    @NotBlank
    private String address;

    private String detailedAddress;

    @NotNull
    private BigDecimal latitude;

    @NotNull
    private BigDecimal longitude;

    private String priceRangeLunch;

    private String priceRangeDinner;

    @Builder.Default
    private Boolean isParking = false;
}

