import { useEffect, useState } from 'react'
import { applyModeration, fetchAdminMatches, fetchAdminStats, fetchAdminUserMatches, fetchAdminUsers } from '../shared/api/admin'
import { AdminStats, AdminUser, ModerationAction } from '../shared/types/admin'
import { MatchResult } from '../shared/types/match'
import { useAuth } from '../features/auth/AuthProvider'

/**
 * [페이지] frontend/src/pages/AdminPage.tsx
 * 설명:
 *   - 관리자 권한으로 사용자 목록, 전적, 제재 도구, 기본 상태 지표를 제공한다.
 *   - v0.9.0에서 운영 최소 기능을 갖춘 콘솔 화면을 구축한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/frontend/v0.9.0-admin-console.md
 */
export function AdminPage() {
  const { token } = useAuth()
  const [users, setUsers] = useState<AdminUser[]>([])
  const [matches, setMatches] = useState<MatchResult[]>([])
  const [userMatches, setUserMatches] = useState<MatchResult[]>([])
  const [stats, setStats] = useState<AdminStats | null>(null)
  const [reason, setReason] = useState('운영자 제재')
  const [duration, setDuration] = useState(60)
  const [action, setAction] = useState<ModerationAction>('SUSPEND')
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) return
    fetchAdminUsers(token).then(setUsers).catch(() => setError('사용자 목록을 불러오지 못했습니다.'))
    fetchAdminMatches(token).then(setMatches).catch(() => setError('최근 경기 목록을 불러오지 못했습니다.'))
    fetchAdminStats(token).then(setStats).catch(() => setError('상태 정보를 가져오지 못했습니다.'))
  }, [token])

  const loadUserMatches = (userId: number) => {
    if (!token) return
    setSelectedUserId(userId)
    fetchAdminUserMatches(userId, token)
      .then(setUserMatches)
      .catch(() => setError('사용자 전적을 불러오지 못했습니다.'))
  }

  const handleModeration = async (userId: number) => {
    if (!token) return
    try {
      const updated = await applyModeration(userId, action, reason || '운영자 제재', duration, token)
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)))
      if (selectedUserId === userId) {
        loadUserMatches(userId)
      }
      setError('')
    } catch (e) {
      setError('제재 적용에 실패했습니다.')
    }
  }

  const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' }) : '-')

  return (
    <main className="page">
      <h1>운영자 콘솔</h1>
      <section className="panel">
        <h2>기본 상태</h2>
        {stats ? (
          <ul className="list compact">
            <li>등록 사용자: {stats.userCount}명</li>
            <li>누적 경기 수: {stats.totalMatches}건</li>
            <li>활성 경기 방: {stats.activeGames}개</li>
            <li>관전자 세션: {stats.activeSpectators}개</li>
          </ul>
        ) : (
          <p>상태 정보를 불러오는 중입니다...</p>
        )}
      </section>

      <section className="panel">
        <h2>사용자 제재</h2>
        <div className="form-grid">
          <label>
            제재 유형
            <select value={action} onChange={(e) => setAction(e.target.value as ModerationAction)}>
              <option value="BAN">밴</option>
              <option value="SUSPEND">정지</option>
              <option value="MUTE">뮤트</option>
            </select>
          </label>
          <label>
            사유
            <input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="제재 사유" />
          </label>
          <label>
            기간(분)
            <input
              type="number"
              value={duration}
              onChange={(e) => setDuration(Number(e.target.value))}
              min={0}
            />
          </label>
        </div>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>닉네임</th>
                <th>상태</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.username}</td>
                  <td>{user.nickname}</td>
                  <td>
                    {user.banned ? '밴' : '정상'}
                    {user.suspendedUntil && !user.banned && ` · 정지 ${formatDate(user.suspendedUntil)}`}
                    {user.mutedUntil && ` · 뮤트 ${formatDate(user.mutedUntil)}`}
                    <div className="badge" style={{ marginTop: '4px' }}>
                      로그인: {user.authProvider ? user.authProvider : '로컬'} / 로케일 {user.locale ?? '미지정'}
                    </div>
                  </td>
                  <td>
                    <button className="button" type="button" onClick={() => handleModeration(user.id)}>
                      적용
                    </button>
                    <button className="link-button" type="button" onClick={() => loadUserMatches(user.id)}>
                      전적 확인
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2>최근 경기</h2>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>방 번호</th>
                <th>참가자</th>
                <th>점수</th>
                <th>종료 시각</th>
              </tr>
            </thead>
            <tbody>
              {matches.map((match) => (
                <tr key={match.id}>
                  <td>{match.roomId}</td>
                  <td>
                    {match.playerANickname} vs {match.playerBNickname}
                  </td>
                  <td>
                    {match.scoreA} : {match.scoreB} ({match.matchType})
                  </td>
                  <td>{formatDate(match.finishedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2>선택한 사용자 전적</h2>
        {selectedUserId ? <p>대상 ID: {selectedUserId}</p> : <p>사용자를 선택해 전적을 확인하세요.</p>}
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>방 번호</th>
                <th>점수</th>
                <th>종료 시각</th>
              </tr>
            </thead>
            <tbody>
              {userMatches.map((match) => (
                <tr key={match.id}>
                  <td>{match.roomId}</td>
                  <td>
                    {match.playerANickname} {match.scoreA} : {match.scoreB} {match.playerBNickname}
                  </td>
                  <td>{formatDate(match.finishedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {error && <p className="error">{error}</p>}
    </main>
  )
}
