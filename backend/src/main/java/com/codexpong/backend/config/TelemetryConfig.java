package com.codexpong.backend.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.IdGenerator;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/config/TelemetryConfig.java
 * 설명:
 *   - v1.2.0 관측성 요구사항에 맞춰 OpenTelemetry 트레이서를 구성한다.
 *   - OTLP 엔드포인트가 지정되지 않은 경우 로그 익스포터로 폴백해 로컬에서도 추적값을 확인할 수 있게 한다.
 *   - HTTP 서버 타이머에 p95 퍼센타일을 고정 노출하도록 MeterFilter를 추가한다.
 * 버전: v1.2.0
 */
@Configuration
public class TelemetryConfig {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConfig.class);

    private SdkTracerProvider tracerProvider;

    @Bean
    public OpenTelemetry openTelemetry(Environment environment) {
        String endpoint = environment.getProperty("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317");
        SpanExporter exporter = buildExporter(endpoint);
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .setIdGenerator(IdGenerator.random())
                .setResource(Resource.getDefault().merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), "codexpong-backend"
                ))))
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
        try {
            GlobalOpenTelemetry.set(sdk);
        } catch (IllegalStateException alreadySet) {
            log.warn("GlobalOpenTelemetry 이미 설정됨: {}", alreadySet.getMessage());
        }
        return sdk;
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("codexpong-backend");
    }

    @Bean
    public MeterFilter httpServerRequestPercentileFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if ("http.server.requests".equals(id.getName())) {
                    return DistributionStatisticConfig.builder()
                            .percentiles(0.95)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }

    @PreDestroy
    public void shutdown() {
        if (tracerProvider != null) {
            tracerProvider.close();
        }
    }

    private SpanExporter buildExporter(String endpoint) {
        try {
            return OtlpGrpcSpanExporter.builder()
                    .setEndpoint(endpoint)
                    .build();
        } catch (Throwable exc) { // NoClassDef 등 치명적 오류도 폴백
            log.warn("OTLP 익스포터 초기화 실패, 로깅 익스포터로 폴백: {}", exc.getMessage());
            return LoggingSpanExporter.create();
        }
    }
}
