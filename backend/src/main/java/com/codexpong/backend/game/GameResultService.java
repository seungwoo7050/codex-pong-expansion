package com.codexpong.backend.game;

import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.game.service.RankingService;
import com.codexpong.backend.user.domain.User;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/game/GameResultService.java
 * 설명:
 *   - 실시간 경기 종료 시 결과를 생성하고 최근 전적을 조회한다.
 *   - v0.4.0에서는 랭크전 결과에 따라 User 레이팅을 갱신하고 변동 폭을 기록한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 * 변경 이력:
 *   - v0.1.0: 서비스 계층 최초 구현
 *   - v0.3.0: 사용자 연계 및 자동 기록 로직으로 확장
 *   - v0.4.0: 랭크전 레이팅 갱신 로직 추가
 */
@Service
public class GameResultService {

    private final GameResultRepository gameResultRepository;
    private final RankingService rankingService;
    private final ApplicationEventPublisher eventPublisher;

    public GameResultService(GameResultRepository gameResultRepository, RankingService rankingService,
            ApplicationEventPublisher eventPublisher) {
        this.gameResultRepository = gameResultRepository;
        this.rankingService = rankingService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public GameResult recordResult(String roomId, User playerA, User playerB, int scoreA, int scoreB,
            MatchType matchType, LocalDateTime startedAt, LocalDateTime finishedAt) {
        int ratingChangeA = 0;
        int ratingChangeB = 0;
        int ratingAfterA = playerA.getRating();
        int ratingAfterB = playerB.getRating();

        if (matchType == MatchType.RANKED) {
            RankingService.RatingOutcome outcome = rankingService.applyRanking(playerA, playerB, scoreA, scoreB);
            ratingChangeA = outcome.ratingChangeA();
            ratingChangeB = outcome.ratingChangeB();
            ratingAfterA = outcome.ratingAfterA();
            ratingAfterB = outcome.ratingAfterB();
        }

        GameResult gameResult = new GameResult(playerA, playerB, scoreA, scoreB, roomId, matchType,
                ratingChangeA, ratingChangeB, ratingAfterA, ratingAfterB, startedAt, finishedAt);
        GameResult saved = gameResultRepository.save(gameResult);
        eventPublisher.publishEvent(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<GameResult> findRecentResults() {
        return gameResultRepository.findTop20ByOrderByFinishedAtDesc();
    }
}
