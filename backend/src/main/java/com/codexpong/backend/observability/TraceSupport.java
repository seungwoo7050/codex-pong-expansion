package com.codexpong.backend.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;

/**
 * [헬퍼] backend/src/main/java/com/codexpong/backend/observability/TraceSupport.java
 * 설명:
 *   - traceId 문자열로부터 부모 컨텍스트를 생성해 게이트웨이/샤드/워커 스팬을 동일한 트레이스에 묶는다.
 *   - 유효하지 않은 traceId가 들어오면 새 트레이스를 생성하되 원본 correlation id를 속성에 남겨 추적 가능성을 높인다.
 * 버전: v1.2.0
 */
public class TraceSupport {

    private final Tracer tracer;
    private final IdGenerator idGenerator;

    public TraceSupport(Tracer tracer) {
        this(tracer, IdGenerator.random());
    }

    public TraceSupport(Tracer tracer, IdGenerator idGenerator) {
        this.tracer = tracer;
        this.idGenerator = idGenerator;
    }

    public String normalizeTraceId(String traceId) {
        if (TraceId.isValid(traceId)) {
            return traceId;
        }
        return idGenerator.generateTraceId();
    }

    public Span startSpan(String name, String traceId, SpanKind kind) {
        SpanBuilder builder = tracer.spanBuilder(name).setSpanKind(kind);
        builder.setParent(buildParentContext(traceId));
        if (!TraceId.isValid(traceId) && traceId != null && !traceId.isBlank()) {
            builder.setAttribute("correlation.trace_id", traceId);
        }
        return builder.startSpan();
    }

    private Context buildParentContext(String traceId) {
        if (TraceId.isValid(traceId)) {
            SpanContext parent = SpanContext.createFromRemoteParent(traceId, idGenerator.generateSpanId(), TraceFlags.getSampled(),
                    TraceState.getDefault());
            return Context.current().with(Span.wrap(parent));
        }
        return Context.current();
    }
}
