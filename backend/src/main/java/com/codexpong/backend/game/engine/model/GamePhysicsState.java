package com.codexpong.backend.game.engine.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [도메인] backend/src/main/java/com/codexpong/backend/game/engine/model/GamePhysicsState.java
 * 설명:
 *   - 경기장의 좌표, 공/패들 위치, 점수와 종료 상태를 보관한다.
 *   - 엔진이 내부적으로 갱신하며, 외부에는 GameSnapshot 형태로 노출된다.
 * 버전: v0.3.0
 * 관련 설계문서:
 *   - design/realtime/v0.3.0-game-loop-and-events.md
 *   - design/backend/v0.3.0-game-and-matchmaking.md
 */
public class GamePhysicsState {

    private final double courtWidth;
    private final double courtHeight;
    private final double paddleHeight;
    private final int targetScore;

    private final String roomId;
    private final LocalDateTime startedAt;

    private double ballX;
    private double ballY;
    private double ballVelocityX;
    private double ballVelocityY;
    private double leftPaddleY;
    private double rightPaddleY;
    private int leftScore;
    private int rightScore;
    private boolean finished;

    public GamePhysicsState(double courtWidth, double courtHeight, double paddleHeight, int targetScore) {
        this.courtWidth = courtWidth;
        this.courtHeight = courtHeight;
        this.paddleHeight = paddleHeight;
        this.targetScore = targetScore;
        this.roomId = UUID.randomUUID().toString();
        this.startedAt = LocalDateTime.now();
    }

    public GameSnapshot toSnapshot() {
        return new GameSnapshot(roomId, ballX, ballY, ballVelocityX, ballVelocityY, leftPaddleY, rightPaddleY,
                leftScore, rightScore, targetScore, finished);
    }

    public void applyPaddleMove(GameSide side, double deltaY) {
        if (side == GameSide.LEFT) {
            leftPaddleY = clamp(leftPaddleY + deltaY, 0, courtHeight - paddleHeight);
        } else {
            rightPaddleY = clamp(rightPaddleY + deltaY, 0, courtHeight - paddleHeight);
        }
    }

    public void moveBall(double seconds) {
        ballX += ballVelocityX * seconds;
        ballY += ballVelocityY * seconds;
    }

    public void reflectVertical() {
        ballVelocityY = -ballVelocityY;
    }

    public void reflectHorizontal() {
        ballVelocityX = -ballVelocityX;
    }

    public void resetBall(GameSide direction, double speed) {
        this.ballX = courtWidth / 2;
        this.ballY = courtHeight / 2;
        double initialX = direction == GameSide.LEFT ? speed : -speed;
        this.ballVelocityX = initialX;
        this.ballVelocityY = speed / 2;
    }

    public void resetPaddles(double height) {
        this.leftPaddleY = (courtHeight - height) / 2;
        this.rightPaddleY = (courtHeight - height) / 2;
    }

    public void score(GameSide side) {
        if (side == GameSide.LEFT) {
            leftScore += 1;
        } else {
            rightScore += 1;
        }
        if (leftScore >= targetScore || rightScore >= targetScore) {
            finished = true;
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public double courtWidth() {
        return courtWidth;
    }

    public double courtHeight() {
        return courtHeight;
    }

    public double paddleHeight() {
        return paddleHeight;
    }

    public String roomId() {
        return roomId;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public double ballX() {
        return ballX;
    }

    public double ballY() {
        return ballY;
    }

    public double ballVelocityX() {
        return ballVelocityX;
    }

    public double ballVelocityY() {
        return ballVelocityY;
    }

    public double leftPaddleY() {
        return leftPaddleY;
    }

    public double rightPaddleY() {
        return rightPaddleY;
    }

    public int leftScore() {
        return leftScore;
    }

    public int rightScore() {
        return rightScore;
    }

    public int targetScore() {
        return targetScore;
    }

    public boolean finished() {
        return finished;
    }
}
