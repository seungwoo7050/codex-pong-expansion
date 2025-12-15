package com.codexpong.backend.game;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.game.dto.MatchmakingResponse;
import com.codexpong.backend.game.service.MatchmakingService;
import com.codexpong.backend.game.service.MatchmakingService.MatchTicket;
import com.codexpong.backend.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/game/MatchmakingController.java
 * 설명:
 *   - 빠른 대전 큐에 사용자를 등록하고 매칭 상태를 조회하는 엔드포인트를 제공한다.
 *   - WebSocket 연결 전에 roomId를 전달받기 위한 티켓 형태로 응답한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 * 변경 이력:
 *   - v0.9.0: 밴/정지 사용자 차단을 위해 활성 사용자 조회 헬퍼 사용
 */
@RestController
@RequestMapping("/api/match/quick")
public class MatchmakingController {

    private final MatchmakingService matchmakingService;
    private final UserService userService;

    public MatchmakingController(MatchmakingService matchmakingService, UserService userService) {
        this.matchmakingService = matchmakingService;
        this.userService = userService;
    }

    /**
     * 설명:
     *   - 현재 사용자로 빠른 대전 큐 등록을 시도한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchmakingResponse enqueue(@AuthenticationPrincipal AuthenticatedUser user) {
        MatchTicket ticket = matchmakingService.enqueue(userService.getActiveUserEntity(user.id()), MatchType.NORMAL);
        return MatchmakingResponse.from(ticket);
    }

    /**
     * 설명:
     *   - 티켓 ID 기준으로 매칭 진행 상태와 roomId를 반환한다.
     */
    @GetMapping("/{ticketId}")
    public MatchmakingResponse status(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String ticketId) {
        MatchTicket ticket = matchmakingService.findTicket(ticketId)
                .filter(t -> t.userId().equals(user.id()) && t.matchType() == MatchType.NORMAL)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대기열 정보를 찾을 수 없습니다."));
        return MatchmakingResponse.from(ticket);
    }
}
