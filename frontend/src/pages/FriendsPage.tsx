import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../features/auth/AuthProvider'
import { fetchDmHistory, sendDmMessage } from '../features/chat/api'
import {
  acceptFriendRequest,
  acceptInvite,
  blockUser,
  fetchBlocks,
  fetchFriendRequests,
  fetchFriends,
  fetchIncomingInvites,
  fetchOutgoingInvites,
  rejectFriendRequest,
  rejectInvite,
  sendFriendRequest,
  sendInvite,
  unblockUser,
} from '../features/social/api'
import { useChatSocket } from '../hooks/useChatSocket'
import { useLiveMatches } from '../hooks/useLiveMatches'
import { ChatMessage } from '../shared/types/chat'
import { FriendRequestItem, FriendSummary, GameInvite } from '../shared/types/social'

const formatKst = (value: string) => new Date(value).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' })

/**
 * [페이지] frontend/src/pages/FriendsPage.tsx
 * 설명:
 *   - 친구 목록, 친구 요청, 차단, 초대 흐름을 한 화면에서 관리한다.
 *   - v0.5.0 소셜 기능을 UI로 연결하고 초대 수락 시 게임 화면으로 이동한다.
 *   - v0.6.0에서 친구 목록과 연계된 DM 채팅 패널을 제공한다.
 *   - v0.8.0에서는 친구가 진행 중인 방으로 바로 관전 진입할 수 있는 버튼을 추가한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 *   - design/realtime/v0.8.0-spectator-events.md
 */
