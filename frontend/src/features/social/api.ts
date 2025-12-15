import { apiFetch } from '../../shared/api/client'
import { BlockedUser, FriendRequestList, FriendSummary, GameInvite } from '../../shared/types/social'

/**
 * [API 모듈] frontend/src/features/social/api.ts
 * 설명:
 *   - 친구 목록, 요청, 차단, 초대 관련 REST 호출을 캡슐화한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/frontend/v0.5.0-friends-and-invites-ui.md
 */
export function fetchFriends(token: string) {
  return apiFetch<FriendSummary[]>('/api/social/friends', { method: 'GET' }, token)
}

export function fetchFriendRequests(token: string) {
  return apiFetch<FriendRequestList>('/api/social/friend-requests', { method: 'GET' }, token)
}

export function sendFriendRequest(token: string, targetUsername: string) {
  return apiFetch('/api/social/friend-requests', {
    method: 'POST',
    body: JSON.stringify({ targetUsername }),
  }, token)
}

export function acceptFriendRequest(token: string, requestId: number) {
  return apiFetch('/api/social/friend-requests/' + requestId + '/accept', { method: 'POST' }, token)
}

export function rejectFriendRequest(token: string, requestId: number) {
  return apiFetch('/api/social/friend-requests/' + requestId + '/reject', { method: 'POST' }, token)
}

export function fetchBlocks(token: string) {
  return apiFetch<BlockedUser[]>('/api/social/blocks', { method: 'GET' }, token)
}

export function blockUser(token: string, targetUsername: string) {
  return apiFetch<BlockedUser>('/api/social/blocks', {
    method: 'POST',
    body: JSON.stringify({ targetUsername }),
  }, token)
}

export function unblockUser(token: string, userId: number) {
  return apiFetch('/api/social/blocks/' + userId, { method: 'DELETE' }, token)
}

export function sendInvite(token: string, targetUserId: number) {
  return apiFetch<GameInvite>('/api/social/invites', {
    method: 'POST',
    body: JSON.stringify({ targetUserId }),
  }, token)
}

export function fetchIncomingInvites(token: string) {
  return apiFetch<GameInvite[]>('/api/social/invites/incoming', { method: 'GET' }, token)
}

export function fetchOutgoingInvites(token: string) {
  return apiFetch<GameInvite[]>('/api/social/invites/outgoing', { method: 'GET' }, token)
}

export function acceptInvite(token: string, inviteId: number) {
  return apiFetch<GameInvite>('/api/social/invites/' + inviteId + '/accept', { method: 'POST' }, token)
}

export function rejectInvite(token: string, inviteId: number) {
  return apiFetch<GameInvite>('/api/social/invites/' + inviteId + '/reject', { method: 'POST' }, token)
}
