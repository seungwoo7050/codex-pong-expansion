/**
 * [타입] frontend/src/shared/types/social.ts
 * 설명:
 *   - 친구/차단/초대 관련 API 응답을 표현하는 공용 타입 정의다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/frontend/v0.5.0-friends-and-invites-ui.md
 */
export interface FriendSummary {
  userId: number
  nickname: string
  avatarUrl?: string | null
  online: boolean
  since: string
}

export interface FriendRequestItem {
  id: number
  senderId: number
  senderNickname: string
  receiverId: number
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  createdAt: string
  respondedAt?: string
}

export interface FriendRequestList {
  incoming: FriendRequestItem[]
  outgoing: FriendRequestItem[]
}

export interface BlockedUser {
  userId: number
  nickname: string
  blockedAt: string
}

export interface GameInvite {
  id: number
  senderId: number
  senderNickname: string
  receiverId: number
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  matchType: 'NORMAL' | 'RANKED'
  roomId?: string | null
  createdAt: string
  respondedAt?: string | null
}
