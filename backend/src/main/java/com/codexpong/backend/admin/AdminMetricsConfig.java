package com.codexpong.backend.admin;

import com.codexpong.backend.game.GameResultRepository;
import com.codexpong.backend.game.service.GameRoomService;
import com.codexpong.backend.user.repository.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/admin/AdminMetricsConfig.java
 * 설명:
 *   - Prometheus 노출을 위해 사용자 수, 누적 경기 수, 활성 방/관전자 수를 게이지로 등록한다.
 *   - v0.9.0 모니터링 대시보드에서 활용된다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.9.0-admin-and-ops.md
 */
@Configuration
public class AdminMetricsConfig {

    public AdminMetricsConfig(MeterRegistry registry, UserRepository userRepository,
            GameResultRepository gameResultRepository, GameRoomService gameRoomService) {
        Gauge.builder("codexpong_users_total", userRepository::count)
                .description("등록된 사용자 수")
                .register(registry);
        Gauge.builder("codexpong_matches_total", gameResultRepository::count)
                .description("누적 경기 기록 수")
                .register(registry);
        Gauge.builder("codexpong_games_active", gameRoomService::activeRoomCount)
                .description("메모리 상 활성 경기 방 수")
                .register(registry);
        Gauge.builder("codexpong_spectators_active", gameRoomService::totalSpectatorCount)
                .description("실시간 관전자 세션 수")
                .register(registry);
    }
}
