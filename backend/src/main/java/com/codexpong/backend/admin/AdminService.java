package com.codexpong.backend.admin;

import com.codexpong.backend.admin.dto.AdminStatsResponse;
import com.codexpong.backend.admin.dto.AdminUserResponse;
import com.codexpong.backend.admin.dto.ModerationRequest;
import com.codexpong.backend.chat.domain.ChatMute;
import com.codexpong.backend.chat.service.ChatModerationService;
import com.codexpong.backend.game.GameResultRepository;
import com.codexpong.backend.game.GameResultResponse;
import com.codexpong.backend.game.service.GameRoomService;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/admin/AdminService.java
 * 설명:
 *   - 관리자용 계정 제재, 상태 조회, 전적 조회를 담당한다.
 *   - v0.9.0 모니터링 스택과 연동되어 기본 통계를 제공한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.9.0-admin-and-ops.md
 */
@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final GameResultRepository gameResultRepository;
    private final ChatModerationService chatModerationService;
    private final GameRoomService gameRoomService;

    public AdminService(UserRepository userRepository, GameResultRepository gameResultRepository,
            ChatModerationService chatModerationService, GameRoomService gameRoomService) {
        this.userRepository = userRepository;
        this.gameResultRepository = gameResultRepository;
        this.chatModerationService = chatModerationService;
        this.gameRoomService = gameRoomService;
    }

    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt))
                .map(user -> AdminUserResponse.from(user, activeMuteExpiry(user.getId()).orElse(null)))
                .toList();
    }

    public List<GameResultResponse> recentMatches() {
        return gameResultRepository.findTop20ByOrderByFinishedAtDesc().stream()
                .map(GameResultResponse::from)
                .toList();
    }

    public List<GameResultResponse> userMatches(Long userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return gameResultRepository.findTop20ByPlayerAOrPlayerBOrderByFinishedAtDesc(target, target).stream()
                .map(GameResultResponse::from)
                .toList();
    }

    public AdminUserResponse moderate(Long userId, ModerationRequest request) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        switch (request.getAction()) {
            case BAN -> target.ban(request.getReason());
            case SUSPEND -> target.suspendUntil(now.plusMinutes(request.getDurationMinutes()));
            case MUTE -> chatModerationService.muteUser(userId, request.getReason(),
                    request.getDurationMinutes() > 0 ? now.plusMinutes(request.getDurationMinutes()) : null);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 제재 유형입니다.");
        }
        User saved = userRepository.save(target);
        return AdminUserResponse.from(saved, activeMuteExpiry(userId).orElse(null));
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        return new AdminStatsResponse(
                userRepository.count(),
                gameResultRepository.count(),
                gameRoomService.activeRoomCount(),
                gameRoomService.totalSpectatorCount()
        );
    }

    private Optional<LocalDateTime> activeMuteExpiry(Long userId) {
        return chatModerationService.activeMute(userId).map(ChatMute::getExpiresAt);
    }
}
