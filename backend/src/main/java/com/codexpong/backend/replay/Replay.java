package com.codexpong.backend.replay;

import com.codexpong.backend.game.GameResult;
import com.codexpong.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * [엔티티] backend/src/main/java/com/codexpong/backend/replay/Replay.java
 * 설명:
 *   - v0.11.0 리플레이 녹화 결과를 영속화하는 엔티티다.
 *   - 매치(GameResult)와 사용자 소유자, 저장 위치/체크섬 메타데이터를 포함한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
@Entity
@Table(name = "replays")
public class Replay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "replay_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id")
    private GameResult match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private long durationMs;

    @Column(nullable = false, length = 50)
    private String eventFormat;

    @Column(nullable = false, length = 500)
    private String storageUri;

    @Column(nullable = false, length = 128)
    private String checksum;

    protected Replay() {
    }

    public Replay(GameResult match, User owner, long durationMs, String eventFormat, String storageUri, String checksum) {
        this.match = match;
        this.owner = owner;
        this.durationMs = durationMs;
        this.eventFormat = eventFormat;
        this.storageUri = storageUri;
        this.checksum = checksum;
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

    public GameResult getMatch() {
        return match;
    }

    public User getOwner() {
        return owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getEventFormat() {
        return eventFormat;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public String getChecksum() {
        return checksum;
    }
}
