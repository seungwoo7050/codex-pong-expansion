package com.codexpong.backend.user.domain;

import com.codexpong.backend.common.KstDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/user/domain/User.java
 * 설명:
 *   - 계정 및 기본 프로필 정보를 보관하는 사용자 엔티티다.
 *   - v0.4.0에서 랭크 시스템 적용을 위해 레이팅 필드를 추가하고 기본값을 관리한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.2.0: 사용자 엔티티 및 타임스탬프 관리 추가
 *   - v0.4.0: 레이팅 필드와 접근자 추가
 *   - v0.9.0: 밴/정지 상태 필드와 헬퍼 메서드 추가
 *   - v0.10.0: OAuth 공급자 식별자와 이메일/로케일 필드 추가, 타임존 일원화
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, length = 120)
    private String password;

    @Column(length = 30)
    private String authProvider;

    @Column(length = 120)
    private String providerUserId;

    @Column(length = 120)
    private String email;

    @Column(length = 20)
    private String locale;

    @Column(nullable = false, length = 60)
    private String nickname;

    @Column(length = 255)
    private String avatarUrl;

    @Column(nullable = false, columnDefinition = "int default 1200")
    private Integer rating;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean banned;

    @Column(length = 255)
    private String banReason;

    @Column
    private LocalDateTime bannedAt;

    @Column
    private LocalDateTime suspendedUntil;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected User() {
    }

    public User(String username, String password, String nickname, String avatarUrl) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.rating = 1200;
    }

    public static User oauth(String provider, String providerUserId, String nickname, String email, String locale,
            String encodedPassword, String avatarUrl) {
        User user = new User(provider + "_" + providerUserId, encodedPassword, nickname, avatarUrl);
        user.authProvider = provider;
        user.providerUserId = providerUserId;
        user.email = email;
        user.locale = locale;
        return user;
    }

    public void updateRating(int nextRating) {
        this.rating = nextRating;
    }

    public void updateProfile(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = KstDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.rating == null) {
            this.rating = 1200;
        }
        if (this.suspendedUntil == null) {
            this.suspendedUntil = null;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KstDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getLocale() {
        return locale;
    }

    public Integer getRating() {
        return rating;
    }

    public boolean isBanned() {
        return banned;
    }

    public LocalDateTime getSuspendedUntil() {
        return suspendedUntil;
    }

    public LocalDateTime getBannedAt() {
        return bannedAt;
    }

    public String getBanReason() {
        return banReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isSuspended() {
        return suspendedUntil != null && suspendedUntil.isAfter(KstDateTime.now());
    }

    public void ban(String reason) {
        this.banned = true;
        this.banReason = reason;
        this.bannedAt = KstDateTime.now();
    }

    public void clearBan() {
        this.banned = false;
        this.banReason = null;
        this.bannedAt = null;
    }

    public void suspendUntil(LocalDateTime until) {
        this.suspendedUntil = until;
    }
}
