package com.codexpong.backend.game.service;

import com.codexpong.backend.game.domain.GameRoom;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.user.domain.User;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Service;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/game/service/MatchmakingService.java
 * 설명:
 *   - v0.3.0 빠른 대전 큐를 관리하고 두 사용자를 매칭해 GameRoom을 생성한다.
 *   - 대기열은 메모리 기반이며 동일 사용자의 중복 대기를 방지한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 */
@Service
public class MatchmakingService {

    private final Map<MatchType, Queue<User>> waitingQueues = new ConcurrentHashMap<>();
    private final Map<String, MatchTicket> tickets = new ConcurrentHashMap<>();
    private final GameRoomService gameRoomService;

    public MatchmakingService(GameRoomService gameRoomService) {
        this.gameRoomService = gameRoomService;
    }

    /**
     * 설명:
     *   - 사용자를 빠른 대전 큐에 추가하고 즉시 매칭 가능한 경우 방을 생성한다.
     */
    public MatchTicket enqueue(User user, MatchType matchType) {
        Optional<MatchTicket> existing = tickets.values().stream()
                .filter(ticket -> ticket.userId().equals(user.getId()) && ticket.matchType() == matchType
                        && !ticket.status().equals("CANCELLED"))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        User opponent = queueFor(matchType).poll();
        if (opponent != null && !opponent.getId().equals(user.getId())) {
            GameRoom room = gameRoomService.createRoom(opponent, user, matchType);
            MatchTicket opponentTicket = new MatchTicket(UUID.randomUUID().toString(), opponent.getId(), matchType,
                    "MATCHED", room.getRoomId());
            tickets.put(opponentTicket.ticketId(), opponentTicket);
            MatchTicket myTicket = new MatchTicket(UUID.randomUUID().toString(), user.getId(), matchType, "MATCHED",
                    room.getRoomId());
            tickets.put(myTicket.ticketId(), myTicket);
            return myTicket;
        }
        queueFor(matchType).offer(user);
        MatchTicket ticket = new MatchTicket(UUID.randomUUID().toString(), user.getId(), matchType, "WAITING", null);
        tickets.put(ticket.ticketId(), ticket);
        return ticket;
    }

    public Optional<MatchTicket> findTicket(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    private Queue<User> queueFor(MatchType matchType) {
        return waitingQueues.computeIfAbsent(matchType, key -> new ConcurrentLinkedQueue<>());
    }

    public record MatchTicket(String ticketId, Long userId, MatchType matchType, String status, String roomId) {
    }
}
