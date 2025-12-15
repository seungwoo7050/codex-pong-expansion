package com.codexpong.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

/**
 * [헬스 프로브] backend/src/test/java/com/codexpong/backend/health/HealthActuatorTest.java
 * 설명:
 *   - 쿠버네티스 프로브에서 사용할 액추에이터 헬스 엔드포인트가 공개되고 정상 응답을 반환하는지 검증한다.
 *   - 종료 시 그레이스풀 셧다운 설정이 적용됐는지 확인해 롤링 업데이트 안정성을 보장한다.
 * 버전: v1.4.0
 * 관련 설계문서:
 *   - design/infra/v1.4.0-k8s-iac-gitops.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthActuatorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void readinessActuatorIsPublicAndHealthy() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void livenessActuatorIsPublicAndHealthy() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void gracefulShutdownSettingsAreEnabled() {
        assertThat(environment.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("30s");
    }
}
