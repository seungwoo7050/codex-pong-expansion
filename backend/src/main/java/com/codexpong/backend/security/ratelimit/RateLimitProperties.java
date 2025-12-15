package com.codexpong.backend.security.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/security/ratelimit/RateLimitProperties.java
 * 설명:
 *   - 로그인/채팅/웹소켓 연결에 대한 기본 레이트리밋 한도를 주입한다.
 */
@Component
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private RateLimitRule login = new RateLimitRule(5, Duration.ofMinutes(1));
    private RateLimitRule chat = new RateLimitRule(30, Duration.ofMinutes(1));
    private RateLimitRule websocket = new RateLimitRule(20, Duration.ofMinutes(1));

    public RateLimitRule getLogin() {
        return login;
    }

    public void setLogin(RateLimitRule login) {
        this.login = login;
    }

    public RateLimitRule getChat() {
        return chat;
    }

    public void setChat(RateLimitRule chat) {
        this.chat = chat;
    }

    public RateLimitRule getWebsocket() {
        return websocket;
    }

    public void setWebsocket(RateLimitRule websocket) {
        this.websocket = websocket;
    }

    public record RateLimitRule(int limit, Duration window) {
    }
}
