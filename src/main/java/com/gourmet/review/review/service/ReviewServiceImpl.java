package com.gourmet.review.review.service;

import com.gourmet.review.common.exception.BusinessException;
import com.gourmet.review.common.exception.ErrorCode;
import com.gourmet.review.common.util.SecurityUtil;
import com.gourmet.review.domain.entity.Member;
import com.gourmet.review.domain.entity.MemberStoreVisit;
import com.gourmet.review.domain.entity.Review;
import com.gourmet.review.domain.entity.ReviewHelpful;
import com.gourmet.review.domain.entity.ReviewImage;
import com.gourmet.review.domain.entity.Store;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.domain.enums.ReviewStatus;
import com.gourmet.review.member.repository.MemberRepository;
import com.gourmet.review.review.dto.ReviewCreateRequest;
import com.gourmet.review.review.dto.ReviewDetailResponse;
import com.gourmet.review.review.dto.ReviewModerationResponse;
import com.gourmet.review.review.dto.ReviewRejectRequest;
import com.gourmet.review.review.dto.ReviewResponse;
import com.gourmet.review.review.dto.ReviewUpdateRequest;
import com.gourmet.review.review.repository.MemberStoreVisitRepository;
import com.gourmet.review.review.repository.ReviewHelpfulRepository;
import com.gourmet.review.review.repository.ReviewImageRepository;
import com.gourmet.review.review.repository.ReviewRepository;
import com.gourmet.review.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    // NOTE: 점수 계산(베이지안 평균/가중치/감가상각 등)은 ReviewScoreService로 분리되어 있다.

    // p0: 극단 점수 리뷰 쿨다운
    // NOTE: 쿨다운은 '등록 차단'이 아니라 'PENDING 유지 후 배치 승인' 정책이므로,
    //       여기서는 중복 차단 로직을 두지 않고 ReviewPolicyJobService.processCooldownExpirations()에서 처리한다.

    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final MemberStoreVisitRepository memberStoreVisitRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final ReviewScoreService reviewScoreService;
    private final ReviewPolicyJobService policyJobService;

    @Override
    @Transactional
    public ReviewResponse registerReview(ReviewCreateRequest request) {
        Member member = getCurrentMemberOrThrow();
        if (!Boolean.TRUE.equals(member.getIsPhoneVerified())) {
            throw new BusinessException(ErrorCode.PHONE_VERIFICATION_REQUIRED, "리뷰 작성 전 휴대폰 인증이 필요합니다.");
        }
        MemberTier oldTier = member.getTier();

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "가게 정보를 찾을 수 없습니다."));

        // p0: 쿨다운은 등록 차단이 아니라 PENDING 유지 정책(배치에서 만료 처리)

        Review review = Review.builder()
                .store(store)
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .partySize(request.getPartySize())
                .scoreTaste(request.getScoreTaste())
                .scoreValue(request.getScoreValue())
                .scoreService(request.getScoreService())
                .scoreAmbiance(request.getScoreAmbiance())
                .visitDate(request.getVisitDate())
                .build();

        Review saved = reviewRepository.save(review);
        store.incrementReviewCount();
        member.incrementReviewCount();

        MemberTier newTier = member.getTier();
        if (oldTier != null && newTier != null && oldTier != newTier) {
            policyJobService.handleMemberTierChanged(member.getId(), oldTier, newTier);
        }

        saveImages(saved, request.getImages());
        return toReviewResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request) {
        Review review = getReviewOrThrow(reviewId);
        validateReviewOwnerOrAdmin(review);
        review.updateReview(request.getTitle(), request.getPartySize(), request.getContent(),
                request.getScoreTaste(), request.getScoreValue(), request.getScoreAmbiance(),
                request.getScoreService());

        if (review.getStatus() == ReviewStatus.PUBLIC) {
            recalculateStoreScores(review.getStore());
        }
        return toReviewResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = getReviewOrThrow(reviewId);
        validateReviewOwnerOrAdmin(review);
        boolean wasPublic = review.getStatus() == ReviewStatus.PUBLIC;
        Store store = review.getStore();

        reviewImageRepository.deleteByReviewId(reviewId);
        reviewHelpfulRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);
        store.decrementReviewCount();
        if (wasPublic) {
            recalculateStoreScores(store);
        }
    }

    @Override
    public ReviewDetailResponse getReview(Long reviewId) {
        Review review = reviewRepository.findWithStoreAndMemberById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰 정보를 찾을 수 없습니다."));
        List<ReviewImage> images = reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(reviewId);
        // 블라인드 가게인 경우 점수를 마스킹해야 한다.
        boolean hideScores = Boolean.TRUE.equals(review.getStore().getIsBlind());
        return toReviewDetailResponse(review, images, hideScores);
    }

    @Override
    public Page<ReviewResponse> getStoreReviews(Long storeId, Pageable pageable) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "가게 정보를 찾을 수 없습니다."));

        // 블라인드 상태에서는 점수/평점은 숨기고(=null), 텍스트/이미지/정보만 노출한다.
        // 데이터 수집 단계(APPROVED/BLIND_HELD)는 공개 노출하지 않고, PUBLIC만 노출한다.
        boolean hideScores = Boolean.TRUE.equals(store.getIsBlind());

        return reviewRepository.findByStoreIdAndStatus(storeId, ReviewStatus.PUBLIC, pageable)
                .map(review -> toStoreReviewResponse(review, hideScores));
    }

    private ReviewResponse toStoreReviewResponse(Review review, boolean hideScores) {
        ReviewResponse base = toReviewResponse(review);
        if (!hideScores) {
            return base;
        }
        // 점수만 가리고(=null), 나머지 필드는 유지
        return ReviewResponse.builder()
                .id(base.getId())
                .storeId(base.getStoreId())
                .storeName(base.getStoreName())
                .scoreTaste(null)
                .scoreService(null)
                .scoreAmbiance(null)
                .scoreValue(null)
                .scoreCalculated(null)
                .content(base.getContent())
                .visitCount(base.getVisitCount())
                .status(base.getStatus())
                .helpfulCount(base.getHelpfulCount())
                .isHelpfulByMe(base.getIsHelpfulByMe())
                .createdAt(base.getCreatedAt())
                .partySize(base.getPartySize())
                .build();
    }

    @Override
    public Page<ReviewResponse> getMyReviews(Pageable pageable) {
        Long memberId = getCurrentMemberIdOrThrow();
        return reviewRepository.findByMemberId(memberId, pageable)
                .map(this::toReviewResponse);
    }

    @Override
    public Page<ReviewModerationResponse> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByStatus(ReviewStatus.PENDING, pageable)
                .map(this::toReviewModerationResponse);
    }

    @Override
    @Transactional
    public void markHelpful(Long reviewId) {
        Review review = getReviewOrThrow(reviewId);
        Long memberId = getCurrentMemberIdOrThrow();
        if (reviewHelpfulRepository.existsByReviewIdAndMemberId(reviewId, memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 도움됨을 누른 리뷰입니다.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원 정보를 찾을 수 없습니다."));

        // 도움됨을 누른 '현재 회원'의 tier는 이 로직에서 변하지 않는다.
        // tier 변동 가능성이 있는 주체는 '리뷰 작성자'이므로, 해당 멤버의 tier 변경을 감지한다.
        Member author = review.getMember();
        MemberTier oldTier = author != null ? author.getTier() : null;

        reviewHelpfulRepository.save(ReviewHelpful.builder()
                .review(review)
                .member(member)
                .build());
        review.incrementHelpfulCount();
        review.getMember().incrementHelpfulCount();

        if (author != null) {
            MemberTier newTier = author.getTier();
            if (oldTier != null && newTier != null && oldTier != newTier) {
                policyJobService.handleMemberTierChanged(author.getId(), oldTier, newTier);
            }
        }
    }

    @Override
    @Transactional
    public void unmarkHelpful(Long reviewId) {
        Review review = getReviewOrThrow(reviewId);
        Long memberId = getCurrentMemberIdOrThrow();
        ReviewHelpful helpful = reviewHelpfulRepository.findByReviewIdAndMemberId(reviewId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "도움됨 정보를 찾을 수 없습니다."));
        reviewHelpfulRepository.delete(helpful);
        review.decrementHelpfulCount();
        review.getMember().decrementHelpfulCount();
    }

    @Override
    @Transactional
    public void approveReview(Long reviewId) {
        Review review = getReviewOrThrow(reviewId);
        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "검수 대기 상태가 아닙니다.");
        }
        review.approve();
        Store store = review.getStore();

        long approvedCount = reviewRepository.countByStoreIdAndStatusIn(store.getId(),
                List.of(ReviewStatus.APPROVED, ReviewStatus.BLIND_HELD, ReviewStatus.PUBLIC));
        if (approvedCount < 5) {
            review.holdForBlind();
            return;
        }

        List<Review> publishTargets = reviewRepository.findByStoreIdAndStatusIn(store.getId(),
                List.of(ReviewStatus.APPROVED, ReviewStatus.BLIND_HELD));
        for (Review target : publishTargets) {
            if (target.publish()) {
                applyVisitCount(target);
            }
        }
        recalculateStoreScores(store);
    }

    @Override
    @Transactional
    public void rejectReview(Long reviewId, ReviewRejectRequest request) {
        Review review = getReviewOrThrow(reviewId);
        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "검수 대기 상태가 아닙니다.");
        }
        review.reject(request.getAdminComment());
    }

    private Review getReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "리뷰 정보를 찾을 수 없습니다."));
    }

    private ReviewResponse toReviewResponse(Review review){
        Boolean isHelpfulByMe = resolveIsHelpfulByMe(review.getId());
        return ReviewResponse.builder()
                .id(review.getId())
                .storeId(review.getStore().getId())
                .storeName(review.getStore().getName())
                .scoreTaste(review.getScoreTaste())
                .scoreService(review.getScoreService())
                .scoreAmbiance(review.getScoreAmbiance())
                .scoreValue(review.getScoreValue())
                .scoreCalculated(review.getScoreCalculated())
                .content(review.getContent())
                .visitCount(review.getVisitCount())
                .status(review.getStatus())
                .helpfulCount(review.getHelpfulCount())
                .isHelpfulByMe(isHelpfulByMe)
                .createdAt(review.getCreatedAt())
                .partySize(review.getPartySize())
                .build();
    }

    private ReviewDetailResponse toReviewDetailResponse(Review review, List<ReviewImage> images, boolean hideScores){
        Store store = review.getStore();
        Member member = review.getMember();
        Boolean isHelpfulByMe = resolveIsHelpfulByMe(review.getId());
        List<ReviewDetailResponse.ImageResponse> imageResponses = images.stream()
                .map(image -> ReviewDetailResponse.ImageResponse.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .displayOrder(image.getDisplayOrder())
                        .build())
                .toList();

        // 블라인드이면 점수 관련 필드를 null로 채운다. 리뷰 엔티티의 점수 필드는 BigDecimal이다.
        BigDecimal scoreTaste = hideScores ? null : review.getScoreTaste();
        BigDecimal scoreService = hideScores ? null : review.getScoreService();
        BigDecimal scoreAmbiance = hideScores ? null : review.getScoreAmbiance();
        BigDecimal scoreValue = hideScores ? null : review.getScoreValue();
        BigDecimal scoreCalculated = hideScores ? null : review.getScoreCalculated();

        return ReviewDetailResponse.builder()
                .id(review.getId())
                .store(ReviewDetailResponse.StoreSummary.builder()
                        .id(store.getId())
                        .name(store.getName())
                        .build())
                .member(ReviewDetailResponse.MemberSummary.builder()
                        .id(member.getId())
                        .nickname(member.getNickname())
                        .tier(member.getTier())
                        .build())
                .scoreTaste(scoreTaste)
                .scoreService(scoreService)
                .scoreAmbiance(scoreAmbiance)
                .scoreValue(scoreValue)
                .scoreCalculated(scoreCalculated)
                .content(review.getContent())
                .visitDate(review.getVisitDate())
                .visitCount(review.getVisitCount())
                .isHelpfulByMe(isHelpfulByMe)
                .status(review.getStatus())
                .helpfulCount(review.getHelpfulCount())
                .images(imageResponses)
                .comments(List.of())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private ReviewModerationResponse toReviewModerationResponse(Review review) {
        return ReviewModerationResponse.builder()
                .id(review.getId())
                .storeName(review.getStore().getName())
                .memberNickname(review.getMember().getNickname())
                .memberTier(review.getMember().getTier())
                .scoreCalculated(review.getScoreCalculated())
                .content(review.getContent())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private void saveImages(Review review, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        List<ReviewImage> reviewImages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            reviewImages.add(ReviewImage.builder()
                    .review(review)
                    .imageUrl(images.get(i))
                    .displayOrder(i)
                    .build());
        }
        reviewImageRepository.saveAll(reviewImages);
    }

    private Member getCurrentMemberOrThrow() {
        Long memberId = getCurrentMemberIdOrThrow();
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
    }

    private Long getCurrentMemberIdOrThrow() {
        return SecurityUtil.getCurrentMemberId()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    private void validateReviewOwnerOrAdmin(Review review) {
        Member current = getCurrentMemberOrThrow();
        if (!review.getMember().getId().equals(current.getId()) && current.getRole() != MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
    }

    private void applyVisitCount(Review review) {
        Member member = review.getMember();
        Store store = review.getStore();
        MemberStoreVisit visit = memberStoreVisitRepository.findByMemberIdAndStoreId(member.getId(), store.getId())
                .orElseGet(() -> memberStoreVisitRepository.save(MemberStoreVisit.builder()
                        .member(member)
                        .store(store)
                        .build()));
        int visitCount = visit.incrementVisitCount();
        review.updateVisitCount(visitCount);
    }

    private void recalculateStoreScores(Store store) {
        reviewScoreService.recalculateStoreScores(store);
    }

    /**
     * 현재 로그인한 회원이 해당 리뷰에 '도움이 됨'을 눌렀는지 여부.
     * - 비로그인 상태이면 null 반환(프론트에서 버튼 비활성/숨김 등 처리 가능)
     */
    private Boolean resolveIsHelpfulByMe(Long reviewId) {
        if (reviewId == null) {
            return null;
        }
        return SecurityUtil.getCurrentMemberId()
                .map(memberId -> reviewHelpfulRepository.existsByReviewIdAndMemberId(reviewId, memberId))
                .orElse(null);
    }
}
