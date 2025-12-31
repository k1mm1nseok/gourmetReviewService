package com.gourmet.review.store.service;

import com.gourmet.review.store.dto.StoreDetailResponse;
import com.gourmet.review.store.dto.StoreRegisterRequest;
import com.gourmet.review.store.dto.StoreResponse;
import com.gourmet.review.store.dto.StoreSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoreService {

    StoreResponse register(StoreRegisterRequest request);

    StoreDetailResponse getStoreDetail(Long storeId);

    Page<StoreResponse> search(StoreSearchCondition condition);

    void scrap(Long storeId);

    void unscrap(Long storeId);

    Page<StoreResponse> getMyScraps(Pageable pageable);
}

