package com.codexpong.backend.realtime.scaleout;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/MessageEnvelope.java
 * 설명:
 *   - 게이트웨이와 샤드 간에 주고받는 공통 메시지 래퍼를 표현한다.
 *   - v1.1.0 실시간 수평 확장 설계에서 모든 메시지는 표준 필드를 포함해야 한다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-gateway-shard-protocol.md
 *   - design/realtime/v1.1.0-protocol.md
 * 변경 이력:
 *   - v1.1.0: 기본 envelope 정의 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public record MessageEnvelope(
        UUID messageId,
        String type,
        Instant occurredAt,
        String sessionId,
        String traceId,
        Map<String, Object> payload
) {
    /**
     * 표준 필드가 null 이거나 비어 있는지 검증한다.
     */
    public MessageEnvelope {
        if (messageId == null || type == null || occurredAt == null || sessionId == null || traceId == null) {
            throw new IllegalArgumentException("메시지 필드는 null 이 될 수 없습니다");
        }
    }
}
