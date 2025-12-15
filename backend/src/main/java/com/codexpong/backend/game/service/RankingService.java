package com.codexpong.backend.game.service;

import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/game/service/RankingService.java
 * 설명:
 *   - v0.4.0 랭크전 결과를 기반으로 ELO/MMR 스타일의 레이팅을 갱신한다.
 *   - 저장된 레이팅을 반환해 게임 결과 기록과 리더보드에 활용한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 */
@Service
public class RankingService {

    private static final int BASE_RATING = 1200;
    private static final int K_FACTOR = 32;

    private final UserRepository userRepository;

    public RankingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 설명:
     *   - 랭크전 점수 차를 승/패/무로 환산해 두 사용자의 레이팅을 수정한다.
     * 출력:
     *   - RatingOutcome: 각 사용자별 변동 폭과 최종 레이팅
     */
    public RatingOutcome applyRanking(User playerA, User playerB, int scoreA, int scoreB) {
        int beforeA = defaultRating(playerA.getRating());
        int beforeB = defaultRating(playerB.getRating());

        double expectedA = 1.0 / (1 + Math.pow(10, (beforeB - beforeA) / 400.0));
        double expectedB = 1.0 / (1 + Math.pow(10, (beforeA - beforeB) / 400.0));

        double actualA;
        if (scoreA == scoreB) {
            actualA = 0.5;
        } else if (scoreA > scoreB) {
            actualA = 1.0;
        } else {
            actualA = 0.0;
        }
        double actualB = 1.0 - actualA;

        int afterA = Math.max(1, (int) Math.round(beforeA + K_FACTOR * (actualA - expectedA)));
        int afterB = Math.max(1, (int) Math.round(beforeB + K_FACTOR * (actualB - expectedB)));

        playerA.updateRating(afterA);
        playerB.updateRating(afterB);
        userRepository.saveAll(List.of(playerA, playerB));

        return new RatingOutcome(afterA - beforeA, afterB - beforeB, afterA, afterB);
    }

    private int defaultRating(Integer rating) {
        return rating == null ? BASE_RATING : rating;
    }

    public record RatingOutcome(int ratingChangeA, int ratingChangeB, int ratingAfterA, int ratingAfterB) {
    }
}
