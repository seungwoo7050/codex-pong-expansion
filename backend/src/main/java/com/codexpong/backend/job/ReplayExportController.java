package com.codexpong.backend.job;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/job/ReplayExportController.java
 * 설명:
 *   - 리플레이 뷰어에서 요청하는 MP4/썸네일 내보내기 잡 생성을 처리한다.
 *   - 잡 생성 즉시 jobId를 반환하고 큐 디스패처가 워커에게 작업을 위임한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
@RestController
public class ReplayExportController {

    private final JobService jobService;

    public ReplayExportController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/api/replays/{replayId}/exports/mp4")
    public JobCreateResponse requestMp4Export(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long replayId) {
        return jobService.requestMp4(user.id(), replayId);
    }

    @PostMapping("/api/replays/{replayId}/exports/thumbnail")
    public JobCreateResponse requestThumbnailExport(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long replayId) {
        return jobService.requestThumbnail(user.id(), replayId);
    }
}
