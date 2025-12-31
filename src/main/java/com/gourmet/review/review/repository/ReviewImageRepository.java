package com.gourmet.review.review.repository;

import com.gourmet.review.domain.entity.ReviewImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    List<ReviewImage> findByReviewIdOrderByDisplayOrderAsc(Long reviewId);

    List<ReviewImage> findByReviewIdInOrderByReviewIdAscDisplayOrderAsc(List<Long> reviewIds);

    void deleteByReviewId(Long reviewId);
}
