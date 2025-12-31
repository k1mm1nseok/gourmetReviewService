package com.gourmet.review.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.MemberFollow;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.support.TestJpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(TestJpaAuditingConfig.class)
class MemberFollowRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberFollowRepository memberFollowRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void existsByFollowerIdAndFollowingId_returnsTrueWhenRelationExists() {
        Member follower = memberRepository.save(member("follower@example.com", "follower"));
        Member following = memberRepository.save(member("target@example.com", "target"));
        memberFollowRepository.save(MemberFollow.builder()
                .follower(follower)
                .following(following)
                .build());
        entityManager.flush();

        assertThat(memberFollowRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId()))
                .isTrue();
        assertThat(memberFollowRepository.existsByFollowerIdAndFollowingId(following.getId(), follower.getId()))
                .isFalse();
    }

    @Test
    void findByFollowerIdAndFollowingId_returnsRelationWhenPresent() {
        Member follower = memberRepository.save(member("follower@example.com", "follower"));
        Member following = memberRepository.save(member("target@example.com", "target"));
        MemberFollow relation = memberFollowRepository.save(MemberFollow.builder()
                .follower(follower)
                .following(following)
                .build());
        entityManager.flush();

        var result = memberFollowRepository.findByFollowerIdAndFollowingId(follower.getId(), following.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(relation.getId());
    }

    @Test
    void findByFollowingId_returnsPagedFollowers() {
        Member target = memberRepository.save(member("target@example.com", "target"));
        Member follower1 = memberRepository.save(member("user1@example.com", "user1"));
        Member follower2 = memberRepository.save(member("user2@example.com", "user2"));
        memberFollowRepository.saveAll(List.of(
                MemberFollow.builder().follower(follower1).following(target).build(),
                MemberFollow.builder().follower(follower2).following(target).build()
        ));
        entityManager.flush();

        var page = memberFollowRepository.findByFollowingId(target.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(follow -> follow.getFollower().getId())
                .containsExactlyInAnyOrder(follower1.getId(), follower2.getId());
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
