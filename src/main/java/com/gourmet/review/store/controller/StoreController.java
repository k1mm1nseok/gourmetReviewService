package com.gourmet.review.store.controller;

import com.gourmet.review.common.dto.ApiResponse;
import com.gourmet.review.store.dto.StoreDetailResponse;
import com.gourmet.review.store.dto.StoreRegisterRequest;
import com.gourmet.review.store.dto.StoreResponse;
import com.gourmet.review.store.dto.StoreSearchCondition;
import com.gourmet.review.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    // TODO: @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StoreResponse> register(@RequestBody @Valid StoreRegisterRequest request) {
        return ApiResponse.success(storeService.register(request));
    }

    @GetMapping("/{storeId}")
    public ApiResponse<StoreDetailResponse> getStoreDetail(@PathVariable Long storeId) {
        return ApiResponse.success(storeService.getStoreDetail(storeId));
    }

    @GetMapping("/search")
    public ApiResponse<Page<StoreResponse>> search(@ModelAttribute StoreSearchCondition condition) {
        return ApiResponse.success(storeService.search(condition));
    }

    @PostMapping("/{storeId}/scrap")
    public ApiResponse<Void> scrap(@PathVariable Long storeId) {
        storeService.scrap(storeId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{storeId}/scrap")
    public ApiResponse<Void> unscrap(@PathVariable Long storeId) {
        storeService.unscrap(storeId);
        return ApiResponse.success(null);
    }
}

