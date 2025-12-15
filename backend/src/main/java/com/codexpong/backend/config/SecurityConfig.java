package com.codexpong.backend.config;

import com.codexpong.backend.auth.config.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * [보안 설정] backend/src/main/java/com/codexpong/backend/config/SecurityConfig.java
 * 설명:
 *   - v0.2.0에서 JWT 기반 무상태 인증을 활성화하고 공개/보호 엔드포인트를 구분한다.
 *   - CORS 기본값을 활성화해 프런트엔드 SPA와 통신할 수 있도록 한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: JWT 필터와 세션 정책 설정 추가
 *   - v0.6.0: 로비/매치 채팅 히스토리 GET 엔드포인트를 비인증 허용으로 확장
 *   - v0.10.0: Swagger 문서 경로를 공개로 허용
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다."))
                )
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/oauth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chat/lobby", "/api/chat/match/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