export function FriendsPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [friendTarget, setFriendTarget] = useState('')
  const [blockTarget, setBlockTarget] = useState('')
  const [statusMessage, setStatusMessage] = useState('')
  const [selectedFriend, setSelectedFriend] = useState<FriendSummary | null>(null)
  const [dmMessages, setDmMessages] = useState<ChatMessage[]>([])
  const [dmInput, setDmInput] = useState('')
  const chatSocket = useChatSocket(token, (msg) => {
    if (msg.channelType === 'DM' && selectedFriend &&
      ((msg.senderId === selectedFriend.userId && msg.recipientId === user?.id) ||
        (msg.senderId === user?.id && msg.recipientId === selectedFriend.userId))) {
      setDmMessages((prev) => [...prev, msg])
    }
  })
  const { liveMatches } = useLiveMatches(token)

  const liveMatchFor = (userId: number) =>
    liveMatches.find((match) => match.leftPlayerId === userId || match.rightPlayerId === userId)

  const friendsQuery = useQuery({
    queryKey: ['friends'],
    queryFn: () => fetchFriends(token ?? ''),
    enabled: Boolean(token),
  })

  const requestsQuery = useQuery({
    queryKey: ['friendRequests'],
    queryFn: () => fetchFriendRequests(token ?? ''),
    enabled: Boolean(token),
  })

  const blocksQuery = useQuery({
    queryKey: ['blocks'],
    queryFn: () => fetchBlocks(token ?? ''),
    enabled: Boolean(token),
  })

  const incomingInvitesQuery = useQuery({
    queryKey: ['incomingInvites'],
    queryFn: () => fetchIncomingInvites(token ?? ''),
    enabled: Boolean(token),
  })

  const outgoingInvitesQuery = useQuery({
    queryKey: ['outgoingInvites'],
    queryFn: () => fetchOutgoingInvites(token ?? ''),
    enabled: Boolean(token),
  })

  const sendRequestMutation = useMutation({
    mutationFn: () => sendFriendRequest(token ?? '', friendTarget),
    onSuccess: () => {
      setStatusMessage('친구 요청을 보냈습니다.')
      setFriendTarget('')
      queryClient.invalidateQueries({ queryKey: ['friendRequests'] })
    },
    onError: () => setStatusMessage('친구 요청에 실패했습니다. 아이디와 차단 여부를 확인하세요.'),
  })

  const blockMutation = useMutation({
    mutationFn: () => blockUser(token ?? '', blockTarget),
    onSuccess: () => {
      setStatusMessage('차단 목록을 업데이트했습니다.')
      setBlockTarget('')
      queryClient.invalidateQueries({ queryKey: ['blocks'] })
      queryClient.invalidateQueries({ queryKey: ['friends'] })
    },
    onError: () => setStatusMessage('차단 처리에 실패했습니다.'),
  })

  const inviteMutation = useMutation({
    mutationFn: (friendId: number) => sendInvite(token ?? '', friendId),
    onSuccess: () => {
      setStatusMessage('초대를 전송했습니다.')
      queryClient.invalidateQueries({ queryKey: ['outgoingInvites'] })
    },
    onError: () => setStatusMessage('초대를 보내지 못했습니다. 차단 또는 친구 상태를 확인하세요.'),
  })

  const handleAcceptInvite = (inviteId: number) => {
    acceptInvite(token ?? '', inviteId)
      .then((invite) => {
        setStatusMessage('초대를 수락했습니다. 게임 방으로 이동합니다.')
        queryClient.invalidateQueries({ queryKey: ['incomingInvites'] })
        if (invite.roomId) {
          navigate(`/game?roomId=${invite.roomId}`)
        }
      })
      .catch(() => setStatusMessage('초대 수락에 실패했습니다.'))
  }

  const handleRejectInvite = (inviteId: number) => {
    rejectInvite(token ?? '', inviteId)
      .then(() => {
        setStatusMessage('초대를 거절했습니다.')
        queryClient.invalidateQueries({ queryKey: ['incomingInvites'] })
      })
      .catch(() => setStatusMessage('초대 거절에 실패했습니다.'))
  }

  const handleAcceptRequest = (item: FriendRequestItem) => {
    acceptFriendRequest(token ?? '', item.id)
      .then(() => {
        setStatusMessage('친구 요청을 수락했습니다.')
        queryClient.invalidateQueries({ queryKey: ['friendRequests'] })
        queryClient.invalidateQueries({ queryKey: ['friends'] })
      })
      .catch(() => setStatusMessage('요청 수락에 실패했습니다.'))
  }

  const handleRejectRequest = (item: FriendRequestItem) => {
    rejectFriendRequest(token ?? '', item.id)
      .then(() => {
        setStatusMessage('친구 요청을 거절했습니다.')
        queryClient.invalidateQueries({ queryKey: ['friendRequests'] })
      })
      .catch(() => setStatusMessage('요청 거절에 실패했습니다.'))
  }

  const handleUnblock = (userId: number) => {
    unblockUser(token ?? '', userId)
      .then(() => {
        setStatusMessage('차단을 해제했습니다.')
        queryClient.invalidateQueries({ queryKey: ['blocks'] })
      })
      .catch(() => setStatusMessage('차단 해제에 실패했습니다.'))
  }

  const handleSelectFriend = (friend: FriendSummary) => {
    setSelectedFriend(friend)
    setDmInput('')
    if (!token) return
    fetchDmHistory(token, friend.userId)
      .then((res) => setDmMessages(res.messages))
      .catch(() => setStatusMessage('DM 기록을 불러오지 못했습니다.'))
  }

  const handleSendDm = () => {
    if (!selectedFriend || !token || !dmInput.trim()) return
    chatSocket.sendDm(selectedFriend.userId, dmInput.trim())
    sendDmMessage(token, selectedFriend.userId, dmInput.trim()).catch(() => setStatusMessage('DM 전송에 실패했습니다.'))
    setDmInput('')
  }

  const renderFriend = (friend: FriendSummary) => (
    <li
      key={friend.userId}
      className="list-item friend-item"
      onClick={() => handleSelectFriend(friend)}
      role="button"
      tabIndex={0}
    >
      <div>
        <div className="row">
          <span className="nickname">{friend.nickname}</span>
          <span className={friend.online ? 'badge online' : 'badge offline'}>
            {friend.online ? '온라인' : '오프라인'}
          </span>
        </div>
        <small>친구 등록일: {formatKst(friend.since)}</small>
      </div>
      <div className="friend-actions">
        <button className="button" type="button" onClick={() => inviteMutation.mutate(friend.userId)}>
          게임 초대
        </button>
        {liveMatchFor(friend.userId) && (
          <button
            className="secondary"
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              const match = liveMatchFor(friend.userId)
              if (match) {
                navigate(`/spectate?roomId=${match.roomId}`)
              }
            }}
          >
            관전
          </button>
        )}
      </div>
    </li>
  )

  const renderInvite = (invite: GameInvite, incoming: boolean) => (
    <li key={invite.id} className="list-item">
      <div className="row">
        <strong>{incoming ? invite.senderNickname : '대상 ID: ' + invite.receiverId}</strong>
        <span className="badge">{invite.status === 'PENDING' ? '대기' : invite.status}</span>
      </div>
      <small>{formatKst(invite.createdAt)}</small>
      {incoming && invite.status === 'PENDING' && (
        <div className="actions">
          <button className="button" type="button" onClick={() => handleAcceptInvite(invite.id)}>
            수락
          </button>
          <button className="secondary" type="button" onClick={() => handleRejectInvite(invite.id)}>
            거절
          </button>
        </div>
      )}
    </li>
  )

  return (
    <main className="page">
      <section className="panel">
        <h2>친구 추가</h2>
        <p className="hint">상대방의 아이디를 입력해 친구 요청을 보냅니다.</p>
        <div className="form inline-form">
          <input
            value={friendTarget}
            onChange={(e) => setFriendTarget(e.target.value)}
            placeholder="친구로 추가할 아이디"
          />
          <button className="button" type="button" onClick={() => sendRequestMutation.mutate()}>
            친구 요청 보내기
          </button>
        </div>
      </section>

      <section className="panel">
        <h2>친구 목록</h2>
        {friendsQuery.isLoading && <p>불러오는 중...</p>}
        {friendsQuery.data && friendsQuery.data.length === 0 && <p>아직 친구가 없습니다.</p>}
        <ul className="list">{friendsQuery.data?.map(renderFriend)}</ul>
      </section>

      <section className="panel">
        <h2>DM 채팅</h2>
        <p className="hint">친구 목록에서 대상을 선택하면 최근 대화를 불러옵니다. 연결 상태: {chatSocket.connected ? '실시간' : '대기 중'}</p>
        {selectedFriend ? (
          <div className="chat-box">
            <div className="chat-messages">
              {dmMessages.map((msg) => (
                <div key={msg.id} className="chat-line">
                  <strong>{msg.senderNickname}</strong>: {msg.content}
                </div>
              ))}
              {dmMessages.length === 0 && <p className="hint">메시지가 없습니다. 첫 메시지를 보내보세요.</p>}
            </div>
            <div className="chat-input">
              <input
                value={dmInput}
                onChange={(e) => setDmInput(e.target.value)}
                placeholder={`${selectedFriend.nickname}에게 보낼 메시지`}
              />
              <button className="button" type="button" onClick={handleSendDm}>
                전송
              </button>
            </div>
          </div>
        ) : (
          <p className="hint">DM을 보낼 친구를 목록에서 선택하세요.</p>
        )}
      </section>

      <section className="panel grid-two">
        <div>
          <h3>받은 친구 요청</h3>
          <ul className="list">
            {requestsQuery.data?.incoming.map((req) => (
              <li key={req.id} className="list-item">
                <div className="row">
                  <span>{req.senderNickname}</span>
                  <span className="badge">{req.status}</span>
                </div>
                <small>{formatKst(req.createdAt)}</small>
                {req.status === 'PENDING' && (
                  <div className="actions">
                    <button className="button" type="button" onClick={() => handleAcceptRequest(req)}>
                      수락
                    </button>
                    <button className="secondary" type="button" onClick={() => handleRejectRequest(req)}>
                      거절
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </div>
        <div>
          <h3>보낸 친구 요청</h3>
          <ul className="list">
            {requestsQuery.data?.outgoing.map((req) => (
              <li key={req.id} className="list-item">
                <div className="row">
                  <span>대상 ID: {req.receiverId}</span>
                  <span className="badge">{req.status}</span>
                </div>
                <small>{formatKst(req.createdAt)}</small>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="panel grid-two">
        <div>
          <h3>받은 초대</h3>
          <ul className="list">{incomingInvitesQuery.data?.map((invite) => renderInvite(invite, true))}</ul>
        </div>
        <div>
          <h3>보낸 초대</h3>
          <ul className="list">{outgoingInvitesQuery.data?.map((invite) => renderInvite(invite, false))}</ul>
        </div>
      </section>

      <section className="panel">
        <h2>차단 관리</h2>
        <div className="form inline-form">
          <input
            value={blockTarget}
            onChange={(e) => setBlockTarget(e.target.value)}
            placeholder="차단할 아이디"
          />
          <button className="secondary" type="button" onClick={() => blockMutation.mutate()}>
            차단 추가
          </button>
        </div>
        <ul className="list">
          {blocksQuery.data?.map((block) => (
            <li key={block.userId} className="list-item">
              <div className="row">
                <span>{block.nickname}</span>
                <button className="secondary" type="button" onClick={() => handleUnblock(block.userId)}>
                  해제
                </button>
              </div>
              <small>{formatKst(block.blockedAt)}</small>
            </li>
          ))}
        </ul>
      </section>

      {statusMessage && <p className="success">{statusMessage}</p>}
    </main>
  )
}
