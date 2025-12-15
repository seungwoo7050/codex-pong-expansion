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
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/game/RankedMatchmakingController.java
 * 설명:
 *   - v0.4.0 랭크 큐 전용 엔드포인트를 제공해 일반전과 큐를 분리한다.
 *   - 동일한 응답 포맷으로 roomId와 매치 타입을 반환한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 * 변경 이력:
 *   - v0.9.0: 랭크 큐에서도 밴/정지 사용자를 차단하도록 개선
 */
@RestController
@RequestMapping("/api/match/ranked")
public class RankedMatchmakingController {

    private final MatchmakingService matchmakingService;
    private final UserService userService;

    public RankedMatchmakingController(MatchmakingService matchmakingService, UserService userService) {
        this.matchmakingService = matchmakingService;
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchmakingResponse enqueue(@AuthenticationPrincipal AuthenticatedUser user) {
        MatchTicket ticket = matchmakingService.enqueue(userService.getActiveUserEntity(user.id()), MatchType.RANKED);
        return MatchmakingResponse.from(ticket);
    }

    @GetMapping("/{ticketId}")
    public MatchmakingResponse status(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String ticketId) {
        MatchTicket ticket = matchmakingService.findTicket(ticketId)
                .filter(t -> t.userId().equals(user.id()) && t.matchType() == MatchType.RANKED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대기열 정보를 찾을 수 없습니다."));
        return MatchmakingResponse.from(ticket);
    }
}
