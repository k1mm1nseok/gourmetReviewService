package com.gourmet.review.store.service;

import com.gourmet.review.domain.entity.Category;
import com.gourmet.review.domain.entity.Region;
import com.gourmet.review.store.dto.CategoryResponse;
import com.gourmet.review.store.dto.RegionResponse;
import com.gourmet.review.store.repository.CategoryRepository;
import com.gourmet.review.store.repository.RegionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoreLookupServiceImpl implements StoreLookupService {

    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;

    @Override
    public List<CategoryResponse> getCategories() {
        List<Category> categories = categoryRepository.findAllByOrderByDepthAscIdAsc();
        return buildCategoryTree(categories);
    }

    @Override
    public List<RegionResponse> getRegions() {
        List<Region> regions = regionRepository.findAllByOrderByDepthAscIdAsc();
        return buildRegionTree(regions);
    }

    private List<CategoryResponse> buildCategoryTree(List<Category> categories) {
        Map<Long, CategoryResponse> nodes = new LinkedHashMap<>();
        for (Category category : categories) {
            nodes.put(category.getId(), CategoryResponse.from(category));
        }

        List<CategoryResponse> roots = new ArrayList<>();
        for (Category category : categories) {
            CategoryResponse node = nodes.get(category.getId());
            if (category.getParent() == null) {
                roots.add(node);
                continue;
            }

            CategoryResponse parent = nodes.get(category.getParent().getId());
            if (parent == null) {
                roots.add(node);
                continue;
            }
            parent.getChildren().add(node);
        }

        return roots;
    }

    private List<RegionResponse> buildRegionTree(List<Region> regions) {
        Map<Long, RegionResponse> nodes = new LinkedHashMap<>();
        for (Region region : regions) {
            nodes.put(region.getId(), RegionResponse.from(region));
        }

        List<RegionResponse> roots = new ArrayList<>();
        for (Region region : regions) {
            RegionResponse node = nodes.get(region.getId());
            if (region.getParent() == null) {
                roots.add(node);
                continue;
            }

            RegionResponse parent = nodes.get(region.getParent().getId());
            if (parent == null) {
                roots.add(node);
                continue;
            }
            parent.getChildren().add(node);
        }

        return roots;
    }
}

