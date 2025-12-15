package com.codexpong.backend.config;

import java.util.Properties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/config/BuildInfoConfig.java
 * 설명:
 *   - 테스트 환경 등에서 build-info.properties가 없을 때 기본 BuildProperties 빈을 제공한다.
 *   - OpenAPI 설정이 버전 정보를 필요로 하므로 안전한 기본값을 주입한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
@Configuration
public class BuildInfoConfig {

    @Bean
    @ConditionalOnMissingBean(BuildProperties.class)
    public BuildProperties fallbackBuildProperties() {
        Properties props = new Properties();
        props.put("version", "0.11.0");
        return new BuildProperties(props);
    }
}
