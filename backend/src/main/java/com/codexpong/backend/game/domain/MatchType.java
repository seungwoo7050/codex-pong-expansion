package com.codexpong.backend.game.domain;

/**
 * [열거형] backend/src/main/java/com/codexpong/backend/game/domain/MatchType.java
 * 설명:
 *   - v0.4.0 랭크 시스템에서 일반전과 랭크전을 구분하기 위한 타입이다.
 *   - WebSocket 이벤트와 전적 저장 시 동일한 값을 사용한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 */
public enum MatchType {
    NORMAL,
    RANKED
}
