package com.codexpong.backend.realtime.scaleout;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.codexpong.backend.observability.ObservabilityMetrics;
import com.codexpong.backend.observability.TraceSupport;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/GameSessionShard.java
 * 설명:
 *   - 권위 있는 게임 세션을 관리하는 샤드 역할을 단순화한 시뮬레이터이다.
 *   - Redis 백플레인 요청을 읽어 상태 전이를 수행하고, 결정적 종료 정보를 기록한다.
 * 버전: v1.2.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-architecture.md
 *   - design/realtime/v1.1.0-handoff-and-reconnect-semantics.md
 * 변경 이력:
 *   - v1.1.0: 기본 샤드 시뮬레이터 추가
 *   - v1.2.0: traceId 스팬/지터 메트릭 기록 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public class GameSessionShard {

    private final String shardId;
    private final GatewayShardBackplane backplane;
    private final Clock clock;
    private final Duration heartbeatInterval;
    private final int maxTicksBeforeTerminate;
    private final Map<String, SessionState> sessions = new HashMap<>();
    private Instant nextHeartbeat;
    private Instant lastTickAt;
    private final ObservabilityMetrics metrics;
    private final TraceSupport traceSupport;

    public GameSessionShard(String shardId, GatewayShardBackplane backplane, Clock clock, Duration heartbeatInterval, int maxTicksBeforeTerminate) {
        this(shardId, backplane, clock, heartbeatInterval, maxTicksBeforeTerminate, ObservabilityMetrics.noop(),
                new TraceSupport(GlobalOpenTelemetry.getTracer("realtime-shard")));
    }

    public GameSessionShard(String shardId, GatewayShardBackplane backplane, Clock clock, Duration heartbeatInterval,
                            int maxTicksBeforeTerminate, ObservabilityMetrics metrics, Tracer tracer) {
        this(shardId, backplane, clock, heartbeatInterval, maxTicksBeforeTerminate, metrics, new TraceSupport(tracer));
    }

    public GameSessionShard(String shardId, GatewayShardBackplane backplane, Clock clock, Duration heartbeatInterval,
                            int maxTicksBeforeTerminate, ObservabilityMetrics metrics, TraceSupport traceSupport) {
        this.shardId = shardId;
        this.backplane = backplane;
        this.clock = clock;
        this.heartbeatInterval = heartbeatInterval;
        this.maxTicksBeforeTerminate = maxTicksBeforeTerminate;
        this.nextHeartbeat = clock.instant();
        this.metrics = metrics;
        this.traceSupport = traceSupport;
        backplane.registerShard(shardId, clock.instant());
    }

    /**
     * 주기적으로 호출되어 하트비트 갱신과 요청 처리, 상태 전이를 수행한다.
     */
    public void tick() {
        Instant now = clock.instant();
        recordJitter(now);
        if (!now.isBefore(nextHeartbeat)) {
            backplane.heartbeat(shardId, now);
            nextHeartbeat = now.plus(heartbeatInterval);
        }
        backplane.drainRequests(shardId).forEach(envelope -> handleRequest(envelope, now));
        sessions.values().forEach(state -> state.advance());
        sessions.values().stream()
                .filter(SessionState::shouldTerminate)
                .forEach(state -> terminate(state, TerminationReason.NORMAL_COMPLETION, "ticks reached"));
    }

    private void handleRequest(MessageEnvelope envelope, Instant now) {
        Span span = traceSupport.startSpan("shard." + envelope.type().toLowerCase(), envelope.traceId(), SpanKind.CONSUMER);
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("session.id", envelope.sessionId());
            span.setAttribute("message.type", envelope.type());
            SessionState state = sessions.computeIfAbsent(envelope.sessionId(), key -> new SessionState(key));
            state.lastTraceId = envelope.traceId();
            if ("SESSION_CONNECTED".equals(envelope.type())) {
                state.markConnected();
                backplane.appendResponse(envelope.sessionId(), ack("SESSION_ACK", envelope.sessionId(), envelope.traceId(), now));
                return;
            }
            if ("SESSION_RECONNECTED".equals(envelope.type())) {
                backplane.appendResponse(envelope.sessionId(), ack("RECONNECTED", envelope.sessionId(), envelope.traceId(), now));
                return;
            }
            if ("START_GAME".equals(envelope.type())) {
                state.markPlaying();
                backplane.appendResponse(envelope.sessionId(), ack("PLAY_STARTED", envelope.sessionId(), envelope.traceId(), now));
                metrics.recordMatchStart(true);
            }
        }
        span.end();
    }

    private void terminate(SessionState state, TerminationReason reason, String detail) {
        if (state.isTerminated()) {
            return;
        }
        state.markTerminated();
        TerminationContext context = new TerminationContext(state.sessionId, reason, clock.instant(), detail);
        backplane.recordTermination(state.sessionId, context);
        backplane.appendResponse(state.sessionId, new MessageEnvelope(
                UUID.randomUUID(),
                "TERMINATED",
                clock.instant(),
                state.sessionId,
                state.lastTraceId,
                MapBuilder.of("reason", reason.name())
        ));
    }

    /**
     * 강제 실패 시나리오를 표현하기 위해 샤드 상태를 즉시 종료 처리한다.
     */
    public void forceFailSession(String sessionId, String traceId, TerminationReason reason, String detail) {
        SessionState state = sessions.computeIfAbsent(sessionId, SessionState::new);
        state.lastTraceId = traceSupport.normalizeTraceId(traceId);
        terminate(state, reason, detail);
    }

    private MessageEnvelope ack(String type, String sessionId, String traceId, Instant now) {
        return new MessageEnvelope(UUID.randomUUID(), type, now, sessionId, traceId, MapBuilder.empty());
    }

    private void recordJitter(Instant now) {
        if (lastTickAt == null) {
            lastTickAt = now;
            return;
        }
        Duration delta = Duration.between(lastTickAt, now);
        Duration jitter = delta.minus(heartbeatInterval);
        if (jitter.isNegative()) {
            jitter = jitter.negated();
        }
        metrics.recordTickJitter(jitter);
        lastTickAt = now;
    }

    /**
     * 세션 상태를 단순 관리하는 내부 클래스.
     */
    private class SessionState {
        private final String sessionId;
        private SessionPhase phase = SessionPhase.WAITING;
        private int ticks = 0;
        private String lastTraceId = "";

        private SessionState(String sessionId) {
            this.sessionId = sessionId;
        }

        void markConnected() {
            this.phase = SessionPhase.WAITING;
        }

        void markPlaying() {
            this.phase = SessionPhase.PLAYING;
        }

        void markTerminated() {
            this.phase = SessionPhase.TERMINATED;
        }

        void advance() {
            if (phase == SessionPhase.PLAYING) {
                ticks += 1;
            }
        }

        boolean shouldTerminate() {
            return phase == SessionPhase.PLAYING && ticks >=  maxTicksBeforeTerminate;
        }

        boolean isTerminated() {
            return phase == SessionPhase.TERMINATED;
        }
    }
}
