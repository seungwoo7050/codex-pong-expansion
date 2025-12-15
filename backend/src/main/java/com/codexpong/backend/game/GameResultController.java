package com.codexpong.backend.game;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/game/GameResultController.java
 * 설명:
 *   - 실시간 경기 종료 후 저장된 결과를 조회하는 API를 제공한다.
 *   - v0.3.0에서는 게임 엔진이 자동으로 기록한 결과를 반환하며, 수동 생성 기능은 제거했다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 * 변경 이력:
 *   - v0.1.0: 경기 생성/조회 API 추가
 *   - v0.3.0: 조회 전용 엔드포인트로 단순화
 *   - v0.4.0: 매치 타입/레이팅 정보를 포함하도록 응답 확장
 */
@RestController
@RequestMapping("/api/games")
public class GameResultController {

    private final GameResultService gameResultService;

    public GameResultController(GameResultService gameResultService) {
        this.gameResultService = gameResultService;
    }

    @GetMapping
    public List<GameResultResponse> listGames() {
        return gameResultService.findRecentResults().stream()
                .map(GameResultResponse::from)
                .collect(Collectors.toList());
    }
}
