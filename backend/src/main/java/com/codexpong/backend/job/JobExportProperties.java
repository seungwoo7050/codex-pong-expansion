package com.codexpong.backend.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/job/JobExportProperties.java
 * 설명:
 *   - 워커가 출력 파일을 기록할 기본 디렉터리를 외부 설정으로 관리한다.
 *   - 리플레이 JSONL 원본은 v0.11.0 저장 경로를 그대로 재사용한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
@ConfigurationProperties(prefix = "jobs.export")
public class JobExportProperties {

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
