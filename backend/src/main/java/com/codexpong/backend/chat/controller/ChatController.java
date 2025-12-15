package com.codexpong.backend.chat.controller;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.chat.dto.ChatHistoryResponse;
import com.codexpong.backend.chat.dto.ChatMessageResponse;
import com.codexpong.backend.chat.dto.ChatSendRequest;
import com.codexpong.backend.chat.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/chat/controller/ChatController.java
 * 설명:
 *   - DM, 로비, 매치 채팅의 저장/조회 REST API를 제공한다.
 *   - WebSocket 이전 초동 화면에서 사용할 히스토리 조회 API도 함께 노출한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/dm/{targetUserId}")
    public ChatMessageResponse sendDm(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long targetUserId,
            @Valid @RequestBody ChatSendRequest request) {
        return chatService.sendDm(user.id(), targetUserId, request.getContent());
    }

    @GetMapping("/dm/{targetUserId}")
    public ChatHistoryResponse dmHistory(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long targetUserId) {
        List<ChatMessageResponse> messages = chatService.dmHistory(user.id(), targetUserId);
        return new ChatHistoryResponse(messages);
    }

    @PostMapping("/lobby")
    public ChatMessageResponse sendLobby(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChatSendRequest request) {
        return chatService.sendLobby(user.id(), request.getContent());
    }

    @GetMapping("/lobby")
    public ChatHistoryResponse lobbyHistory() {
        return new ChatHistoryResponse(chatService.lobbyHistory());
    }

    @PostMapping("/match/{roomId}")
    public ChatMessageResponse sendMatch(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String roomId,
            @Valid @RequestBody ChatSendRequest request) {
        return chatService.sendMatch(user.id(), roomId, request.getContent());
    }

    @GetMapping("/match/{roomId}")
    public ChatHistoryResponse matchHistory(@PathVariable String roomId) {
        return new ChatHistoryResponse(chatService.matchHistory(roomId));
    }
}
