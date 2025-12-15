package com.codexpong.backend.chat.repository;

import com.codexpong.backend.chat.domain.ChatChannelType;
import com.codexpong.backend.chat.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * [레포지토리] backend/src/main/java/com/codexpong/backend/chat/repository/ChatMessageRepository.java
 * 설명:
 *   - 채팅 메시지 조회/저장을 담당하는 JPA 레포지토리다.
 *   - DM, 로비, 매치별 조회 쿼리를 구분해 제공한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop50ByChannelTypeAndChannelKeyOrderByCreatedAtDesc(ChatChannelType channelType, String channelKey);

    @Query("SELECT m FROM ChatMessage m WHERE m.channelType = :type AND m.channelKey = :key ORDER BY m.createdAt ASC")
    List<ChatMessage> findChannelHistory(@Param("type") ChatChannelType type, @Param("key") String key);

    @Query("SELECT m FROM ChatMessage m WHERE m.channelType = 'DM' AND ((m.sender.id = :userA AND m.recipient.id = :userB)"
            + " OR (m.sender.id = :userB AND m.recipient.id = :userA)) ORDER BY m.createdAt ASC")
    List<ChatMessage> findDmHistory(@Param("userA") Long userA, @Param("userB") Long userB);

    Optional<ChatMessage> findTopByChannelTypeAndChannelKeyOrderByCreatedAtDesc(ChatChannelType channelType, String channelKey);
}
