package com.gourmet.review.member.service;

import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.member.dto.MemberLoginRequest;
import com.gourmet.review.member.dto.MemberLoginResponse;
import com.gourmet.review.member.dto.MemberProfileResponse;
import com.gourmet.review.member.dto.MemberProfileUpdateRequest;
import com.gourmet.review.member.dto.MemberRegisterRequest;
import com.gourmet.review.member.dto.MemberResponse;
import com.gourmet.review.member.dto.MemberSimpleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberService {

    MemberResponse registerMember(MemberRegisterRequest request);

    MemberLoginResponse login(MemberLoginRequest request);

    MemberProfileResponse getMyProfile();

    MemberResponse updateMyProfile(MemberProfileUpdateRequest request);

    void follow(Long targetMemberId);

    void unfollow(Long targetMemberId);

    Page<MemberSimpleResponse> getFollowers(Long memberId, Pageable pageable);

    /**
     * 관리자(ADMIN)가 특정 회원의 tier를 강제로 변경한다.
     * 변경 시 정책 소급 반영(해당 회원이 작성한 리뷰가 반영된 store 점수 재계산 등)이 수행되어야 한다.
     */
    MemberResponse adminUpdateMemberTier(Long memberId, MemberTier newTier);

    /**
     * 관리자(ADMIN)가 특정 회원의 role을 강제로 변경한다.
     * (운영 필요 시 사용. 현재는 인증 체계가 dev basic auth라 role/권한은 내부 로직에서 별도로 확인한다.)
     */
    MemberResponse adminUpdateMemberRole(Long memberId, MemberRole newRole);
}
