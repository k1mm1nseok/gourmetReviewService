package com.gourmet.review.store.service;

import com.gourmet.review.common.exception.BusinessException;
import com.gourmet.review.common.exception.ErrorCode;
import com.gourmet.review.common.util.SecurityUtil;
import com.gourmet.review.domain.entity.Category;
import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.Region;
import com.gourmet.review.domain.entity.Store;
import com.gourmet.review.domain.entity.StoreAward;
import com.gourmet.review.domain.entity.StoreScrap;
import com.gourmet.review.domain.entity.Review;
import com.gourmet.review.domain.entity.ReviewImage;
import com.gourmet.review.domain.enums.ReviewStatus;
import com.gourmet.review.member.repository.MemberRepository;
import com.gourmet.review.review.repository.ReviewImageRepository;
import com.gourmet.review.review.repository.ReviewRepository;
import com.gourmet.review.store.dto.StoreDetailResponse;
import com.gourmet.review.store.dto.StoreRegisterRequest;
import com.gourmet.review.store.dto.StoreResponse;
import com.gourmet.review.store.dto.StoreSearchCondition;
import com.gourmet.review.store.repository.CategoryRepository;
import com.gourmet.review.store.repository.RegionRepository;
import com.gourmet.review.store.repository.StoreAwardRepository;
import com.gourmet.review.store.repository.StoreRepository;
import com.gourmet.review.store.repository.StoreScrapRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final int RECENT_REVIEW_LIMIT = 3;

    private final StoreRepository storeRepository;
    private final StoreScrapRepository storeScrapRepository;
    private final StoreAwardRepository storeAwardRepository;
    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;

    @Override
    @Transactional
    public StoreResponse register(StoreRegisterRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "카테고리를 찾을 수 없습니다."));
        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "지역 정보를 찾을 수 없습니다."));

        Store store = Store.builder()
                .name(request.getName())
                .category(category)
                .region(region)
                .address(request.getAddress())
                .detailedAddress(request.getDetailedAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .priceRangeLunch(request.getPriceRangeLunch())
                .priceRangeDinner(request.getPriceRangeDinner())
                .isParking(Boolean.TRUE.equals(request.getIsParking()))
                .build();

        Store saved = storeRepository.save(store);
        return toStoreResponse(saved);
    }

    @Override
    @Transactional
    public StoreDetailResponse getStoreDetail(Long storeId) {
        Store store = storeRepository.findWithCategoryAndRegionById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "가게 정보를 찾을 수 없습니다."));

        store.incrementViewCount();

        int collectedCount = Math.toIntExact(reviewRepository.countByStoreIdAndStatusIn(storeId,
                List.of(ReviewStatus.APPROVED, ReviewStatus.BLIND_HELD, ReviewStatus.PUBLIC)));

        boolean isBlind = Boolean.TRUE.equals(store.getIsBlind());

        List<StoreDetailResponse.AwardResponse> awards = isBlind
                ? List.of()
                : storeAwardRepository.findByStoreIdOrderByAwardYearDesc(storeId)
                .stream()
                .map(this::toAwardResponse)
                .toList();

        // 블라인드 가게에서도 최근 리뷰 텍스트는 보여준다(점수는 마스킹)
        List<StoreDetailResponse.RecentReviewResponse> recentReviews = getRecentReviews(storeId, isBlind);

        if (isBlind) {
            return StoreDetailResponse.builder()
                    .id(store.getId())
                    .name(store.getName())
                    .categoryName(store.getCategory().getName())
                    .regionName(store.getRegion().getName())
                    .address(store.getAddress())
                    .detailedAddress(store.getDetailedAddress())
                    .latitude(store.getLatitude())
                    .longitude(store.getLongitude())
                    .scoreWeighted(null)
                    .avgRating(null)
                    .isBlind(true)
                    .blindMessage("현재 " + collectedCount + "개의 리뷰가 수집되었습니다. 곧 평점이 공개됩니다.")
                    .reviewCount(store.getReviewCount())
                    .reviewCountValid(collectedCount)
                    .scrapCount(store.getScrapCount())
                    .viewCount(store.getViewCount())
                    .priceRangeLunch(store.getPriceRangeLunch())
                    .priceRangeDinner(store.getPriceRangeDinner())
                    .isParking(store.getIsParking())
                    .awards(awards)
                    .recentReviews(recentReviews)
                    .build();
        }

        // non-blind
        List<StoreDetailResponse.RecentReviewResponse> nonBlindRecentReviews = recentReviews;

        return StoreDetailResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .categoryName(store.getCategory().getName())
                .regionName(store.getRegion().getName())
                .address(store.getAddress())
                .detailedAddress(store.getDetailedAddress())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .scoreWeighted(store.getScoreWeighted())
                .avgRating(store.getAvgRating())
                .isBlind(store.getIsBlind())
                .reviewCount(store.getReviewCount())
                .reviewCountValid(store.getReviewCountValid())
                .scrapCount(store.getScrapCount())
                .viewCount(store.getViewCount())
                .priceRangeLunch(store.getPriceRangeLunch())
                .priceRangeDinner(store.getPriceRangeDinner())
                .isParking(store.getIsParking())
                .awards(awards)
                .recentReviews(nonBlindRecentReviews)
                .build();
    }

    @Override
    public Page<StoreResponse> search(StoreSearchCondition condition) {
        Pageable pageable = createPageable(condition);

        Page<Store> stores = storeRepository.searchStores(
                normalizeKeyword(condition.getKeyword()),
                condition.getCategoryId(),
                condition.getRegionId(),
                condition.getMinScore(),
                condition.getMaxScore(),
                pageable
        );

        return stores.map(this::toStoreResponse);
    }

    @Override
    @Transactional
    public void scrap(Long storeId) {
        Long memberId = getCurrentMemberIdOrThrow();
        if (storeScrapRepository.existsByStoreIdAndMemberId(storeId, memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 스크랩한 가게입니다.");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "가게 정보를 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원 정보를 찾을 수 없습니다."));

        storeScrapRepository.save(StoreScrap.builder()
                .store(store)
                .member(member)
                .build());

        store.incrementScrapCount();
    }

    @Override
    @Transactional
    public void unscrap(Long storeId) {
        Long memberId = getCurrentMemberIdOrThrow();
        StoreScrap scrap = storeScrapRepository.findByStoreIdAndMemberId(storeId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "스크랩 정보를 찾을 수 없습니다."));

        storeScrapRepository.delete(scrap);
        scrap.getStore().decrementScrapCount();
    }

    @Override
    public Page<StoreResponse> getMyScraps(Pageable pageable) {
        Long memberId = getCurrentMemberIdOrThrow();
        return storeScrapRepository.findByMemberId(memberId, pageable)
                .map(scrap -> toStoreResponse(scrap.getStore()));
    }

    private List<StoreDetailResponse.RecentReviewResponse> getRecentReviews(Long storeId, boolean hideScores) {
        Pageable pageable = PageRequest.of(0, RECENT_REVIEW_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Review> reviews = reviewRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, ReviewStatus.PUBLIC, pageable);
        if (reviews.isEmpty()) {
            return List.of();
        }

        List<Long> reviewIds = reviews.stream()
                .map(Review::getId)
                .toList();
        Map<Long, List<String>> imageMap = reviewImageRepository.findByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(image -> image.getReview().getId(),
                        Collectors.mapping(ReviewImage::getImageUrl, Collectors.toList())));

        return reviews.stream()
                .map(review -> StoreDetailResponse.RecentReviewResponse.builder()
                        .id(review.getId())
                        .memberNickname(review.getMember().getNickname())
                        .memberTier(review.getMember().getTier())
                        .scoreCalculated(hideScores ? null : review.getScoreCalculated())
                        .scoreTaste(hideScores ? null : review.getScoreTaste())
                        .scoreValue(hideScores ? null : review.getScoreValue())
                        .scoreAmbiance(hideScores ? null : review.getScoreAmbiance())
                        .scoreService(hideScores ? null : review.getScoreService())
                        .content(review.getContent())
                        .images(imageMap.getOrDefault(review.getId(), List.of()))
                        .helpfulCount(review.getHelpfulCount())
                        .createdAt(review.getCreatedAt())
                        .build())
                .toList();
    }

    private StoreResponse toStoreResponse(Store store) {
        BigDecimal scoreWeighted = Boolean.TRUE.equals(store.getIsBlind()) ? null : store.getScoreWeighted();
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .categoryName(store.getCategory().getName())
                .regionName(store.getRegion().getName())
                .address(store.getAddress())
                .scoreWeighted(scoreWeighted)
                .isBlind(store.getIsBlind())
                .reviewCountValid(store.getReviewCountValid())
                .scrapCount(store.getScrapCount())
                .thumbnailImage(null) // TODO: 대표 이미지 정책 확정 후 적용
                .createdAt(store.getCreatedAt())
                .build();
    }

    private StoreDetailResponse.AwardResponse toAwardResponse(StoreAward award) {
        return StoreDetailResponse.AwardResponse.builder()
                .awardName(award.getAwardName())
                .awardGrade(award.getAwardGrade())
                .awardYear(award.getAwardYear())
                .build();
    }

    private Pageable createPageable(StoreSearchCondition condition) {
        int page = condition.getPage() != null ? condition.getPage() : 0;
        int size = condition.getSize() != null ? condition.getSize() : 20;

        Sort sort = createSort(condition.getSortBy(), condition.getSortDirection());
        return PageRequest.of(page, size, sort);
    }

    private Sort createSort(String sortBy, String sortDirection) {
        String property = switch (sortBy == null ? "" : sortBy) {
            case "review_count" -> "reviewCountValid";
            case "created_at" -> "createdAt";
            case "score_weighted", "" -> "scoreWeighted";
            default -> sortBy;
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private Long getCurrentMemberIdOrThrow() {
        return SecurityUtil.getCurrentMemberId()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
