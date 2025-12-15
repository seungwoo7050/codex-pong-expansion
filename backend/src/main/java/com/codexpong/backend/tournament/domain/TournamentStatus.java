package com.codexpong.backend.tournament.domain;

/**
 * [상태] backend/src/main/java/com/codexpong/backend/tournament/domain/TournamentStatus.java
 * 설명:
 *   - 토너먼트 전체 진행 단계를 표현한다.
 *   - v0.7.0 단일 제거 방식에서 등록/진행/완료 3단계를 명확히 구분한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public enum TournamentStatus {
    REGISTRATION,
    IN_PROGRESS,
    COMPLETED
}
