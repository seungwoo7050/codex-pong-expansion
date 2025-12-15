package com.codexpong.backend.social.domain;

import com.codexpong.backend.game.domain.MatchType;
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

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/social/domain/GameInvite.java
 * 설명:
 *   - 친구에게 발송한 게임 초대 정보를 저장하고 수락/거절 결과와 매치 타입을 기록한다.
 *   - 초대가 수락되면 roomId 필드에 생성된 게임 방 식별자를 연결한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
@Entity
@Table(name = "game_invites")
public class GameInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InviteStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchType matchType;

    @Column(length = 120)
    private String roomId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime respondedAt;

    protected GameInvite() {
    }

    public GameInvite(User sender, User receiver, MatchType matchType) {
        this.sender = sender;
        this.receiver = receiver;
        this.matchType = matchType;
        this.status = InviteStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void accept(String roomId) {
        this.status = InviteStatus.ACCEPTED;
        this.roomId = roomId;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = InviteStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public String getRoomId() {
        return roomId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
}
