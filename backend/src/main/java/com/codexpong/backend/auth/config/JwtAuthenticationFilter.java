package com.codexpong.backend.auth.config;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.auth.service.AuthService;
import com.codexpong.backend.auth.service.AuthTokenService;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * [필터] backend/src/main/java/com/codexpong/backend/auth/config/JwtAuthenticationFilter.java
 * 설명:
 *   - HTTP Authorization 헤더의 Bearer 토큰을 검증해 SecurityContext에 인증 정보를 적재한다.
 *   - 토큰이 없거나 잘못된 경우는 다음 필터로 전달하여 인증이 필요한 API에서 거부되도록 한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: JWT 기반 stateless 인증 필터 추가
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenService authTokenService;
    private final AuthService authService;

    public JwtAuthenticationFilter(AuthTokenService authTokenService, AuthService authService) {
        this.authTokenService = authTokenService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<AuthenticatedUser> parsed = authTokenService.parse(token)
                    .flatMap(user -> authService.findById(user.id()).map(authService::toAuthenticatedUser));
            if (parsed.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthenticatedUser principal = parsed.get();
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
