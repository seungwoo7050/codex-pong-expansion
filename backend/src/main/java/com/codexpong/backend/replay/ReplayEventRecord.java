package com.codexpong.backend.replay;

import com.codexpong.backend.game.engine.model.GameSnapshot;

/**
 * [모델] backend/src/main/java/com/codexpong/backend/replay/ReplayEventRecord.java
 * 설명:
 *   - JSON Lines 포맷으로 직렬화되는 리플레이 이벤트 단위다.
 *   - offsetMs는 녹화 시작 시점으로부터의 경과 시간을 나타낸다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
public record ReplayEventRecord(
        long offsetMs,
        GameSnapshot snapshot
) {
}
