package com.gourmet.review.review.repository;

import com.gourmet.review.domain.entity.MemberStoreVisit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberStoreVisitRepository extends JpaRepository<MemberStoreVisit, Long> {

    Optional<MemberStoreVisit> findByMemberIdAndStoreId(Long memberId, Long storeId);
}
