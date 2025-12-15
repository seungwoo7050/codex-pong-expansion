package com.codexpong.backend.realtime.scaleout;

import java.time.Instant;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/TerminationContext.java
 * 설명:
 *   - 결정적 종료 정책(B)에서 세션이 종료될 때 기록되는 근거 데이터를 표현한다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-handoff-and-reconnect-semantics.md
 * 변경 이력:
 *   - v1.1.0: 종료 컨텍스트 정의 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public record TerminationContext(
        String sessionId,
        TerminationReason reason,
        Instant occurredAt,
        String detail
) {
    public TerminationContext {
        if (sessionId == null || reason == null || occurredAt == null) {
            throw new IllegalArgumentException("종료 컨텍스트 필수 필드가 비어 있습니다");
        }
    }
}
