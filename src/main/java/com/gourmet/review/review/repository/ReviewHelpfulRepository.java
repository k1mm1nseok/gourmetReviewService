package com.gourmet.review.review.repository;

import com.gourmet.review.domain.entity.ReviewHelpful;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, Long> {

    boolean existsByReviewIdAndMemberId(Long reviewId, Long memberId);

    Optional<ReviewHelpful> findByReviewIdAndMemberId(Long reviewId, Long memberId);

    void deleteByReviewId(Long reviewId);
}
