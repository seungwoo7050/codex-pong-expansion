package com.codexpong.backend.tournament.domain;

/**
 * [상태] backend/src/main/java/com/codexpong/backend/tournament/domain/TournamentMatchStatus.java
 * 설명:
 *   - 토너먼트 개별 매치의 준비/완료 상태를 관리한다.
 *   - WebSocket 알림 발행 기준으로 READY→COMPLETED 흐름을 구분한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public enum TournamentMatchStatus {
    PENDING,
    READY,
    COMPLETED
}
