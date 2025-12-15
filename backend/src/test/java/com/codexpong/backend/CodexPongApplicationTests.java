package com.codexpong.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * [테스트] backend/src/test/java/com/codexpong/backend/CodexPongApplicationTests.java
 * 설명:
 *   - 기본 컨텍스트 로딩이 정상적으로 수행되는지 확인한다.
 * 버전: v0.1.0
 * 관련 설계문서:
 *   - design/backend/v0.1.0-core-skeleton-and-health.md
 */
@SpringBootTest
@ActiveProfiles("test")
class CodexPongApplicationTests {

    @Test
    void contextLoads() {
    }
}
