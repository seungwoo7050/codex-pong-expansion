package com.codexpong.backend.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/job/JobQueueProperties.java
 * 설명:
 *   - Redis Streams 큐 토폴로지에 사용되는 스트림 이름, 그룹 이름, 활성화 여부를 외부 설정으로 묶는다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/infra/v0.12.0-worker-and-queue-topology.md
 */
@ConfigurationProperties(prefix = "jobs.queue")
public class JobQueueProperties {

    private boolean enabled = true;
    private String requestStream = "job.requests";
    private String progressStream = "job.progress";
    private String resultStream = "job.results";
    private String consumerGroup = "replay-jobs";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRequestStream() {
        return requestStream;
    }

    public void setRequestStream(String requestStream) {
        this.requestStream = requestStream;
    }

    public String getProgressStream() {
        return progressStream;
    }

    public void setProgressStream(String progressStream) {
        this.progressStream = progressStream;
    }

    public String getResultStream() {
        return resultStream;
    }

    public void setResultStream(String resultStream) {
        this.resultStream = resultStream;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
}
