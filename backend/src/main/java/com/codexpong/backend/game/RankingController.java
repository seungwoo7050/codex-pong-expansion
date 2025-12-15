package com.codexpong.backend.game;

import com.codexpong.backend.game.dto.LeaderboardEntryResponse;
import com.codexpong.backend.user.repository.UserRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/game/RankingController.java
 * 설명:
 *   - v0.4.0 리더보드 조회 API를 제공하여 상위 레이팅 사용자를 반환한다.
 *   - 단순 정렬 기반 글로벌 순위를 제공하며 페이지네이션은 이후 버전에서 확장한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 */
@RestController
@RequestMapping("/api/rank")
public class RankingController {

    private final UserRepository userRepository;

    public RankingController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntryResponse> leaderboard() {
        List<com.codexpong.backend.user.domain.User> users = userRepository.findTop20ByOrderByRatingDesc();
        return IntStream.range(0, users.size())
                .mapToObj(index -> LeaderboardEntryResponse.from(users.get(index), index + 1))
                .toList();
    }
}
