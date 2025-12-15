package com.codexpong.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [설정] backend/src/main/java/com/codexpong/backend/config/OpenApiConfig.java
 * 설명:
 *   - springdoc-openapi를 통해 자동으로 생성되는 OpenAPI 문서를 구성한다.
 *   - JWT Bearer 인증 스킴만 선언해 보안 설정을 최소한으로 반영한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.10.0: Swagger UI 노출을 위한 기본 OpenAPI 설정 추가
 *   - v0.10.0: 빌드 버전 정보를 이용해 OpenAPI 버전 정보를 자동 동기화
 */
@Configuration
public class OpenApiConfig {

    private final BuildProperties buildProperties;

    public OpenApiConfig(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public OpenAPI codexPongOpenApi() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .name("BearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .info(new Info().title("Codex Pong API").version("v" + buildProperties.getVersion()))
                .components(new Components().addSecuritySchemes("BearerAuth", bearerAuth))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
