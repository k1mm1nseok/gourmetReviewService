package com.gourmet.review.store.repository;

import com.gourmet.review.domain.entity.StoreAward;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreAwardRepository extends JpaRepository<StoreAward, Long> {

    List<StoreAward> findByStoreIdOrderByAwardYearDesc(Long storeId);
}

