package com.gourmet.review.support;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * 테스트에서 추가 AutoConfiguration/Bean을 주입해야 할 때 사용하는 placeholder 입니다.
 * (JPA Auditing은 애플리케이션 메인에서 활성화되고 있으므로 테스트에서 별도 활성화하지 않습니다.)
 */
@TestConfiguration
public class TestJpaAuditingConfig {
}
