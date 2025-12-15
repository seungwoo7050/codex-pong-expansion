package com.codexpong.backend;

import com.codexpong.backend.job.JobExportProperties;
import com.codexpong.backend.job.JobQueueProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * [부트스트랩] backend/src/main/java/com/codexpong/backend/CodexPongApplication.java
 * 설명:
 *   - Spring Boot 애플리케이션의 진입점이다.
 *   - v0.12.0 기준 리플레이 내보내기 잡 큐/워커 연동 설정까지 포함해 모든 주요 모듈을 함께 구동한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.1.0-core-skeleton-and-health.md
 *   - design/realtime/v0.1.0-basic-websocket-wiring.md
 *   - design/backend/v0.2.0-auth-and-profile.md
 *   - design/backend/v0.5.0-friends-and-blocks.md
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 * 변경 이력:
 *   - v0.1.0: 프로젝트 부트스트랩 생성
 *   - v0.2.0: 인증/프로필 모듈 구동 항목 반영
 *   - v0.5.0: 소셜 도메인(WebSocket 포함) 구동 명시
 *   - v0.12.0: 잡 큐/워커 설정 바인딩 추가
 */
@SpringBootApplication
@EnableConfigurationProperties({JobQueueProperties.class, JobExportProperties.class})
public class CodexPongApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodexPongApplication.class, args);
    }
}
