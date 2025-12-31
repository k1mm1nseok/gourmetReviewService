package com.gourmet.review.review.repository;

import com.gourmet.review.domain.entity.Review;
import com.gourmet.review.domain.enums.ReviewStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"store", "member"})
    Optional<Review> findWithStoreAndMemberById(Long id);

    @EntityGraph(attributePaths = {"store"})
    Page<Review> findByStoreIdAndStatus(Long storeId, ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"store"})
    Page<Review> findByMemberId(Long memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"member"})
    List<Review> findByStoreIdAndStatus(Long storeId, ReviewStatus status);

    @EntityGraph(attributePaths = {"member"})
    List<Review> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"member"})
    List<Review> findByStoreIdAndStatusIn(Long storeId, Collection<ReviewStatus> statuses);

    long countByStoreIdAndStatusIn(Long storeId, Collection<ReviewStatus> statuses);

    @EntityGraph(attributePaths = {"store", "member"})
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    /**
     * 쿨다운(PENDING) 만료 리뷰를 조회한다.
     * - createdAt + duration 이전에 작성된 PENDING 리뷰
     */
    @EntityGraph(attributePaths = {"store", "member"})
    List<Review> findByStatusAndCreatedAtBefore(ReviewStatus status,
                                               java.time.LocalDateTime cutoff);

    /**
     * 편차 보정 대상 산정을 위해, 특정 회원의 최근 PUBLIC 리뷰를 최신순으로 제한 조회한다.
     */
    @EntityGraph(attributePaths = {"member"})
    List<Review> findTop20ByMemberIdAndStatusOrderByCreatedAtDesc(Long memberId, ReviewStatus status);

    /**
     * 특정 회원이 작성한 PUBLIC 리뷰가 존재하는 storeId 목록을 중복 없이 가져온다.
     * (tier 변경 소급 재계산 트리거용)
     */
    @org.springframework.data.jpa.repository.Query(
            "select distinct r.store.id from Review r " +
            "where r.member.id = :memberId and r.status = :status")
    List<Long> findDistinctStoreIdsByMemberIdAndStatus(Long memberId, ReviewStatus status);

    /**
     * 특정 storeId 목록에 대해 PUBLIC 리뷰를 가진 storeId만 중복 없이 반환한다.
     * (00:00 batch 등에서 재계산 대상 store 추출용)
     */
    @org.springframework.data.jpa.repository.Query(
            "select distinct r.store.id from Review r " +
            "where r.status = :status and r.store.id in :storeIds")
    List<Long> findDistinctStoreIdsByStatusAndStoreIdIn(ReviewStatus status, List<Long> storeIds);

    /**
     * p0: 극단 점수(1.0/5.0) 리뷰 쿨다운(12h) 검증을 위한 존재 여부 체크
     */
    boolean existsByMemberIdAndScoreCalculatedAndCreatedAtAfter(Long memberId,
                                                                java.math.BigDecimal scoreCalculated,
                                                                java.time.LocalDateTime cutoff);
}
