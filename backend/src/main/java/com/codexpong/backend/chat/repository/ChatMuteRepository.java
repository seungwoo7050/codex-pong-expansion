package com.codexpong.backend.chat.repository;

import com.codexpong.backend.chat.domain.ChatMute;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * [레포지토리] backend/src/main/java/com/codexpong/backend/chat/repository/ChatMuteRepository.java
 * 설명:
 *   - 채팅 뮤트 정보를 조회/삭제하기 위한 저장소다.
 *   - 만료 시간이 지난 항목은 조회 시 필터링한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
public interface ChatMuteRepository extends JpaRepository<ChatMute, Long> {

    @Query("SELECT m FROM ChatMute m WHERE m.mutedUser.id = :userId")
    List<ChatMute> findByMutedUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM ChatMute m WHERE m.mutedUser.id = :userId AND (m.expiresAt IS NULL OR m.expiresAt > :now)")
    Optional<ChatMute> findActiveMute(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
