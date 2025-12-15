package com.codexpong.backend.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * [필터] backend/src/main/java/com/codexpong/backend/config/TraceContextFilter.java
 * 설명:
 *   - HTTP 요청에 대해 W3C traceparent를 추출하고 서버 스팬을 생성한다.
 *   - 응답 헤더에 traceId를 노출해 클라이언트가 게이트웨이/샤드/워커로 전달할 수 있게 한다.
 * 버전: v1.2.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceContextFilter extends OncePerRequestFilter {

    private final Tracer tracer;
    private final TextMapPropagator propagator;

    public TraceContextFilter(Tracer tracer, OpenTelemetry openTelemetry) {
        this.tracer = tracer;
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Context extracted = propagator.extract(Context.current(), request, new ServletGetter());
        Span span = tracer.spanBuilder(request.getMethod() + " " + request.getRequestURI())
                .setParent(extracted)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", request.getMethod());
            span.setAttribute("http.target", request.getRequestURI());
            filterChain.doFilter(request, response);
            span.setAttribute("http.status_code", (long) response.getStatus());
        } finally {
            response.setHeader("X-Trace-Id", span.getSpanContext().getTraceId());
            span.end();
        }
    }

    private static class ServletGetter implements TextMapGetter<HttpServletRequest> {

        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return carrier.getHeaderNames() == null
                    ? java.util.List.of()
                    : java.util.Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }
    }
}
