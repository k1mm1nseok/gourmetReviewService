package com.gourmet.review.store.service;

import com.gourmet.review.store.dto.CategoryResponse;
import com.gourmet.review.store.dto.RegionResponse;
import java.util.List;

public interface StoreLookupService {

    List<CategoryResponse> getCategories();

    List<RegionResponse> getRegions();
}

