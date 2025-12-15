package com.codexpong.backend.job;

import com.codexpong.backend.replay.Replay;
import com.codexpong.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/job/Job.java
 * 설명:
 *   - v0.12.0 리플레이 내보내기 잡의 상태, 소유자, 대상 리플레이, 결과 경로를 저장한다.
 *   - 워커 진행 상황에 따라 상태 전이를 관리하며, 타임스탬프는 KST 기준으로 기록한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private JobType jobType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_replay_id")
    private Replay targetReplay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.QUEUED;

    @Column(nullable = false)
    private int progress = 0;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Column(length = 100)
    private String errorCode;

    @Column(length = 500)
    private String errorMessage;

    @Column(length = 500)
    private String resultUri;

    @Column(length = 128)
    private String resultChecksum;

    protected Job() {
    }

    public Job(JobType jobType, User owner, Replay targetReplay) {
        this.jobType = jobType;
        this.owner = owner;
        this.targetReplay = targetReplay;
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }
    }

    public Long getId() {
        return id;
    }

    public JobType getJobType() {
        return jobType;
    }

    public User getOwner() {
        return owner;
    }

    public Replay getTargetReplay() {
        return targetReplay;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getResultUri() {
        return resultUri;
    }

    public String getResultChecksum() {
        return resultChecksum;
    }

    public void markRunning(int progress) {
        if (status == JobStatus.QUEUED) {
            this.startedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }
        this.status = JobStatus.RUNNING;
        this.progress = clamp(progress);
    }

    public void updateProgress(int progress) {
        this.progress = clamp(progress);
    }

    public void succeed(String resultUri, String checksum) {
        this.status = JobStatus.SUCCEEDED;
        this.progress = 100;
        this.resultUri = resultUri;
        this.resultChecksum = checksum;
        this.endedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void fail(String errorCode, String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.endedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void cancel(String errorCode, String errorMessage) {
        this.status = JobStatus.CANCELLED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.endedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    private int clamp(int value) {
        return Math.min(100, Math.max(0, value));
    }
}
