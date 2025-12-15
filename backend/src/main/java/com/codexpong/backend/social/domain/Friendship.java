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
 * [엔티티] backend/src/main/java/com/codexpong/backend/social/domain/Friendship.java
 * 설명:
 *   - 친구 관계를 양방향 쌍으로 저장한다. userA, userB 순서는 id 크기로 정규화해 중복을 방지한다.
 *   - 친구 목록 조회 시 생성 시각을 기반으로 정렬하거나 표시할 수 있다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
@Entity
@Table(name = "friendships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_a_id", "user_b_id"})
})
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_a_id")
    private User userA;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_b_id")
    private User userB;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Friendship() {
    }

    public Friendship(User first, User second) {
        if (first.getId() < second.getId()) {
            this.userA = first;
            this.userB = second;
        } else {
            this.userA = second;
            this.userB = first;
        }
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUserA() {
        return userA;
    }

    public User getUserB() {
        return userB;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
