package com.codexpong.backend.job;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/job/JobController.java
 * 설명:
 *   - 잡 상태 조회, 목록 필터링, 결과 다운로드 엔드포인트를 제공한다.
 *   - 인증된 사용자 기준으로만 접근을 허용하며, 파일 다운로드 시 Content-Disposition을 명시한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
@RestController
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/api/jobs")
    public JobPageResponse listMyJobs(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        Optional<JobStatus> statusFilter = parseStatus(status);
        Optional<JobType> typeFilter = parseType(type);
        return jobService.listJobs(user.id(), typeFilter, statusFilter, page, size);
    }

    @GetMapping("/api/jobs/{jobId}")
    public JobResponse getJob(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long jobId) {
        return jobService.getJob(user.id(), jobId);
    }

    @GetMapping("/api/jobs/{jobId}/result")
    public ResponseEntity<InputStreamResource> downloadResult(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long jobId) throws IOException {
        Path file = jobService.resolveResultPath(user.id(), jobId);
        MediaType mediaType = detectMediaType(file);
        InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    private Optional<JobStatus> parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JobStatus.valueOf(status));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Optional<JobType> parseType(String type) {
        if (type == null || type.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JobType.valueOf(type));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private MediaType detectMediaType(Path file) throws IOException {
        String detected = Files.probeContentType(file);
        if (detected != null) {
            return MediaType.parseMediaType(detected);
        }
        if (file.toString().endsWith(".mp4")) {
            return MediaType.valueOf("video/mp4");
        }
        if (file.toString().endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (file.toString().endsWith(".jpg") || file.toString().endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
