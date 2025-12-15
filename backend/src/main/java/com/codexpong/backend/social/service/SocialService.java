package com.codexpong.backend.social.service;

import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.game.service.GameRoomService;
import com.codexpong.backend.social.domain.Block;
import com.codexpong.backend.social.domain.FriendRequest;
import com.codexpong.backend.social.domain.FriendRequestStatus;
import com.codexpong.backend.social.domain.Friendship;
import com.codexpong.backend.social.domain.GameInvite;
import com.codexpong.backend.social.domain.InviteStatus;
import com.codexpong.backend.social.dto.BlockedUserResponse;
import com.codexpong.backend.social.dto.FriendRequestListResponse;
import com.codexpong.backend.social.dto.FriendRequestResponse;
import com.codexpong.backend.social.dto.FriendSummaryResponse;
import com.codexpong.backend.social.dto.GameInviteResponse;
import com.codexpong.backend.social.repository.BlockRepository;
import com.codexpong.backend.social.repository.FriendRequestRepository;
import com.codexpong.backend.social.repository.FriendshipRepository;
import com.codexpong.backend.social.repository.GameInviteRepository;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/social/service/SocialService.java
 * 설명:
 *   - 친구 요청/수락, 차단, 게임 초대를 처리하는 핵심 도메인 서비스다.
 *   - 차단 상태나 기존 친구 관계를 검사해 중복 흐름을 방지하고, WebSocket 구독자에게 이벤트를 발행한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
