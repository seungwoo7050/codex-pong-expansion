package com.codexpong.backend.config;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import net.logstash.logback.argument.StructuredArguments;

/**
 * [필터] backend/src/main/java/com/codexpong/backend/config/RequestLoggingFilter.java
 * 설명:
 *   - 모든 HTTP 요청에 대해 처리 시간, 상태 코드, 사용자 ID를 JSON 로그로 남긴다.
 *   - v0.9.0 모니터링 스택에서 로그 기반 분석을 쉽게 하기 위해 구조화된 필드를 사용한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.9.0-admin-and-ops.md
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Instant start = Instant.now();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            Long userId = currentUserId();
            log.info("요청 처리 완료",
                    StructuredArguments.kv("method", request.getMethod()),
                    StructuredArguments.kv("path", request.getRequestURI()),
                    StructuredArguments.kv("status", response.getStatus()),
                    StructuredArguments.kv("durationMs", durationMs),
                    StructuredArguments.kv("userId", userId));
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.id();
        }
        return null;
    }
}
