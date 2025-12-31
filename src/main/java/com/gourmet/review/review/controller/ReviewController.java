package com.gourmet.review.review.controller;

import com.gourmet.review.common.dto.ApiResponse;
import com.gourmet.review.review.dto.ReviewCreateRequest;
import com.gourmet.review.review.dto.ReviewDetailResponse;
import com.gourmet.review.review.dto.ReviewResponse;
import com.gourmet.review.review.dto.ReviewUpdateRequest;
import com.gourmet.review.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewResponse> register(@RequestBody @Valid ReviewCreateRequest request) {
        return ApiResponse.success("리뷰가 작성되었습니다. 운영자 검수 후 공개됩니다.", reviewService.registerReview(request));
    }

    @PatchMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> update(@PathVariable Long reviewId,
                                              @RequestBody @Valid ReviewUpdateRequest request) {
        return ApiResponse.success(reviewService.updateReview(reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> delete(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{reviewId}")
    public ApiResponse<ReviewDetailResponse> get(@PathVariable Long reviewId) {
        return ApiResponse.success(reviewService.getReview(reviewId));
    }

    @PostMapping("/{reviewId}/helpful")
    public ApiResponse<Void> markHelpful(@PathVariable Long reviewId) {
        reviewService.markHelpful(reviewId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{reviewId}/helpful")
    public ApiResponse<Void> unmarkHelpful(@PathVariable Long reviewId) {
        reviewService.unmarkHelpful(reviewId);
        return ApiResponse.success(null);
    }
}
