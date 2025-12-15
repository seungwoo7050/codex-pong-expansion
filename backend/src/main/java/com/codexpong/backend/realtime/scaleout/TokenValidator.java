package com.codexpong.backend.realtime.scaleout;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/TokenValidator.java
 * 설명:
 *   - 게이트웨이가 위임받은 인증 검증 역할을 단순화하기 위한 인터페이스이다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-gateway-shard-protocol.md
 * 변경 이력:
 *   - v1.1.0: 토큰 검증 인터페이스 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public interface TokenValidator {
    boolean isValid(String token);
}
