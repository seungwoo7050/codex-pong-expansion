package com.codexpong.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexpong.backend.realtime.scaleout.DeterministicClock;
import com.codexpong.backend.realtime.scaleout.GameSessionShard;
import com.codexpong.backend.realtime.scaleout.GatewaySessionManager;
import com.codexpong.backend.realtime.scaleout.GatewaySessionResult;
import com.codexpong.backend.realtime.scaleout.InMemoryBackplane;
import com.codexpong.backend.realtime.scaleout.MapBuilder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * [통합테스트] backend/src/test/java/com/codexpong/backend/observability/TraceCorrelationIntegrationTest.java
 * 설명:
 *   - API(HTTP 필터)에서 생성한 traceId가 게이트웨이와 샤드로 전달될 때 하나의 트레이스로 묶이는지 검증한다.
 *   - 인메모리 Span 익스포터와 메트릭 레지스트리를 사용해 외부 의존성 없이 관찰한다.
 * 버전: v1.2.0
 */
class TraceCorrelationIntegrationTest {

    private InMemorySpanExporter spanExporter;
    private SdkTracerProvider tracerProvider;
    private Tracer tracer;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .setIdGenerator(IdGenerator.random())
                .build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
        tracer = openTelemetry.getTracer("test-observability");
        meterRegistry = new SimpleMeterRegistry();
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
        spanExporter.reset();
        meterRegistry.close();
    }

    @Test
    void traceIdFlowsAcrossGatewayAndShard() {
        DeterministicClock clock = new DeterministicClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        InMemoryBackplane backplane = new InMemoryBackplane();
        ObservabilityMetrics metrics = new ObservabilityMetrics(meterRegistry);

        GatewaySessionManager manager = new GatewaySessionManager(backplane, token -> true, Duration.ofSeconds(5), clock, metrics, tracer);
        GameSessionShard shard = new GameSessionShard("shard-int", backplane, clock, Duration.ofSeconds(1), 2, metrics, tracer);

        Span apiSpan = tracer.spanBuilder("HTTP POST /api/matchmaking")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        String apiTraceId = apiSpan.getSpanContext().getTraceId();
        try (Scope ignored = apiSpan.makeCurrent()) {
            GatewaySessionResult result = manager.openSession("sess-int", "ok", apiTraceId);
            assertThat(result.shardId()).contains("shard-int");

            shard.tick();
            backplane.appendRequest("shard-int", new com.codexpong.backend.realtime.scaleout.MessageEnvelope(
                    UUID.randomUUID(),
                    "START_GAME",
                    clock.instant(),
                    "sess-int",
                    apiTraceId,
                    MapBuilder.empty()
            ));
            clock.advanceSeconds(1);
            shard.tick();
            clock.advanceSeconds(1);
            shard.tick();
        } finally {
            apiSpan.end();
        }

        List<String> traceIds = spanExporter.getFinishedSpanItems().stream()
                .map(span -> span.getTraceId())
                .distinct()
                .toList();

        assertThat(spanExporter.getFinishedSpanItems()).isNotEmpty();
        assertThat(traceIds).containsOnly(apiTraceId);
        assertThat(spanExporter.getFinishedSpanItems()
                .stream()
                .anyMatch(span -> span.getName().contains("shard.start_game"))).isTrue();
        var jitterSummary = meterRegistry.find("realtime.tick.jitter").summary();
        assertThat(jitterSummary).isNotNull();
        assertThat(jitterSummary.count()).isGreaterThan(0);
    }
}
