package com.gourmet.review.member.service;

import com.gourmet.review.common.exception.BusinessException;
import com.gourmet.review.common.util.SecurityUtil;
import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.member.repository.MemberRepository;
import com.gourmet.review.review.service.ReviewPolicyJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class MemberServiceAdminUpdateTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @MockBean
    ReviewPolicyJobService reviewPolicyJobService;

    Member admin;
    Member target;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();

        admin = memberRepository.save(Member.builder()
                .email("admin@test.com")
                .nickname("admin")
                .password("pw")
                .role(MemberRole.ADMIN)
                .tier(MemberTier.GOLD)
                .build());

        target = memberRepository.save(Member.builder()
                .email("user@test.com")
                .nickname("user")
                .password("pw")
                .role(MemberRole.USER)
                .tier(MemberTier.SILVER)
                .build());
    }

    @Test
    void adminUpdateTier_triggers_handleMemberTierChanged() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(java.util.Optional.of(admin.getId()));

            memberService.adminUpdateMemberTier(target.getId(), MemberTier.BLACK);

            verify(reviewPolicyJobService, times(1))
                    .handleMemberTierChanged(eq(target.getId()), eq(MemberTier.SILVER), eq(MemberTier.BLACK));
        }
    }

    @Test
    void nonAdmin_cannot_updateTier() {
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::getCurrentMemberId).thenReturn(java.util.Optional.of(target.getId()));

            assertThrows(BusinessException.class,
                    () -> memberService.adminUpdateMemberTier(target.getId(), MemberTier.BLACK));
        }
    }
}
