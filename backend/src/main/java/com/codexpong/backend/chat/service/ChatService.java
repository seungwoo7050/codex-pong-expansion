package com.codexpong.backend.chat.service;

import com.codexpong.backend.chat.domain.ChatChannelType;
import com.codexpong.backend.chat.domain.ChatMessage;
import com.codexpong.backend.chat.dto.ChatMessageResponse;
import com.codexpong.backend.chat.dto.ChatSocketMessage;
import com.codexpong.backend.chat.repository.ChatMessageRepository;
import com.codexpong.backend.social.repository.BlockRepository;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/chat/service/ChatService.java
 * 설명:
 *   - DM/로비/매치 채팅 생성과 조회를 처리한다.
 *   - 차단 및 뮤트 상태를 검증하고, 저장된 메시지를 응답 DTO로 변환한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
@Service
@Transactional
public class ChatService {

    private static final String LOBBY_KEY = "GLOBAL_LOBBY";

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final ChatModerationService chatModerationService;

    public ChatService(ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            BlockRepository blockRepository,
            ChatModerationService chatModerationService) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.chatModerationService = chatModerationService;
    }

    public ChatMessageResponse sendDm(Long senderId, Long targetUserId, String content) {
        User sender = getUser(senderId);
        User target = getUser(targetUserId);
        ensureNotBlocked(sender, target);
        ensureNotMuted(sender.getId());
        String channelKey = buildDmKey(sender.getId(), target.getId());
        ChatMessage saved = chatMessageRepository.save(
                new ChatMessage(ChatChannelType.DM, channelKey, sender, target, content));
        return new ChatMessageResponse(saved);
    }

    public ChatMessageResponse sendLobby(Long senderId, String content) {
        User sender = getUser(senderId);
        ensureNotMuted(sender.getId());
        ChatMessage saved = chatMessageRepository.save(
                new ChatMessage(ChatChannelType.LOBBY, LOBBY_KEY, sender, null, content));
        return new ChatMessageResponse(saved);
    }

    public ChatMessageResponse sendMatch(Long senderId, String roomId, String content) {
        if (roomId == null || roomId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방 번호가 필요합니다.");
        }
        User sender = getUser(senderId);
        ensureNotMuted(sender.getId());
        ChatMessage saved = chatMessageRepository.save(
                new ChatMessage(ChatChannelType.MATCH, roomId, sender, null, content));
        return new ChatMessageResponse(saved);
    }

    public List<ChatMessageResponse> dmHistory(Long userId, Long targetUserId) {
        getUser(targetUserId);
        ensureNotBlocked(getUser(userId), getUser(targetUserId));
        return chatMessageRepository.findDmHistory(userId, targetUserId).stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    public List<ChatMessageResponse> lobbyHistory() {
        return chatMessageRepository.findChannelHistory(ChatChannelType.LOBBY, LOBBY_KEY).stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    public List<ChatMessageResponse> matchHistory(String roomId) {
        return chatMessageRepository.findChannelHistory(ChatChannelType.MATCH, roomId).stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    public String buildDmKey(Long a, Long b) {
        List<Long> sorted = List.of(a, b).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        return sorted.get(0) + "-" + sorted.get(1);
    }

    public void ensureNotBlocked(User sender, User target) {
        boolean blocked = blockRepository.existsByBlocker_IdAndBlocked_Id(sender.getId(), target.getId())
                || blockRepository.existsByBlocker_IdAndBlocked_Id(target.getId(), sender.getId());
        if (blocked) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차단된 사용자와는 대화할 수 없습니다.");
        }
    }

    private void ensureNotMuted(Long userId) {
        chatModerationService.cleanupExpiredMutes();
        if (chatModerationService.isMuted(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "뮤트 상태에서는 메시지를 보낼 수 없습니다.");
        }
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    public ChatMessageResponse handleSocketCommand(Long senderId, ChatSocketMessage command) {
        if (command == null || command.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 형식이 잘못되었습니다.");
        }
        return switch (command.getType()) {
            case "DM_SEND" -> sendDm(senderId, command.getTargetUserId(), command.getContent());
            case "LOBBY_SEND" -> sendLobby(senderId, command.getContent());
            case "MATCH_SEND" -> sendMatch(senderId, command.getRoomId(), command.getContent());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 채팅 명령입니다.");
        };
    }
}
