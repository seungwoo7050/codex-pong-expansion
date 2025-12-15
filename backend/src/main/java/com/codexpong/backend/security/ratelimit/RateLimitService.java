package com.codexpong.backend.security.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * [컴포넌트] backend/src/main/java/com/codexpong/backend/security/ratelimit/RateLimitService.java
 * 설명:
 *   - 로그인/채팅/웹소켓 카테고리별 고정 윈도우 카운터를 이용해 레이트리밋을 강제한다.
 *   - Micrometer 카운터로 허용/차단 이벤트를 관측 가능하게 남긴다.
 */
@Component
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final RateLimitProperties properties;
    private final Map<String, WindowCounter> buckets = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public RateLimitService(RateLimitProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public void checkOrThrow(String category, String key) {
        boolean allowed = allow(category, key);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도하세요.");
        }
    }

    private boolean allow(String category, String key) {
        RateLimitProperties.RateLimitRule rule = resolveRule(category);
        if (rule == null) {
            return true;
        }
        String bucketKey = category + ":" + key;
        long now = Instant.now().toEpochMilli();
        WindowCounter updated = buckets.compute(bucketKey, (k, counter) -> {
            if (counter == null || counter.windowExpiresAt <= now) {
                return new WindowCounter(1, now + rule.window().toMillis());
            }
            return new WindowCounter(counter.count + 1, counter.windowExpiresAt);
        });
        boolean allowed = updated.count <= rule.limit();
        Counter.builder("security.rate_limit")
                .description("카테고리별 레이트리밋 체크")
                .tag("category", category)
                .tag("result", allowed ? "allowed" : "blocked")
                .register(meterRegistry)
                .increment();
        if (!allowed) {
            log.warn("[RATE_LIMIT] category={} key={} blocked count={} limit={} windowEnds={}",
                    category, key, updated.count, rule.limit(), updated.windowExpiresAt);
        }
        return allowed;
    }

    private RateLimitProperties.RateLimitRule resolveRule(String category) {
        return switch (category) {
            case "login" -> properties.getLogin();
            case "chat" -> properties.getChat();
            case "websocket" -> properties.getWebsocket();
            default -> null;
        };
    }

    private record WindowCounter(int count, long windowExpiresAt) {
    }
}
