package com.codexpong.backend.replay;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/replay/ReplayController.java
 * 설명:
 *   - v0.11.0 리플레이 목록/상세 조회와 이벤트 파일 스트리밍 API를 제공한다.
 *   - 모든 엔드포인트는 소유자 인증을 요구하며, 관리자 예외 정책은 추후 버전에서 별도 정의한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
@RestController
public class ReplayController {

    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    /**
     * 설명:
     *   - 현재 로그인한 사용자의 리플레이 목록을 페이징 형태로 반환한다.
     */
    @GetMapping("/api/replays")
    public ReplayPageResponse listMyReplays(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return replayService.listMyReplays(user.id(), page, size);
    }

    /**
     * 설명:
     *   - 개별 리플레이 메타데이터를 반환한다.
     */
    @GetMapping("/api/replays/{replayId}")
    public ReplayDetailResponse getReplay(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long replayId) {
        return replayService.getReplay(user.id(), replayId);
    }

    /**
     * 설명:
     *   - 특정 매치 ID에 대한 리플레이 메타데이터를 조회한다.
     */
    @GetMapping("/api/matches/{matchId}/replay")
    public ReplayDetailResponse getReplayByMatch(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long matchId) {
        return replayService.getReplayByMatch(user.id(), matchId);
    }

    /**
     * 설명:
     *   - JSON Lines 포맷으로 저장된 리플레이 이벤트 파일을 스트리밍한다.
     */
    @GetMapping("/api/replays/{replayId}/events")
    public ResponseEntity<InputStreamResource> streamReplay(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long replayId) {
        InputStreamResource resource = replayService.streamEvents(user.id(), replayId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"replay-" + replayId + ".jsonl\"")
                .contentType(MediaType.valueOf("application/x-ndjson"))
                .body(resource);
    }
}
