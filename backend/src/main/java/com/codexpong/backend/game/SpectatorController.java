package com.codexpong.backend.game;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.game.dto.LiveMatchResponse;
import com.codexpong.backend.game.service.GameRoomService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/game/SpectatorController.java
 * 설명:
 *   - 진행 중인 경기 목록을 조회하고 관전 모드 진입에 필요한 roomId 정보를 전달한다.
 *   - 인증된 사용자가 관전 목록을 조회한다는 가정하에 인증 Principal을 요구한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/backend/v0.8.0-spectator-mode.md
 */
@RestController
@RequestMapping("/api/match/ongoing")
public class SpectatorController {

    private final GameRoomService gameRoomService;

    public SpectatorController(GameRoomService gameRoomService) {
        this.gameRoomService = gameRoomService;
    }

    /**
     * 설명:
     *   - 실시간으로 진행 중인 경기 방 목록을 반환한다.
     */
    @GetMapping
    public List<LiveMatchResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null) {
            return List.of();
        }
        return gameRoomService.listLiveRooms().stream()
                .map(LiveMatchResponse::from)
                .toList();
    }
}
