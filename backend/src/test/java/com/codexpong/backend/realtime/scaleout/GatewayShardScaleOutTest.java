package com.codexpong.backend.realtime.scaleout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * [테스트] backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 * 설명:
 *   - v1.1.0 실시간 수평 확장 요구사항에 맞춰 게이트웨이-샤드 흐름을 결정적으로 검증한다.
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-gateway-shard-protocol.md
 *   - design/realtime/v1.1.0-handoff-and-reconnect-semantics.md
 */
class GatewayShardScaleOutTest {

    private DeterministicClock clock;
    private InMemoryBackplane backplane;

    @BeforeEach
    void setUp() {
        clock = new DeterministicClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        backplane = new InMemoryBackplane();
    }

    @Test
    void authFailureTerminatesSession() {
        GatewaySessionManager manager = new GatewaySessionManager(backplane, token -> false, Duration.ofSeconds(5), clock);

        GatewaySessionResult result = manager.openSession("s1", "bad", "trace-1");

        assertThat(result.terminationContext()).isPresent();
        assertThat(result.terminationContext().get().reason()).isEqualTo(TerminationReason.AUTH_FAILURE);
    }

    @Test
    void reconnectUsesExistingShard() {
        backplane.registerShard("shard-a", clock.instant());
        GatewaySessionManager manager = new GatewaySessionManager(backplane, token -> true, Duration.ofSeconds(5), clock);

        GatewaySessionResult first = manager.openSession("s2", "ok", "trace-2");
        assertThat(first.shardId()).contains("shard-a");

        clock.advanceSeconds(2);
        GatewaySessionResult reconnect = manager.reconnect("s2", "ok", "trace-3");

        assertThat(reconnect.shardId()).contains("shard-a");
        List<MessageEnvelope> requests = backplane.drainRequests("shard-a");
        assertThat(requests.stream().map(MessageEnvelope::type)).contains("SESSION_RECONNECTED");
    }

    @Test
    void shardKillTriggersDeterministicTerminate() {
        backplane.registerShard("shard-b", clock.instant());
        GatewaySessionManager manager = new GatewaySessionManager(backplane, token -> true, Duration.ofSeconds(3), clock);

        GatewaySessionResult first = manager.openSession("s3", "ok", "trace-4");
        assertThat(first.shardId()).contains("shard-b");

        clock.advanceSeconds(10);
        GatewaySessionResult reconnect = manager.reconnect("s3", "ok", "trace-5");

        assertThat(reconnect.terminationContext()).isPresent();
        assertThat(reconnect.terminationContext().get().reason()).isEqualTo(TerminationReason.SHARD_UNAVAILABLE);
    }

    @Test
    void deterministicSimulationTerminatesAfterTicks() {
        backplane.registerShard("shard-c", clock.instant());
        GatewaySessionManager manager = new GatewaySessionManager(backplane, token -> true, Duration.ofSeconds(5), clock);
        GameSessionShard shard = new GameSessionShard("shard-c", backplane, clock, Duration.ofSeconds(1), 3);

        GatewaySessionResult first = manager.openSession("s4", "ok", "trace-6");
        assertThat(first.shardId()).contains("shard-c");

        shard.tick();
        backplane.appendRequest("shard-c", new MessageEnvelope(UUID.randomUUID(), "START_GAME", clock.instant(), "s4", "trace-6", MapBuilder.empty()));

        shard.tick();
        clock.advanceSeconds(1);
        shard.tick();
        clock.advanceSeconds(1);
        shard.tick();

        List<MessageEnvelope> responses = backplane.drainResponses("s4");
        assertThat(responses.stream().map(MessageEnvelope::type)).contains("TERMINATED");
        assertThat(backplane.findTermination("s4").get().reason()).isEqualTo(TerminationReason.NORMAL_COMPLETION);
    }
}
