package com.codexpong.backend.realtime.scaleout;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/GatewaySessionManager.java
 * 설명:
 *   - 무상태 게이트웨이가 인증/세션 소유권 확인 후 올바른 샤드로 라우팅하도록 결정한다.
 *   - Redis 백플레인에 의존해 세션-샤드 매핑과 종료 컨텍스트를 관리한다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-gateway-shard-protocol.md
 *   - design/realtime/v1.1.0-handoff-and-reconnect-semantics.md
 * 변경 이력:
 *   - v1.1.0: 게이트웨이 세션 라우터 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public class GatewaySessionManager {

    private final GatewayShardBackplane backplane;
    private final TokenValidator tokenValidator;
    private final Duration shardExpiry;
    private final Clock clock;

    public GatewaySessionManager(GatewayShardBackplane backplane, TokenValidator tokenValidator, Duration shardExpiry, Clock clock) {
        this.backplane = backplane;
        this.tokenValidator = tokenValidator;
        this.shardExpiry = shardExpiry;
        this.clock = clock;
    }

    /**
     * 클라이언트의 신규 연결을 검증하고 샤드로 라우팅한다.
     * 종료 컨텍스트가 이미 존재하면 복구 대신 즉시 종료 알림을 반환한다.
     */
    public GatewaySessionResult openSession(String sessionId, String token, String traceId) {
        Instant now = clock.instant();
        Optional<TerminationContext> priorTermination = backplane.findTermination(sessionId);
        if (priorTermination.isPresent()) {
            return GatewaySessionResult.terminated(priorTermination.get());
        }
        if (!tokenValidator.isValid(token)) {
            TerminationContext context = new TerminationContext(sessionId, TerminationReason.AUTH_FAILURE, now, "invalid token");
            backplane.recordTermination(sessionId, context);
            return GatewaySessionResult.terminated(context);
        }
        Optional<String> assignedShard = chooseShard(sessionId, now);
        if (assignedShard.isEmpty()) {
            TerminationContext context = new TerminationContext(sessionId, TerminationReason.SHARD_UNAVAILABLE, now, "no shard available");
            backplane.recordTermination(sessionId, context);
            return GatewaySessionResult.terminated(context);
        }
        String shardId = assignedShard.get();
        backplane.mapSessionToShard(sessionId, shardId, now);
        backplane.appendRequest(shardId, new MessageEnvelope(
                UUID.randomUUID(),
                "SESSION_CONNECTED",
                now,
                sessionId,
                traceId,
                MapBuilder.of("token", token)
        ));
        return GatewaySessionResult.assigned(shardId);
    }

    /**
     * 재접속 시 종료 컨텍스트, 샤드 생존 여부를 확인한 뒤 연결을 허용하거나 거부한다.
     */
    public GatewaySessionResult reconnect(String sessionId, String token, String traceId) {
        Instant now = clock.instant();
        Optional<TerminationContext> priorTermination = backplane.findTermination(sessionId);
        if (priorTermination.isPresent()) {
            return GatewaySessionResult.terminated(priorTermination.get());
        }
        Optional<String> shard = backplane.sessionShard(sessionId);
        if (shard.isEmpty()) {
            TerminationContext context = new TerminationContext(sessionId, TerminationReason.SHARD_UNAVAILABLE, now, "session lost");
            backplane.recordTermination(sessionId, context);
            return GatewaySessionResult.terminated(context);
        }
        if (!tokenValidator.isValid(token)) {
            TerminationContext context = new TerminationContext(sessionId, TerminationReason.AUTH_FAILURE, now, "invalid token");
            backplane.recordTermination(sessionId, context);
            return GatewaySessionResult.terminated(context);
        }
        if (!backplane.activeShardIds(now, shardExpiry).contains(shard.get())) {
            TerminationContext context = new TerminationContext(sessionId, TerminationReason.SHARD_UNAVAILABLE, now, "shard missing");
            backplane.recordTermination(sessionId, context);
            return GatewaySessionResult.terminated(context);
        }
        backplane.appendRequest(shard.get(), new MessageEnvelope(
                UUID.randomUUID(),
                "SESSION_RECONNECTED",
                now,
                sessionId,
                traceId,
                MapBuilder.empty()
        ));
        return GatewaySessionResult.assigned(shard.get());
    }

    private Optional<String> chooseShard(String sessionId, Instant now) {
        Optional<String> existing = backplane.sessionShard(sessionId);
        if (existing.isPresent()) {
            return existing;
        }
        return backplane.activeShardIds(now, shardExpiry).stream().findFirst();
    }
}
