package com.gourmet.review.store.controller;

import com.gourmet.review.common.dto.ApiResponse;
import com.gourmet.review.store.dto.StoreResponse;
import com.gourmet.review.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MyScrapController {

    private final StoreService storeService;

    @GetMapping("/scraps")
    public ApiResponse<Page<StoreResponse>> getMyScraps(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(storeService.getMyScraps(pageable));
    }
}