@Service
@Transactional
public class SocialService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final BlockRepository blockRepository;
    private final GameInviteRepository gameInviteRepository;
    private final GameRoomService gameRoomService;
    private final SocialEventPublisher socialEventPublisher;

    public SocialService(UserRepository userRepository,
            FriendshipRepository friendshipRepository,
            FriendRequestRepository friendRequestRepository,
            BlockRepository blockRepository,
            GameInviteRepository gameInviteRepository,
            GameRoomService gameRoomService,
            SocialEventPublisher socialEventPublisher) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.blockRepository = blockRepository;
        this.gameInviteRepository = gameInviteRepository;
        this.gameRoomService = gameRoomService;
        this.socialEventPublisher = socialEventPublisher;
    }

    public List<FriendSummaryResponse> listFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserA_IdOrUserB_Id(userId, userId);
        return friendships.stream()
                .map(friendship -> toFriendSummary(userId, friendship))
                .collect(Collectors.toList());
    }

    public FriendRequestListResponse listRequests(Long userId) {
        List<FriendRequestResponse> incoming = friendRequestRepository.findByReceiver_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FriendRequestResponse::from)
                .toList();
        List<FriendRequestResponse> outgoing = friendRequestRepository.findBySender_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FriendRequestResponse::from)
                .toList();
        return new FriendRequestListResponse(incoming, outgoing);
    }

    public FriendRequestResponse requestFriend(Long senderId, String targetUsername) {
        User sender = getUser(senderId);
        User receiver = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 사용자를 찾을 수 없습니다."));

        if (sender.getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }
        ensureNotBlocked(sender, receiver);
        if (areFriends(sender, receiver)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 친구 상태입니다.");
        }

        Optional<FriendRequest> incomingPending = friendRequestRepository
                .findBySender_IdAndReceiver_IdAndStatus(receiver.getId(), sender.getId(), FriendRequestStatus.PENDING);
        if (incomingPending.isPresent()) {
            FriendRequest request = incomingPending.get();
            request.accept();
            addFriendship(sender, receiver);
            FriendRequestResponse accepted = FriendRequestResponse.from(request);
            socialEventPublisher.publish(receiver.getId(), "FRIEND_ACCEPTED", accepted);
            socialEventPublisher.publish(sender.getId(), "FRIEND_ACCEPTED", accepted);
            return accepted;
        }

        Optional<FriendRequest> existing = friendRequestRepository
                .findBySender_IdAndReceiver_IdAndStatus(sender.getId(), receiver.getId(), FriendRequestStatus.PENDING);
        if (existing.isPresent()) {
            return FriendRequestResponse.from(existing.get());
        }

        FriendRequest request = friendRequestRepository.save(new FriendRequest(sender, receiver));
        FriendRequestResponse response = FriendRequestResponse.from(request);
        socialEventPublisher.publish(receiver.getId(), "FRIEND_REQUEST", response);
        return response;
    }

    public FriendRequestResponse acceptRequest(Long receiverId, Long requestId) {
        FriendRequest request = friendRequestRepository.findByIdAndReceiver_Id(requestId, receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없습니다."));
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 처리된 요청입니다.");
        }
        ensureNotBlocked(request.getReceiver(), request.getSender());
        request.accept();
        addFriendship(request.getSender(), request.getReceiver());
        FriendRequestResponse response = FriendRequestResponse.from(request);
        socialEventPublisher.publish(request.getSender().getId(), "FRIEND_ACCEPTED", response);
        socialEventPublisher.publish(request.getReceiver().getId(), "FRIEND_ACCEPTED", response);
        return response;
    }

    public FriendRequestResponse rejectRequest(Long receiverId, Long requestId) {
        FriendRequest request = friendRequestRepository.findByIdAndReceiver_Id(requestId, receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없습니다."));
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 처리된 요청입니다.");
        }
        request.reject();
        FriendRequestResponse response = FriendRequestResponse.from(request);
        socialEventPublisher.publish(request.getSender().getId(), "FRIEND_REJECTED", response);
        return response;
    }

    public List<BlockedUserResponse> listBlocks(Long blockerId) {
        return blockRepository.findByBlocker_Id(blockerId).stream()
                .map(BlockedUserResponse::from)
                .toList();
    }

    public BlockedUserResponse block(Long blockerId, String targetUsername) {
        User blocker = getUser(blockerId);
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 사용자를 찾을 수 없습니다."));
        if (blocker.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다.");
        }
        if (blockRepository.existsByBlocker_IdAndBlocked_Id(blocker.getId(), target.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 차단한 사용자입니다.");
        }
        removeFriendshipIfExists(blocker, target);
        friendRequestRepository.deleteBySender_IdAndReceiver_Id(blocker.getId(), target.getId());
        friendRequestRepository.deleteBySender_IdAndReceiver_Id(target.getId(), blocker.getId());
        GameInvite pending = gameInviteRepository
                .findBySender_IdAndReceiver_IdAndStatus(blocker.getId(), target.getId(), InviteStatus.PENDING)
                .orElse(null);
        if (pending != null) {
            pending.reject();
        }
        GameInvite opposite = gameInviteRepository
                .findBySender_IdAndReceiver_IdAndStatus(target.getId(), blocker.getId(), InviteStatus.PENDING)
                .orElse(null);
        if (opposite != null) {
            opposite.reject();
        }
        Block block = blockRepository.save(new Block(blocker, target));
        return BlockedUserResponse.from(block);
    }

    public void unblock(Long blockerId, Long blockedUserId) {
        Block block = blockRepository.findByBlocker_IdAndBlocked_Id(blockerId, blockedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "차단 내역을 찾을 수 없습니다."));
        blockRepository.delete(block);
    }

    public GameInviteResponse sendInvite(Long senderId, Long targetUserId) {
        User sender = getUser(senderId);
        User receiver = getUser(targetUserId);
        ensureNotBlocked(sender, receiver);
        if (!areFriends(sender, receiver)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "친구에게만 초대를 보낼 수 있습니다.");
        }
        Optional<GameInvite> existing = gameInviteRepository
                .findBySender_IdAndReceiver_IdAndStatus(sender.getId(), receiver.getId(), InviteStatus.PENDING);
        if (existing.isPresent()) {
            return GameInviteResponse.from(existing.get());
        }
        GameInvite invite = gameInviteRepository.save(new GameInvite(sender, receiver, MatchType.NORMAL));
        GameInviteResponse response = GameInviteResponse.from(invite);
        socialEventPublisher.publish(receiver.getId(), "GAME_INVITE", response);
        return response;
    }

    public GameInviteResponse acceptInvite(Long receiverId, Long inviteId) {
        GameInvite invite = gameInviteRepository.findByIdAndReceiver_Id(inviteId, receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."));
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 처리된 초대입니다.");
        }
        ensureNotBlocked(invite.getReceiver(), invite.getSender());
        String roomId = gameRoomService.createRoom(invite.getSender(), invite.getReceiver(), invite.getMatchType())
                .getRoomId();
        invite.accept(roomId);
        GameInviteResponse response = GameInviteResponse.from(invite);
        socialEventPublisher.publish(invite.getSender().getId(), "INVITE_ACCEPTED", response);
        socialEventPublisher.publish(invite.getReceiver().getId(), "INVITE_ACCEPTED", response);
        return response;
    }

    public GameInviteResponse rejectInvite(Long receiverId, Long inviteId) {
        GameInvite invite = gameInviteRepository.findByIdAndReceiver_Id(inviteId, receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."));
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 처리된 초대입니다.");
        }
        invite.reject();
        GameInviteResponse response = GameInviteResponse.from(invite);
        socialEventPublisher.publish(invite.getSender().getId(), "INVITE_REJECTED", response);
        return response;
    }

    public List<GameInviteResponse> listIncomingInvites(Long receiverId) {
        return gameInviteRepository.findByReceiver_IdAndStatusOrderByCreatedAtDesc(receiverId, InviteStatus.PENDING)
                .stream()
                .map(GameInviteResponse::from)
                .toList();
    }

    public List<GameInviteResponse> listOutgoingInvites(Long senderId) {
        return gameInviteRepository.findBySender_IdAndStatusOrderByCreatedAtDesc(senderId, InviteStatus.PENDING)
                .stream()
                .map(GameInviteResponse::from)
                .toList();
    }

    private FriendSummaryResponse toFriendSummary(Long requesterId, Friendship friendship) {
        User friend = friendship.getUserA().getId().equals(requesterId) ? friendship.getUserB() : friendship.getUserA();
        boolean online = socialEventPublisher.isOnline(friend.getId());
        return FriendSummaryResponse.from(friend, online, friendship.getCreatedAt());
    }

    private void ensureNotBlocked(User actor, User target) {
        boolean blocked = blockRepository.existsByBlocker_IdAndBlocked_Id(actor.getId(), target.getId())
                || blockRepository.existsByBlocker_IdAndBlocked_Id(target.getId(), actor.getId());
        if (blocked) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차단 관계에서는 해당 작업을 수행할 수 없습니다.");
        }
    }

    private boolean areFriends(User a, User b) {
        Long first = Math.min(a.getId(), b.getId());
        Long second = Math.max(a.getId(), b.getId());
        return friendshipRepository.findByUserA_IdAndUserB_Id(first, second).isPresent();
    }

    private void addFriendship(User a, User b) {
        if (!areFriends(a, b)) {
            friendshipRepository.save(new Friendship(a, b));
        }
    }

    private void removeFriendshipIfExists(User a, User b) {
        Long first = Math.min(a.getId(), b.getId());
        Long second = Math.max(a.getId(), b.getId());
        friendshipRepository.findByUserA_IdAndUserB_Id(first, second)
                .ifPresent(friendshipRepository::delete);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
