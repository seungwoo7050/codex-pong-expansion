package com.codexpong.backend.replay;

import java.util.List;

/**
 * [DTO] backend/src/main/java/com/codexpong/backend/replay/ReplayPageResponse.java
 * 설명:
 *   - 리플레이 목록 조회 시 페이징 정보를 포함해 반환한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
public record ReplayPageResponse(
        List<ReplaySummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
