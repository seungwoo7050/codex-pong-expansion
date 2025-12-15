package com.codexpong.backend.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;

/**
 * [헬퍼] backend/src/main/java/com/codexpong/backend/observability/ObservabilityMetrics.java
 * 설명:
 *   - v1.2.0에서 요구하는 핵심 메트릭(WS 연결, 매치 시작, tick loop 지터)을 기록한다.
 *   - MeterRegistry가 없는 테스트에서도 안전하도록 no-op 인스턴스를 제공한다.
 * 버전: v1.2.0
 */
public class ObservabilityMetrics {

    private final Counter wsSuccess;
    private final Counter wsFailure;
    private final Counter matchStartSuccess;
    private final Counter matchStartFailure;
    private final io.micrometer.core.instrument.DistributionSummary tickJitter;

    public ObservabilityMetrics(MeterRegistry registry) {
        this.wsSuccess = Counter.builder("realtime.ws.connections")
                .description("웹소켓 연결 성공 횟수")
                .tag("status", "success")
                .register(registry);
        this.wsFailure = Counter.builder("realtime.ws.connections")
                .description("웹소켓 연결 실패 횟수")
                .tag("status", "failure")
                .register(registry);
        this.matchStartSuccess = Counter.builder("realtime.match.start")
                .description("매치 시작 성공 횟수")
                .tag("status", "success")
                .register(registry);
        this.matchStartFailure = Counter.builder("realtime.match.start")
                .description("매치 시작 실패 횟수")
                .tag("status", "failure")
                .register(registry);
        this.tickJitter = io.micrometer.core.instrument.DistributionSummary.builder("realtime.tick.jitter")
                .description("tick loop 지터(ms)")
                .baseUnit("milliseconds")
                .publishPercentileHistogram()
                .register(registry);
    }

    public static ObservabilityMetrics noop() {
        return new ObservabilityMetrics(new SimpleMeterRegistry());
    }

    public void recordWsConnection(boolean success) {
        if (success) {
            wsSuccess.increment();
        } else {
            wsFailure.increment();
        }
    }

    public void recordMatchStart(boolean success) {
        if (success) {
            matchStartSuccess.increment();
        } else {
            matchStartFailure.increment();
        }
    }

    public void recordTickJitter(Duration jitter) {
        tickJitter.record(jitter.toMillis());
    }
}
