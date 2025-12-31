package com.gourmet.review.review.service;

import com.gourmet.review.common.exception.BusinessException;
import com.gourmet.review.common.exception.ErrorCode;
import com.gourmet.review.domain.entity.Category;
import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.Region;
import com.gourmet.review.domain.entity.Store;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.member.repository.MemberRepository;
import com.gourmet.review.review.dto.ReviewCreateRequest;
import com.gourmet.review.store.repository.CategoryRepository;
import com.gourmet.review.store.repository.RegionRepository;
import com.gourmet.review.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
class ReviewPhoneVerifiedP0Test {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    void registerReview_requiresPhoneVerified() {
        Member member = memberRepository.save(Member.builder()
                .email("p0-phone@test.com")
                .nickname("p0-phone")
                .password("pw")
                .role(MemberRole.USER)
                .tier(MemberTier.BRONZE)
                .isPhoneVerified(false)
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("cat")
                .depth(0)
                .build());

        Region region = regionRepository.save(Region.builder()
                .name("reg")
                .depth(0)
                .build());

        Store store = storeRepository.save(Store.builder()
                .name("store")
                .address("addr")
                .detailedAddress("detail")
                .latitude(new BigDecimal("37.12345678"))
                .longitude(new BigDecimal("127.12345678"))
                .category(category)
                .region(region)
                .build());

        // 테스트 환경에서 SecurityUtil은 principal을 숫자 문자열로 파싱할 수 있으므로,
        // username으로 memberId(숫자)를 주입해 인증을 흉내낸다.
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        String.valueOf(member.getId()), "N/A", java.util.List.of())
        );

        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .storeId(store.getId())
                .title("t")
                .content("c")
                .partySize(2)
                .scoreTaste(new BigDecimal("3.0"))
                .scoreService(new BigDecimal("3.0"))
                .scoreAmbiance(new BigDecimal("3.0"))
                .scoreValue(new BigDecimal("3.0"))
                .visitDate(LocalDate.now())
                .images(java.util.List.of())
                .build();

        assertThatThrownBy(() -> reviewService.registerReview(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PHONE_VERIFICATION_REQUIRED);
    }
}
