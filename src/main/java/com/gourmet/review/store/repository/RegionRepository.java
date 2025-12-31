package com.gourmet.review.store.repository;

import com.gourmet.review.domain.entity.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RegionRepository extends JpaRepository<Region, Long> {

    @EntityGraph(attributePaths = {"parent"})
    List<Region> findAllByOrderByDepthAscIdAsc();
}
