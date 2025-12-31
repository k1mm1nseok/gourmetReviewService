package com.gourmet.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Gourmet Review Service Application
 * Spring Boot 3.x + Java 21 + PostgreSQL
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class GourmetReviewServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GourmetReviewServiceApplication.class, args);
    }
}
