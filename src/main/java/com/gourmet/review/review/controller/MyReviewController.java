package com.gourmet.review.review.controller;

import com.gourmet.review.common.dto.ApiResponse;
import com.gourmet.review.review.dto.ReviewResponse;
import com.gourmet.review.review.service.ReviewService;
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
public class MyReviewController {

    private final ReviewService reviewService;

    @GetMapping("/reviews")
    public ApiResponse<Page<ReviewResponse>> getMyReviews(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(reviewService.getMyReviews(pageable));
    }
}
