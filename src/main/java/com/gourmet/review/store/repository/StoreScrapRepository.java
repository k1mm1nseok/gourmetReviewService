package com.gourmet.review.store.repository;

import com.gourmet.review.domain.entity.StoreScrap;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreScrapRepository extends JpaRepository<StoreScrap, Long> {

    boolean existsByStoreIdAndMemberId(Long storeId, Long memberId);

    Optional<StoreScrap> findByStoreIdAndMemberId(Long storeId, Long memberId);

    @EntityGraph(attributePaths = {"store", "store.category", "store.region"})
    Page<StoreScrap> findByMemberId(Long memberId, Pageable pageable);
}

