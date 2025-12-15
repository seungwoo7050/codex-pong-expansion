package com.codexpong.backend.async.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/async/outbox/OutboxRelayProperties.java
 * 설명:
 *   - 아웃박스 릴레이의 배치 크기와 재시도 한도를 설정한다.
 */
@ConfigurationProperties(prefix = "outbox.relay")
public class OutboxRelayProperties {

    private int batchSize = 20;
    private int maxAttempts = 3;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
