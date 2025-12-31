package com.gourmet.review.review.service;

import com.gourmet.review.domain.entity.Category;
import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.Region;
import com.gourmet.review.domain.entity.Review;
import com.gourmet.review.domain.entity.Store;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.domain.enums.ReviewStatus;
import com.gourmet.review.member.repository.MemberRepository;
import com.gourmet.review.review.repository.MemberStoreVisitRepository;
import com.gourmet.review.review.repository.ReviewRepository;
import com.gourmet.review.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewExtremeScoreCooldownP0Test {

    @Test
    void cooldownTarget_bronzeAndExtremeScore_shouldBeTrue() throws Exception {
        // given
        Member member = Member.builder()
                .email("cool@test.com")
                .nickname("cool")
                .password("pw")
                .role(MemberRole.USER)
                .tier(MemberTier.BRONZE)
                .build();

        Store dummyStore = Store.builder()
                .id(1L)
                .name("s")
                .address("addr")
                .latitude(new BigDecimal("37.0"))
                .longitude(new BigDecimal("127.0"))
                .category(Category.builder().id(1L).name("c").depth(1).build())
                .region(Region.builder().id(1L).name("r").depth(1).build())
                .build();

        Review review = Review.builder()
                .store(dummyStore)
                .member(member)
                .content("c")
                .partySize(1)
                .scoreTaste(new BigDecimal("1.0"))
                .scoreValue(new BigDecimal("1.0"))
                .scoreAmbiance(new BigDecimal("1.0"))
                .scoreService(new BigDecimal("1.0"))
                .scoreCalculated(new BigDecimal("1.0"))
                .status(ReviewStatus.PENDING)
                .visitDate(LocalDate.now())
                .build();

        ReviewRepository reviewRepository = Mockito.mock(ReviewRepository.class);
        MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        MemberStoreVisitRepository memberStoreVisitRepository = Mockito.mock(MemberStoreVisitRepository.class);
        ReviewScoreService reviewScoreService = Mockito.mock(ReviewScoreService.class);

        ReviewPolicyJobServiceImpl impl = new ReviewPolicyJobServiceImpl(
                reviewRepository,
                memberRepository,
                storeRepository,
                memberStoreVisitRepository,
                reviewScoreService,
                java.time.Clock.systemUTC()
        );

        // when
        java.lang.reflect.Method m = ReviewPolicyJobServiceImpl.class.getDeclaredMethod("isCooldownTarget", Review.class);
        m.setAccessible(true);
        boolean isTarget = (boolean) m.invoke(impl, review);

        // then
        assertThat(isTarget).isTrue();
    }
}
