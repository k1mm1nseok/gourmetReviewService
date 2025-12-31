package com.gourmet.review.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.support.TestJpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(TestJpaAuditingConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void existsByEmail_returnsTrueWhenMemberExists() {
        memberRepository.save(member("user@example.com", "user1"));
        entityManager.flush();

        assertThat(memberRepository.existsByEmail("user@example.com")).isTrue();
        assertThat(memberRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void existsByNickname_returnsTrueWhenMemberExists() {
        memberRepository.save(member("user@example.com", "user1"));
        entityManager.flush();

        assertThat(memberRepository.existsByNickname("user1")).isTrue();
        assertThat(memberRepository.existsByNickname("missing")).isFalse();
    }

    @Test
    void findByEmail_returnsMemberWhenPresent() {
        memberRepository.save(member("user@example.com", "user1"));
        entityManager.flush();

        var result = memberRepository.findByEmail("user@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("user1");
    }

    @Test
    void findByEmail_returnsEmptyWhenMissing() {
        assertThat(memberRepository.findByEmail("missing@example.com")).isEmpty();
    }

    private Member member(String email, String nickname) {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .password("encoded-pass")
                .role(MemberRole.USER)
                .tier(MemberTier.BRONZE)
                .build();
    }
}
