package com.codexpong.backend.health;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/health/HealthController.java
 * 설명:
 *   - 백엔드 기동 여부와 기본 메타 정보를 제공하는 헬스체크 엔드포인트를 노출한다.
 *   - 외부 오케스트레이션 및 모니터링에서 사용하기 위한 최소 정보를 반환한다.
 * 버전: v0.1.0
 * 관련 설계문서:
 *   - design/backend/v0.1.0-core-skeleton-and-health.md
 * 변경 이력:
 *   - v0.1.0: 헬스체크 API 최초 추가
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 설명:
     *   - 애플리케이션 상태를 간단히 확인할 수 있도록 현재 시간과 이름을 반환한다.
     * 출력:
     *   - status, service, now 필드를 포함한 맵을 반환한다.
     */
    @GetMapping
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "codex-pong-backend");
        response.put("now", LocalDateTime.now().format(FORMATTER));
        return response;
    }
}
