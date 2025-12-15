package com.codexpong.backend.social.domain;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/social/domain/Block.java
 * 설명:
 *   - 사용자가 특정 사용자를 차단한 기록을 저장한다.
 *   - 차단 관계는 단방향으로 저장되며, 중복 차단을 방지하기 위해 Unique 제약을 둔다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
@Entity
@Table(name = "blocks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
})
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "blocker_id")
    private User blocker;

    @ManyToOne(optional = false)
    @JoinColumn(name = "blocked_id")
    private User blocked;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Block() {
    }

    public Block(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getBlocker() {
        return blocker;
    }

    public User getBlocked() {
        return blocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
