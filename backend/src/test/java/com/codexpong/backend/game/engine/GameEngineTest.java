package com.codexpong.backend.game.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexpong.backend.game.engine.model.GameSnapshot;
import com.codexpong.backend.game.engine.model.PaddleInput;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * [단위 테스트] backend/src/test/java/com/codexpong/backend/game/engine/GameEngineTest.java
 * 설명:
 *   - v0.3.0 게임 엔진이 틱 기반으로 이동/득점 상태를 변경하는지 검증한다.
 */
class GameEngineTest {

    @Test
    @DisplayName("공이 이동하고 점수가 누적된다")
    void ballMovesAndScores() {
        GameEngine engine = new GameEngine();

        GameSnapshot first = engine.tick(Duration.ofMillis(100), PaddleInput.STAY, PaddleInput.STAY);
        assertThat(first.ballX()).isNotEqualTo(0);

        GameSnapshot scored = engine.tick(Duration.ofSeconds(2), PaddleInput.STAY, PaddleInput.STAY);
        assertThat(scored.leftScore() + scored.rightScore()).isGreaterThanOrEqualTo(1);
    }
}
