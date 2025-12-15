package com.codexpong.backend.game.dto;

import com.codexpong.backend.user.domain.User;

/**
 * [DTO] backend/src/main/java/com/codexpong/backend/game/dto/LeaderboardEntryResponse.java
 * 설명:
 *   - 리더보드 상위 사용자의 식별자, 닉네임, 레이팅을 정렬 순위와 함께 전달한다.
 *   - v0.4.0 기본 랭킹 시스템에서 사용한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 */
public record LeaderboardEntryResponse(int rank, Long userId, String nickname, Integer rating, String avatarUrl) {

    public static LeaderboardEntryResponse from(User user, int rank) {
        return new LeaderboardEntryResponse(rank, user.getId(), user.getNickname(), user.getRating(), user.getAvatarUrl());
    }
}
