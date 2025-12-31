package com.gourmet.review.member.repository;

import com.gourmet.review.domain.entity.MemberFollow;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberFollowRepository extends JpaRepository<MemberFollow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<MemberFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Page<MemberFollow> findByFollowingId(Long followingId, Pageable pageable);
}
