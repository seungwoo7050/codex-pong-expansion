package com.codexpong.backend.chat.domain;

import com.codexpong.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/chat/domain/ChatMute.java
 * 설명:
 *   - 특정 사용자를 채팅에서 일시적으로 제한하기 위한 기본 뮤트 정보를 보관한다.
 *   - 만료 시간이 지난 레코드는 조회 시 무시된다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
@Entity
@Table(name = "chat_mutes")
public class ChatMute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "muted_user_id", nullable = false)
    private User mutedUser;

    @Column(length = 200)
    private String reason;

    @Column
    private LocalDateTime expiresAt;

    protected ChatMute() {
    }

    public ChatMute(User mutedUser, String reason, LocalDateTime expiresAt) {
        this.mutedUser = mutedUser;
        this.reason = reason;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public User getMutedUser() {
        return mutedUser;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}
