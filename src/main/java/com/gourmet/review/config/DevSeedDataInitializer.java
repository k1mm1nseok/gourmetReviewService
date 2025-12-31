package com.gourmet.review.config;

import com.gourmet.review.domain.entity.Category;
import com.gourmet.review.domain.entity.Region;
import com.gourmet.review.store.repository.CategoryRepository;
import com.gourmet.review.store.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DevSeedDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final RegionRepository regionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategoriesIfEmpty();
        seedRegionsIfEmpty();
    }

    private void seedCategoriesIfEmpty() {
        if (categoryRepository.count() > 0) {
            return;
        }
        categoryRepository.save(Category.builder()
                .name("양식")
                .depth(0)
                .build());
    }

    private void seedRegionsIfEmpty() {
        if (regionRepository.count() > 0) {
            return;
        }
        regionRepository.save(Region.builder()
                .name("서울특별시")
                .depth(0)
                .build());
    }
}

