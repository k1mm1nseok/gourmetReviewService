package com.gourmet.review.review.service;

import com.gourmet.review.domain.entity.Store;
import java.util.Collection;

/**
 * 스토어 점수/카운트 재계산을 담당하는 내부 서비스.
 * ReviewServiceImpl에 있던 계산 로직을 배치/정책에서도 재사용하기 위해 분리.
 */
public interface ReviewScoreService {
    void recalculateStoreScores(Store store);

    /**
     * 여러 스토어 점수/카운트를 한 번에 재계산한다.
     * - storeIds는 중복/NULL이 섞여도 되며, 내부에서 정리 후 처리한다.
     */
    void recalculateStoreScoresByStoreIds(Collection<Long> storeIds);
}
