package com.codexpong.backend.game.dto;

import com.codexpong.backend.game.service.MatchmakingService;

/**
 * [DTO] backend/src/main/java/com/codexpong/backend/game/dto/MatchmakingResponse.java
 * 설명:
 *   - 매칭 큐 등록 및 상태 조회 응답을 공통 포맷으로 제공한다.
 *   - 매치 타입을 포함해 랭크/일반 구분을 프런트엔드에 전달한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 */
public record MatchmakingResponse(String ticketId, String status, String roomId, String matchType) {

    public static MatchmakingResponse from(MatchmakingService.MatchTicket ticket) {
        return new MatchmakingResponse(ticket.ticketId(), ticket.status(), ticket.roomId(), ticket.matchType().name());
    }
}
