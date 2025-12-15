package com.codexpong.backend.social.controller;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.social.dto.BlockCreateRequest;
import com.codexpong.backend.social.dto.BlockedUserResponse;
import com.codexpong.backend.social.dto.FriendRequestCreateRequest;
import com.codexpong.backend.social.dto.FriendRequestListResponse;
import com.codexpong.backend.social.dto.FriendRequestResponse;
import com.codexpong.backend.social.dto.FriendSummaryResponse;
import com.codexpong.backend.social.dto.GameInviteResponse;
import com.codexpong.backend.social.dto.InviteSendRequest;
import com.codexpong.backend.social.service.SocialService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/social/controller/SocialController.java
 * 설명:
 *   - 친구 목록, 친구 요청, 차단, 게임 초대 관련 REST API를 제공한다.
 *   - 모든 엔드포인트는 인증된 사용자만 접근하며 v0.5.0 소셜 기능을 묶어 제공한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    /**
     * 설명:
     *   - 친구 목록을 반환한다. 온라인 여부는 WebSocket 구독 정보를 기반으로 계산한다.
     */
    @GetMapping("/friends")
    public List<FriendSummaryResponse> friends(@AuthenticationPrincipal AuthenticatedUser user) {
        return socialService.listFriends(user.id());
    }

    /**
     * 설명:
     *   - 보낸/받은 친구 요청 목록을 반환한다.
     */
    @GetMapping("/friend-requests")
    public FriendRequestListResponse requestList(@AuthenticationPrincipal AuthenticatedUser user) {
        return socialService.listRequests(user.id());
    }

    /**
     * 설명:
     *   - 대상 사용자명으로 친구 요청을 생성한다.
     */
    @PostMapping("/friend-requests")
    public FriendRequestResponse requestFriend(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody FriendRequestCreateRequest request) {
        return socialService.requestFriend(user.id(), request.getTargetUsername());
    }

    /**
     * 설명:
     *   - 수신한 친구 요청을 수락한다.
     */
    @PostMapping("/friend-requests/{id}/accept")
    public FriendRequestResponse acceptRequest(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return socialService.acceptRequest(user.id(), id);
    }

    /**
     * 설명:
     *   - 수신한 친구 요청을 거절한다.
     */
    @PostMapping("/friend-requests/{id}/reject")
    public FriendRequestResponse rejectRequest(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return socialService.rejectRequest(user.id(), id);
    }

    /**
     * 설명:
     *   - 현재 차단 목록을 반환한다.
     */
    @GetMapping("/blocks")
    public List<BlockedUserResponse> blocks(@AuthenticationPrincipal AuthenticatedUser user) {
        return socialService.listBlocks(user.id());
    }

    /**
     * 설명:
     *   - 사용자명을 기준으로 차단 목록에 추가한다.
     */
    @PostMapping("/blocks")
    public BlockedUserResponse block(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody BlockCreateRequest request) {
        return socialService.block(user.id(), request.getTargetUsername());
    }

    /**
     * 설명:
     *   - 지정한 사용자를 차단 목록에서 해제한다.
     */
    @DeleteMapping("/blocks/{userId}")
    public void unblock(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long userId) {
        socialService.unblock(user.id(), userId);
    }

    /**
     * 설명:
     *   - 친구에게 일반전 초대를 보낸다.
     */
    @PostMapping("/invites")
    public GameInviteResponse invite(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody InviteSendRequest request) {
        return socialService.sendInvite(user.id(), request.getTargetUserId());
    }

    /**
     * 설명:
     *   - 내가 받은 초대를 모두 조회한다.
     */
    @GetMapping("/invites/incoming")
    public List<GameInviteResponse> incomingInvites(@AuthenticationPrincipal AuthenticatedUser user) {
        return socialService.listIncomingInvites(user.id());
    }

    /**
     * 설명:
     *   - 내가 보낸 대기 중 초대를 조회한다.
     */
    @GetMapping("/invites/outgoing")
    public List<GameInviteResponse> outgoingInvites(@AuthenticationPrincipal AuthenticatedUser user) {
        return socialService.listOutgoingInvites(user.id());
    }

    /**
     * 설명:
     *   - 받은 초대를 수락해 게임 방을 생성한다.
     */
    @PostMapping("/invites/{id}/accept")
    public GameInviteResponse acceptInvite(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return socialService.acceptInvite(user.id(), id);
    }

    /**
     * 설명:
     *   - 받은 초대를 거절한다.
     */
    @PostMapping("/invites/{id}/reject")
    public GameInviteResponse rejectInvite(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return socialService.rejectInvite(user.id(), id);
    }
}
