package com.codexpong.backend.job;

import com.codexpong.backend.replay.Replay;
import com.codexpong.backend.replay.ReplayRepository;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/job/JobService.java
 * 설명:
 *   - 리플레이 내보내기 잡 생성, 상태 조회, 결과 다운로드, 워커 피드백 반영을 담당한다.
 *   - Redis Streams 디스패처/리스너와 WebSocket 퍼블리셔를 연결해 엔드 투 엔드 파이프라인을 완성한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ReplayRepository replayRepository;
    private final UserRepository userRepository;
    private final JobQueuePublisher jobQueuePublisher;
    private final JobEventPublisher jobEventPublisher;
    private final JobExportProperties exportProperties;
    private Path exportBasePath;
    private Path exportBaseRealPath;

    public JobService(JobRepository jobRepository, ReplayRepository replayRepository,
            UserRepository userRepository, JobQueuePublisher jobQueuePublisher,
            JobEventPublisher jobEventPublisher, JobExportProperties exportProperties) {
        this.jobRepository = jobRepository;
        this.replayRepository = replayRepository;
        this.userRepository = userRepository;
        this.jobQueuePublisher = jobQueuePublisher;
        this.jobEventPublisher = jobEventPublisher;
        this.exportProperties = exportProperties;
    }

    @PostConstruct
    public void prepareExportPath() {
        exportBasePath = Paths.get(exportProperties.getPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(exportBasePath);
            exportBaseRealPath = exportBasePath.toRealPath();
        } catch (Exception ignored) {
            exportBaseRealPath = exportBasePath;
        }
    }

    @Transactional
    public JobCreateResponse requestMp4(Long ownerId, Long replayId) {
        return createAndDispatch(ownerId, replayId, JobType.REPLAY_EXPORT_MP4);
    }

    @Transactional
    public JobCreateResponse requestThumbnail(Long ownerId, Long replayId) {
        return createAndDispatch(ownerId, replayId, JobType.REPLAY_THUMBNAIL);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(Long ownerId, Long jobId) {
        User owner = findOwner(ownerId);
        Job job = jobRepository.findByIdAndOwner(jobId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "잡을 찾을 수 없습니다."));
        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public JobPageResponse listJobs(Long ownerId, Optional<JobType> jobType, Optional<JobStatus> status, int page, int size) {
        User owner = findOwner(ownerId);
        var pageable = PageRequest.of(page, size);
        var pageResult = selectPage(owner, jobType, status, pageable);
        return new JobPageResponse(pageResult.map(JobResponse::from).getContent(), pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages());
    }

    private org.springframework.data.domain.Page<Job> selectPage(User owner, Optional<JobType> jobType,
            Optional<JobStatus> status, org.springframework.data.domain.Pageable pageable) {
        boolean hasType = jobType.isPresent();
        boolean hasStatus = status.isPresent();
        if (hasType && hasStatus) {
            return jobRepository.findByOwnerAndJobTypeAndStatus(owner, jobType.get(), status.get(), pageable);
        }
        if (hasType) {
            return jobRepository.findByOwnerAndJobType(owner, jobType.get(), pageable);
        }
        if (hasStatus) {
            return jobRepository.findByOwnerAndStatus(owner, status.get(), pageable);
        }
        return jobRepository.findByOwner(owner, pageable);
    }

    @Transactional(readOnly = true)
    public Path resolveResultPath(Long ownerId, Long jobId) {
        User owner = findOwner(ownerId);
        Job job = jobRepository.findByIdAndOwner(jobId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "잡을 찾을 수 없습니다."));
        if (job.getStatus() != JobStatus.SUCCEEDED || job.getResultUri() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "완료된 잡만 다운로드할 수 있습니다.");
        }
        Path path = Paths.get(job.getResultUri()).toAbsolutePath().normalize();
        if (!isWithinExportRoot(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용된 경로 밖의 파일은 제공하지 않습니다.");
        }
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "결과 파일을 찾을 수 없습니다.");
        }
        return path;
    }

    @Transactional
    public void handleProgress(JobProgressMessage message) {
        jobRepository.findById(message.jobId()).ifPresent(job -> {
            if (isTerminal(job.getStatus())) {
                return;
            }
            job.markRunning(message.progress());
            jobRepository.save(job);
            jobEventPublisher.publishProgress(job.getOwner().getId(), message);
        });
    }

    @Transactional
    public void handleResult(JobResultMessage message) {
        jobRepository.findById(message.jobId()).ifPresent(job -> {
            if (isTerminal(job.getStatus())) {
                return;
            }
            if (message.status() == JobStatus.SUCCEEDED) {
                Path resultPath = Paths.get(message.resultUri()).toAbsolutePath().normalize();
                if (!isWithinExportRoot(resultPath)) {
                    JobResultMessage rejected = new JobResultMessage(message.jobId(), JobStatus.FAILED,
                            resultPath.toString(), message.checksum(), "INVALID_OUTPUT_PATH",
                            "허용된 출력 루트 밖의 결과입니다.");
                    job.fail(rejected.errorCode(), rejected.errorMessage());
                    jobRepository.save(job);
                    jobEventPublisher.publishFailed(job.getOwner().getId(), rejected);
                    return;
                }
                job.succeed(resultPath.toString(), message.checksum());
                jobRepository.save(job);
                jobEventPublisher.publishCompleted(job.getOwner().getId(), message);
                return;
            }
            if (message.status() == JobStatus.CANCELLED) {
                job.cancel(message.errorCode(), message.errorMessage());
                jobRepository.save(job);
                jobEventPublisher.publishFailed(job.getOwner().getId(), message);
                return;
            }
            job.fail(message.errorCode(), message.errorMessage());
            jobRepository.save(job);
            jobEventPublisher.publishFailed(job.getOwner().getId(), message);
        });
    }

    private JobCreateResponse createAndDispatch(Long ownerId, Long replayId, JobType jobType) {
        User owner = findOwner(ownerId);
        Replay replay = replayRepository.findByIdAndOwner(replayId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리플레이를 찾을 수 없습니다."));
        Job job = jobRepository.save(new Job(jobType, owner, replay));
        String outputPath = buildOutputPath(job);
        Map<String, String> options = new HashMap<>();
        options.put("inputPath", replay.getStorageUri());
        options.put("outputPath", outputPath);
        options.put("durationMs", String.valueOf(replay.getDurationMs()));
        options.put("ownerId", String.valueOf(owner.getId()));
        LocalDateTime created = Optional.ofNullable(job.getCreatedAt())
                .orElseGet(() -> LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        ZonedDateTime createdAt = created.atZone(ZoneId.of("Asia/Seoul"));
        options.put("createdAt", createdAt.toString());
        jobQueuePublisher.publish(new JobRequestMessage(job.getId(), jobType, replay.getId(), options));
        return new JobCreateResponse(job.getId());
    }

    private User findOwner(Long ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private boolean isTerminal(JobStatus status) {
        return status == JobStatus.SUCCEEDED || status == JobStatus.FAILED || status == JobStatus.CANCELLED;
    }

    private boolean isWithinExportRoot(Path path) {
        try {
            Path normalized = path.toAbsolutePath().normalize();
            if (!normalized.startsWith(exportBasePath)) {
                return false;
            }
            Path realBase = exportBaseRealPath != null ? exportBaseRealPath : exportBasePath.toRealPath();
            if (Files.exists(normalized)) {
                Path realTarget = normalized.toRealPath();
                return realTarget.startsWith(realBase);
            }
            return normalized.startsWith(realBase);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String buildOutputPath(Job job) {
        String extension = job.getJobType() == JobType.REPLAY_EXPORT_MP4 ? ".mp4" : ".png";
        return exportBasePath.resolve("job-" + job.getId() + extension).toString();
    }
}
