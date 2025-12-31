package com.gourmet.review.store.repository;

import com.gourmet.review.domain.entity.Store;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

    @EntityGraph(attributePaths = {"category", "region"})
    Optional<Store> findWithCategoryAndRegionById(Long id);

    @Query("""
            select s from Store s
            where (:keyword is null or lower(s.name) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or s.category.id = :categoryId)
              and (:regionId is null or s.region.id = :regionId)
              and (:minScore is null or s.scoreWeighted >= :minScore)
              and (:maxScore is null or s.scoreWeighted <= :maxScore)
            """)
    @EntityGraph(attributePaths = {"category", "region"})
    Page<Store> searchStores(@Param("keyword") String keyword,
                             @Param("categoryId") Long categoryId,
                             @Param("regionId") Long regionId,
                             @Param("minScore") BigDecimal minScore,
                             @Param("maxScore") BigDecimal maxScore,
                             Pageable pageable);
}
