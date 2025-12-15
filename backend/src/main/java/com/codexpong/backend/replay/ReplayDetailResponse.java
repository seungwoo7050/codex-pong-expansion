package com.codexpong.backend.replay;

import com.codexpong.backend.user.domain.User;

/**
 * [DTO] backend/src/main/java/com/codexpong/backend/replay/ReplayDetailResponse.java
 * 설명:
 *   - 단일 리플레이 조회 시 체크섬과 이벤트 다운로드 경로를 함께 제공한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
public record ReplayDetailResponse(
        ReplaySummaryResponse summary,
        String checksum,
        String downloadPath
) {

    public static ReplayDetailResponse from(Replay replay, User owner) {
        return new ReplayDetailResponse(
                ReplaySummaryResponse.from(replay, owner),
                replay.getChecksum(),
                "/api/replays/" + replay.getId() + "/events"
        );
    }
}
