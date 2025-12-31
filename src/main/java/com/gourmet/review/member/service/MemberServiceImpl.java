package com.gourmet.review.member.service;

import com.gourmet.review.common.exception.BusinessException;
import com.gourmet.review.common.exception.ErrorCode;
import com.gourmet.review.common.util.SecurityUtil;
import com.gourmet.review.review.service.ReviewPolicyJobService;
import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.MemberFollow;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.member.dto.MemberLoginRequest;
import com.gourmet.review.member.dto.MemberLoginResponse;
import com.gourmet.review.member.dto.MemberProfileResponse;
import com.gourmet.review.member.dto.MemberProfileUpdateRequest;
import com.gourmet.review.member.dto.MemberRegisterRequest;
import com.gourmet.review.member.dto.MemberResponse;
import com.gourmet.review.member.dto.MemberSimpleResponse;
import com.gourmet.review.member.repository.MemberFollowRepository;
import com.gourmet.review.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberFollowRepository memberFollowRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReviewPolicyJobService reviewPolicyJobService;

    @Override
    @Transactional
    public MemberResponse registerMember(MemberRegisterRequest request) {
        validateDuplicateEmail(request.getEmail());
        validateDuplicateNickname(request.getNickname());

        Member member = Member.builder()
                .email(request.getEmail())
                .nickname(request.getNickname())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(MemberRole.USER)
                .tier(MemberTier.BRONZE)
                .build();

        Member saved = memberRepository.save(member);
        return toMemberResponse(saved);
    }

    @Override
    public MemberLoginResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return MemberLoginResponse.builder()
                .accessToken("DUMMY-TOKEN") // TODO: JWT 구현 예정
                .member(toSimpleResponse(member))
                .build();
    }

    @Override
    public MemberProfileResponse getMyProfile() {
        Member member = getCurrentMember();
        return toProfileResponse(member);
    }

    @Override
    @Transactional
    public MemberResponse updateMyProfile(MemberProfileUpdateRequest request) {
        Member member = getCurrentMember();
        if (!member.getNickname().equals(request.getNickname())) {
            validateDuplicateNickname(request.getNickname());
            member.updateNickname(request.getNickname());
        }
        return toMemberResponse(member);
    }

    @Override
    @Transactional
    public void follow(Long targetMemberId) {
        Long currentMemberId = getCurrentMemberIdOrThrow();
        Member follower = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "팔로우 대상 회원을 찾을 수 없습니다."));

        if (follower.getId().equals(target.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "자기 자신은 팔로우할 수 없습니다."); // TODO: self follow 금지 정책 고도화
        }
        if (memberFollowRepository.existsByFollowerIdAndFollowingId(follower.getId(), target.getId())) {
            throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS, "이미 팔로우한 회원입니다."); // TODO: 중복 팔로우 검증 로직 확정
        }

        memberFollowRepository.save(MemberFollow.builder()
                .follower(follower)
                .following(target)
                .build());
    }

    @Override
    @Transactional
    public void unfollow(Long targetMemberId) {
        Long currentMemberId = getCurrentMemberIdOrThrow();
        MemberFollow relation = memberFollowRepository.findByFollowerIdAndFollowingId(currentMemberId, targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLLOW_NOT_FOUND, "팔로우 관계가 존재하지 않습니다."));
        memberFollowRepository.delete(relation);
    }

    @Override
    public Page<MemberSimpleResponse> getFollowers(Long memberId, Pageable pageable) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원 정보를 찾을 수 없습니다."));

        return memberFollowRepository.findByFollowingId(memberId, pageable)
                .map(follow -> toSimpleResponse(follow.getFollower()));
    }

    @Override
    @Transactional
    public MemberResponse adminUpdateMemberTier(Long memberId, MemberTier newTier) {
        requireAdmin();
        if (memberId == null || newTier == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "memberId 또는 tier가 비었습니다.");
        }

        Member target = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "대상 회원을 찾을 수 없습니다."));

        MemberTier oldTier = target.getTier();
        if (oldTier == newTier) {
            return toMemberResponse(target);
        }

        // BLACK/GOURMET 포함 모든 tier 수동 변경 허용
        target.forceUpdateTier(newTier);
        reviewPolicyJobService.handleMemberTierChanged(target.getId(), oldTier, newTier);

        return toMemberResponse(target);
    }

    @Override
    @Transactional
    public MemberResponse adminUpdateMemberRole(Long memberId, MemberRole newRole) {
        requireAdmin();
        if (memberId == null || newRole == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "memberId 또는 role이 비었습니다.");
        }

        Long currentMemberId = getCurrentMemberIdOrThrow();
        if (currentMemberId.equals(memberId)) {
            // 운영 안전장치: 자기 자신 권한 변경은 금지(실수로 ADMIN 권한을 잃는 것을 방지)
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "자기 자신의 role은 변경할 수 없습니다.");
        }

        Member target = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "대상 회원을 찾을 수 없습니다."));

        target.forceUpdateRole(newRole);
        return toMemberResponse(target);
    }

    private void requireAdmin() {
        Member current = getCurrentMember();
        if (current.getRole() != MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.ADMIN_REQUIRED, "관리자 권한이 필요합니다.");
        }
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 이메일입니다.");
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 닉네임입니다.");
        }
    }

    private Member getCurrentMember() {
        Long memberId = getCurrentMemberIdOrThrow();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
    }

    private Long getCurrentMemberIdOrThrow() {
        return SecurityUtil.getCurrentMemberId()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    private MemberResponse toMemberResponse(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .tier(member.getTier())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .build();
    }

    private MemberProfileResponse toProfileResponse(Member member) {
        return MemberProfileResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .tier(member.getTier())
                .role(member.getRole())
                .createdAt(member.getCreatedAt())
                .reviewCount(member.getReviewCount())
                .helpfulCount(member.getHelpfulCount())
                .violationCount(member.getViolationCount())
                .lastReviewAt(member.getLastReviewAt())
                .isActive(member.isActive())
                .build();
    }

    private MemberSimpleResponse toSimpleResponse(Member member) {
        return MemberSimpleResponse.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .tier(member.getTier())
                .build();
    }
}
