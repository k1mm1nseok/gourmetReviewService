package com.gourmet.review.review.controller;

import com.gourmet.review.common.dto.ApiResponse;
import com.gourmet.review.review.dto.ReviewModerationResponse;
import com.gourmet.review.review.dto.ReviewRejectRequest;
import com.gourmet.review.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/pending")
    public ApiResponse<Page<ReviewModerationResponse>> getPendingReviews(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(reviewService.getPendingReviews(pageable));
    }

    @PostMapping("/{reviewId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long reviewId) {
        reviewService.approveReview(reviewId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{reviewId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long reviewId,
                                    @RequestBody @Valid ReviewRejectRequest request) {
        reviewService.rejectReview(reviewId, request);
        return ApiResponse.success(null);
    }
}
