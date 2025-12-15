package com.codexpong.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/config/OAuthRestTemplateConfig.java
 * 설명:
 *   - 카카오/네이버 OAuth 프로필 조회에 사용할 RestTemplate을 분리해 주입 가능하도록 제공한다.
 *   - 테스트에서는 해당 빈에 Mock 서버를 연결해 외부 호출 없이 검증할 수 있다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
@Configuration
public class OAuthRestTemplateConfig {

    private static final ClientHttpRequestInterceptor JSON_ACCEPT_HEADER = (request, body, execution) -> {
        request.getHeaders().set("Accept", "application/json");
        return execution.execute(request, body);
    };

    @Bean
    public RestTemplate kakaoOAuthRestTemplate(RestTemplateBuilder builder) {
        return builder.additionalInterceptors(JSON_ACCEPT_HEADER).build();
    }

    @Bean
    public RestTemplate naverOAuthRestTemplate(RestTemplateBuilder builder) {
        return builder.additionalInterceptors(JSON_ACCEPT_HEADER).build();
    }
}
