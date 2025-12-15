package com.codexpong.backend.realtime.scaleout;

import java.util.Optional;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/GatewaySessionResult.java
 * 설명:
 *   - 게이트웨이 핸드셰이크/재접속 결과를 표현한다.
 *   - 성공 시 샤드 ID를, 실패 시 종료 컨텍스트를 포함한다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-gateway-shard-protocol.md
 * 변경 이력:
 *   - v1.1.0: 결과 객체 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public record GatewaySessionResult(Optional<String> shardId, Optional<TerminationContext> terminationContext) {

    public static GatewaySessionResult assigned(String shardId) {
        return new GatewaySessionResult(Optional.of(shardId), Optional.empty());
    }

    public static GatewaySessionResult terminated(TerminationContext context) {
        return new GatewaySessionResult(Optional.empty(), Optional.of(context));
    }
}
