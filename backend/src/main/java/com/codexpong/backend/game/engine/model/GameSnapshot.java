package com.codexpong.backend.game.engine.model;

/**
 * [DTO] backend/src/main/java/com/codexpong/backend/game/engine/model/GameSnapshot.java
 * 설명:
 *   - 클라이언트로 전송할 수 있는 현재 경기 상태 스냅샷이다.
 *   - 좌표, 속도, 점수, 목표 점수, 종료 여부를 포함한다.
 * 버전: v0.3.0
 * 관련 설계문서:
 *   - design/realtime/v0.3.0-game-loop-and-events.md
 */
public record GameSnapshot(
        String roomId,
        double ballX,
        double ballY,
        double ballVelocityX,
        double ballVelocityY,
        double leftPaddleY,
        double rightPaddleY,
        int leftScore,
        int rightScore,
        int targetScore,
        boolean finished
) {
}
