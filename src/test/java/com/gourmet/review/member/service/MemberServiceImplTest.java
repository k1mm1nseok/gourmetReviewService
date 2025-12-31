package com.gourmet.review.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gourmet.review.common.exception.BusinessException;
import com.gourmet.review.common.exception.ErrorCode;
import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.MemberFollow;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.member.dto.MemberLoginRequest;
import com.gourmet.review.member.dto.MemberProfileResponse;
import com.gourmet.review.member.dto.MemberProfileUpdateRequest;
import com.gourmet.review.member.dto.MemberRegisterRequest;
import com.gourmet.review.member.dto.MemberResponse;
import com.gourmet.review.member.repository.MemberFollowRepository;
import com.gourmet.review.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class   MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberFollowRepository memberFollowRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberServiceImpl memberService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerMember_throwsWhenEmailDuplicated() {
        MemberRegisterRequest request = MemberRegisterRequest.builder()
                .email("dup@example.com")
                .password("password123")
                .nickname("dup")
                .build();

        when(memberRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> memberService.registerMember(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void registerMember_throwsWhenNicknameDuplicated() {
        MemberRegisterRequest request = MemberRegisterRequest.builder()
                .email("user@example.com")
                .password("password123")
                .nickname("dup")
                .build();

        given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(memberRepository.existsByNickname(request.getNickname())).willReturn(true);

        assertThatThrownBy(() -> memberService.registerMember(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);

        then(memberRepository).should(never()).save(any(Member.class));
//        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void registerMember_encodesPasswordAndReturnsResponse() {
        MemberRegisterRequest request = MemberRegisterRequest.builder()
                .email("user@example.com")
                .password("password123")
                .nickname("tester")
                .build();

        given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encoded-pass");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            return Member.builder()
                    .id(1L)
                    .email(member.getEmail())
                    .nickname(member.getNickname())
                    .password(member.getPassword())
                    .role(member.getRole())
                    .tier(member.getTier())
                    .build();
        });

        MemberResponse response = memberService.registerMember(request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        then(memberRepository).should().save(memberCaptor.capture());
//        verify(memberRepository).save(memberCaptor.capture());
        Member saved = memberCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getNickname()).isEqualTo("tester");
        assertThat(saved.getPassword()).isEqualTo("encoded-pass");
        assertThat(saved.getRole()).isEqualTo(MemberRole.USER);
        assertThat(saved.getTier()).isEqualTo(MemberTier.BRONZE);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getNickname()).isEqualTo("tester");
        assertThat(response.getRole()).isEqualTo(MemberRole.USER);
        assertThat(response.getTier()).isEqualTo(MemberTier.BRONZE);
    }

    @Test
    void login_throwsWhenMemberNotFound() {
        MemberLoginRequest request = MemberLoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        when(memberRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    @Test
    void login_throwsWhenPasswordMismatch() {
        MemberLoginRequest request = MemberLoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        Member member = member(1L, "user@example.com", "tester");
        when(memberRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.getPassword(), member.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_returnsTokenAndMember() {
        MemberLoginRequest request = MemberLoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        Member member = member(1L, "user@example.com", "tester");
        when(memberRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.getPassword(), member.getPassword())).thenReturn(true);

        var response = memberService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("DUMMY-TOKEN");
        assertThat(response.getMember().getId()).isEqualTo(1L);
        assertThat(response.getMember().getNickname()).isEqualTo("tester");
        assertThat(response.getMember().getTier()).isEqualTo(MemberTier.BRONZE);
    }

    @Test
    void getMyProfile_throwsWhenUnauthorized() {
        assertThatThrownBy(() -> memberService.getMyProfile())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void getMyProfile_returnsProfile() {
        authenticate(1L);
        Member member = member(1L, "user@example.com", "tester");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberProfileResponse response = memberService.getMyProfile();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getNickname()).isEqualTo("tester");
        assertThat(response.getTier()).isEqualTo(MemberTier.BRONZE);
        assertThat(response.getRole()).isEqualTo(MemberRole.USER);
        assertThat(response.getReviewCount()).isEqualTo(member.getReviewCount());
        assertThat(response.getHelpfulCount()).isEqualTo(member.getHelpfulCount());
        assertThat(response.getViolationCount()).isEqualTo(member.getViolationCount());
        assertThat(response.getIsActive()).isEqualTo(member.isActive());
    }

    @Test
    void updateMyProfile_skipsDuplicateCheckWhenNicknameUnchanged() {
        authenticate(1L);
        Member member = member(1L, "user@example.com", "tester");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberProfileUpdateRequest request = MemberProfileUpdateRequest.builder()
                .nickname("tester")
                .build();

        MemberResponse response = memberService.updateMyProfile(request);

        verify(memberRepository, never()).existsByNickname(any(String.class));
        assertThat(response.getNickname()).isEqualTo("tester");
    }

    @Test
    void updateMyProfile_throwsWhenNicknameDuplicated() {
        authenticate(1L);
        Member member = member(1L, "user@example.com", "tester");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("newNick")).thenReturn(true);

        MemberProfileUpdateRequest request = MemberProfileUpdateRequest.builder()
                .nickname("newNick")
                .build();

        assertThatThrownBy(() -> memberService.updateMyProfile(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    void updateMyProfile_updatesNickname() {
        authenticate(1L);
        Member member = member(1L, "user@example.com", "tester");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("newNick")).thenReturn(false);

        MemberProfileUpdateRequest request = MemberProfileUpdateRequest.builder()
                .nickname("newNick")
                .build();

        MemberResponse response = memberService.updateMyProfile(request);

        assertThat(response.getNickname()).isEqualTo("newNick");
        assertThat(member.getNickname()).isEqualTo("newNick");
    }

    @Test
    void follow_throwsWhenSelfFollow() {
        authenticate(1L);
        Member member = member(1L, "user@example.com", "tester");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.follow(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void follow_throwsWhenAlreadyFollowing() {
        authenticate(1L);
        Member follower = member(1L, "user@example.com", "tester");
        Member target = member(2L, "target@example.com", "target");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(follower));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
        when(memberFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> memberService.follow(2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FOLLOW_ALREADY_EXISTS);
    }

    @Test
    void follow_savesRelation() {
        authenticate(1L);
        Member follower = member(1L, "user@example.com", "tester");
        Member target = member(2L, "target@example.com", "target");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(follower));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
        when(memberFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);

        memberService.follow(2L);

        ArgumentCaptor<MemberFollow> followCaptor = ArgumentCaptor.forClass(MemberFollow.class);
        verify(memberFollowRepository).save(followCaptor.capture());
        MemberFollow saved = followCaptor.getValue();
        assertThat(saved.getFollower()).isEqualTo(follower);
        assertThat(saved.getFollowing()).isEqualTo(target);
    }

    @Test
    void unfollow_throwsWhenRelationNotFound() {
        authenticate(1L);
        when(memberFollowRepository.findByFollowerIdAndFollowingId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.unfollow(2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FOLLOW_NOT_FOUND);
    }

    @Test
    void unfollow_deletesRelation() {
        authenticate(1L);
        Member follower = member(1L, "user@example.com", "tester");
        Member target = member(2L, "target@example.com", "target");
        MemberFollow relation = MemberFollow.builder()
                .follower(follower)
                .following(target)
                .build();
        when(memberFollowRepository.findByFollowerIdAndFollowingId(1L, 2L))
                .thenReturn(Optional.of(relation));

        memberService.unfollow(2L);

        verify(memberFollowRepository).delete(relation);
    }

    @Test
    void getFollowers_throwsWhenMemberNotFound() {
        when(memberRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getFollowers(2L, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    @Test
    void getFollowers_mapsFollowersToSimpleResponse() {
        Member target = member(2L, "target@example.com", "target");
        when(memberRepository.findById(2L)).thenReturn(Optional.of(target));

        Member follower1 = member(1L, "user1@example.com", "user1");
        Member follower2 = member(3L, "user2@example.com", "user2");
        MemberFollow relation1 = MemberFollow.builder()
                .follower(follower1)
                .following(target)
                .build();
        MemberFollow relation2 = MemberFollow.builder()
                .follower(follower2)
                .following(target)
                .build();

        Page<MemberFollow> page = new PageImpl<>(
                List.of(relation1, relation2),
                PageRequest.of(0, 10),
                2
        );
        when(memberFollowRepository.findByFollowingId(2L, PageRequest.of(0, 10))).thenReturn(page);

        Page<?> response = memberService.getFollowers(2L, PageRequest.of(0, 10));

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).extracting("id")
                .containsExactly(1L, 3L);
    }

//    @Test
//    private void get

    private void authenticate(Long memberId) {
        var authentication = new UsernamePasswordAuthenticationToken(memberId, null, List.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private Member member(Long id, String email, String nickname) {
        return Member.builder()
                .id(id)
                .email(email)
                .nickname(nickname)
                .password("encoded-pass")
                .role(MemberRole.USER)
                .tier(MemberTier.BRONZE)
                .reviewCount(3)
                .helpfulCount(2)
                .violationCount(1)
                .lastReviewAt(LocalDateTime.now())
                .build();
    }
}
