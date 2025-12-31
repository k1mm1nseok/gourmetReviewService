package com.gourmet.review.store.controller;

import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.store.dto.StoreDetailResponse;
import com.gourmet.review.store.service.StoreService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoreBlindRecentReviewsP0Test {

    MockMvc mockMvc;
    StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = Mockito.mock(StoreService.class);
        StoreController controller = new StoreController(storeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void blindStoreDetail_shouldExposeRecentReviewTextButHideScores() throws Exception {
        Long storeId = 1L;

        StoreDetailResponse.RecentReviewResponse recent = StoreDetailResponse.RecentReviewResponse.builder()
                .id(10L)
                .memberNickname("nick")
                .memberTier(MemberTier.BRONZE)
                .scoreCalculated(null)
                .scoreTaste(null)
                .scoreValue(null)
                .scoreAmbiance(null)
                .scoreService(null)
                .content("텍스트는 보여야 함")
                .images(List.of())
                .helpfulCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        StoreDetailResponse response = StoreDetailResponse.builder()
                .id(storeId)
                .name("블라인드가게")
                .categoryName("카테고리")
                .regionName("지역")
                .address("주소")
                .scoreWeighted(null)
                .avgRating(null)
                .isBlind(true)
                .blindMessage("현재 1개의 리뷰가 수집되었습니다. 곧 평점이 공개됩니다.")
                .reviewCountValid(1)
                .recentReviews(List.of(recent))
                .build();

        Mockito.when(storeService.getStoreDetail(storeId)).thenReturn(response);

        mockMvc.perform(get("/api/stores/{storeId}", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isBlind").value(true))
                .andExpect(jsonPath("$.data.recentReviews[0].content").value("텍스트는 보여야 함"))
                .andExpect(jsonPath("$.data.recentReviews[0].scoreCalculated").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.recentReviews[0].scoreTaste").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.recentReviews[0].scoreValue").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.recentReviews[0].scoreAmbiance").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.recentReviews[0].scoreService").value(org.hamcrest.Matchers.nullValue()));
    }
}
