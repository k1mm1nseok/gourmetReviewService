package com.gourmet.review.store.controller;

import com.gourmet.review.common.dto.ApiResponse;
import com.gourmet.review.store.dto.RegionResponse;
import com.gourmet.review.store.service.StoreLookupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final StoreLookupService storeLookupService;

    @GetMapping
    public ApiResponse<List<RegionResponse>> getRegions() {
        return ApiResponse.success(storeLookupService.getRegions());
    }
}

