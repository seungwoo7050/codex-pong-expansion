import { FormEvent, useMemo, useState } from 'react'
import { useAuth } from '../features/auth/AuthProvider'
import { updateProfile } from '../features/auth/api'
import { toTierLabel } from '../shared/utils/rating'

/**
 * [페이지] frontend/src/pages/ProfilePage.tsx
 * 설명:
 *   - 현재 로그인한 사용자의 기본 프로필을 보여주고 닉네임/아바타를 수정한다.
 *   - 저장 후 전역 인증 상태도 갱신한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/frontend/v0.4.0-ranking-and-leaderboard-ui.md
 */
export function ProfilePage() {
  const { user, token, refreshProfile } = useAuth()
  const [nickname, setNickname] = useState(user?.nickname ?? '')
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl ?? '')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const createdAt = useMemo(
    () => (user ? new Date(user.createdAt).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' }) : ''),
    [user],
  )
  const updatedAt = useMemo(
    () => (user ? new Date(user.updatedAt).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' }) : ''),
    [user],
  )

  if (!user || !token) {
    return <main className="page">프로필을 불러오는 중 문제가 발생했습니다.</main>
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    setMessage('')
    try {
      await updateProfile(token, { nickname, avatarUrl })
      await refreshProfile()
      setMessage('프로필이 저장되었습니다.')
    } catch (err) {
      setError('프로필 저장에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <section className="panel">
        <h2>내 프로필</h2>
        <p>아이디: {user.username}</p>
        <p>생성 시각: {createdAt}</p>
        <p>최근 수정: {updatedAt}</p>
        <p>현재 레이팅: {user.rating}점 ({toTierLabel(user.rating)})</p>
        <form className="form" onSubmit={handleSubmit}>
          <label>
            닉네임
            <input value={nickname} onChange={(e) => setNickname(e.target.value)} required minLength={2} maxLength={60} />
          </label>
          <label>
            아바타 URL
            <input
              value={avatarUrl ?? ''}
              onChange={(e) => setAvatarUrl(e.target.value)}
              maxLength={255}
              placeholder="https://example.com/avatar.png"
            />
          </label>
          {message && <p className="success">{message}</p>}
          {error && <p className="error">{error}</p>}
          <button className="button" type="submit" disabled={loading}>
            {loading ? '저장 중...' : '프로필 저장'}
          </button>
        </form>
      </section>
    </main>
  )
}
