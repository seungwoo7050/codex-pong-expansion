package com.codexpong.backend.game.engine;

import com.codexpong.backend.game.engine.model.GamePhysicsState;
import com.codexpong.backend.game.engine.model.GameSide;
import com.codexpong.backend.game.engine.model.GameSnapshot;
import com.codexpong.backend.game.engine.model.PaddleInput;
import java.time.Duration;

/**
 * [엔진] backend/src/main/java/com/codexpong/backend/game/engine/GameEngine.java
 * 설명:
 *   - v0.3.0 실시간 1:1 경기를 위한 틱 기반 물리 시뮬레이션을 담당한다.
 *   - 패들 이동 입력과 공 이동, 득점/리셋을 관리하며 스냅샷을 반환한다.
 * 버전: v0.3.0
 * 관련 설계문서:
 *   - design/realtime/v0.3.0-game-loop-and-events.md
 *   - design/backend/v0.3.0-game-and-matchmaking.md
 * 변경 이력:
 *   - v0.3.0: 기본 공/패들 이동 및 득점 판정 로직 추가
 */
public class GameEngine {

    private static final double COURT_WIDTH = 800;
    private static final double COURT_HEIGHT = 480;
    private static final double PADDLE_HEIGHT = 80;
    private static final double PADDLE_SPEED = 260; // px per second
    private static final double BALL_SPEED = 280; // px per second
    private static final int TARGET_SCORE = 5;

    private final GamePhysicsState state;

    public GameEngine() {
        this.state = new GamePhysicsState(COURT_WIDTH, COURT_HEIGHT, PADDLE_HEIGHT, TARGET_SCORE);
        resetRound(GameSide.LEFT);
    }

    public int getTargetScore() {
        return TARGET_SCORE;
    }

    /**
     * 설명:
     *   - 틱마다 패들/공 위치를 갱신하고, 득점 여부를 판단한다.
     * 입력:
     *   - delta: 틱 시간 간격
     *   - leftInput/rightInput: 각 플레이어의 입력 상태
     * 출력:
     *   - 현재 스냅샷 (좌표, 점수, 종료 여부)
     */
    public synchronized GameSnapshot tick(Duration delta, PaddleInput leftInput, PaddleInput rightInput) {
        double seconds = delta.toMillis() / 1000.0;
        movePaddle(GameSide.LEFT, leftInput, seconds);
        movePaddle(GameSide.RIGHT, rightInput, seconds);
        moveBall(seconds);
        return state.toSnapshot();
    }

    public synchronized GameSnapshot forceSnapshot() {
        return state.toSnapshot();
    }

    private void movePaddle(GameSide side, PaddleInput input, double seconds) {
        double deltaY = switch (input) {
            case UP -> -PADDLE_SPEED * seconds;
            case DOWN -> PADDLE_SPEED * seconds;
            default -> 0;
        };
        state.applyPaddleMove(side, deltaY);
    }

    private void moveBall(double seconds) {
        if (state.finished()) {
            return;
        }
        state.moveBall(seconds);
        bounceIfNeeded();
        if (state.ballX() < 0) {
            state.score(GameSide.RIGHT);
            resetRound(GameSide.LEFT);
        } else if (state.ballX() > state.courtWidth()) {
            state.score(GameSide.LEFT);
            resetRound(GameSide.RIGHT);
        }
    }

    private void bounceIfNeeded() {
        if (state.ballY() <= 0 || state.ballY() >= state.courtHeight()) {
            state.reflectVertical();
        }

        double leftPaddleX = 40;
        double rightPaddleX = state.courtWidth() - 40;

        if (state.ballVelocityX() < 0 && state.ballX() <= leftPaddleX
                && state.ballY() >= state.leftPaddleY()
                && state.ballY() <= state.leftPaddleY() + state.paddleHeight()) {
            state.reflectHorizontal();
        }
        if (state.ballVelocityX() > 0 && state.ballX() >= rightPaddleX
                && state.ballY() >= state.rightPaddleY()
                && state.ballY() <= state.rightPaddleY() + state.paddleHeight()) {
            state.reflectHorizontal();
        }
    }

    private void resetRound(GameSide toSide) {
        state.resetBall(toSide, BALL_SPEED);
        state.resetPaddles(state.paddleHeight());
    }
}
