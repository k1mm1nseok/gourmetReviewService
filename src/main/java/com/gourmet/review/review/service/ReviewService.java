package com.gourmet.review.review.service;

import com.gourmet.review.review.dto.ReviewCreateRequest;
import com.gourmet.review.review.dto.ReviewDetailResponse;
import com.gourmet.review.review.dto.ReviewModerationResponse;
import com.gourmet.review.review.dto.ReviewRejectRequest;
import com.gourmet.review.review.dto.ReviewResponse;
import com.gourmet.review.review.dto.ReviewUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    ReviewResponse registerReview(ReviewCreateRequest request);

    ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request);

    void deleteReview(Long reviewId);

    ReviewDetailResponse getReview(Long reviewId);

    Page<ReviewResponse> getStoreReviews(Long storeId, Pageable pageable);

    Page<ReviewResponse> getMyReviews(Pageable pageable);

    Page<ReviewModerationResponse> getPendingReviews(Pageable pageable);

    void markHelpful(Long reviewId);

    void unmarkHelpful(Long reviewId);

    void approveReview(Long reviewId);

    void rejectReview(Long reviewId, ReviewRejectRequest request);

}
