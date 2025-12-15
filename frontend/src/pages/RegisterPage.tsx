import { FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'

/**
 * [페이지] frontend/src/pages/RegisterPage.tsx
 * 설명:
 *   - 신규 계정을 생성하고 즉시 로그인 상태로 전환한다.
 *   - 기본 프로필 필드(닉네임, 아바타)를 입력받는다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/frontend/v0.2.0-auth-and-profile-ui.md
 */
export function RegisterPage() {
  const { register, status } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '', nickname: '', avatarUrl: '' })
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleChange = (key: string, value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await register(form)
      navigate('/profile', { replace: true })
    } catch (err) {
      setError('회원가입에 실패했습니다. 입력값을 확인해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <section className="panel">
        <h2>회원가입</h2>
        <p>아이디와 비밀번호, 닉네임을 입력하면 바로 로그인됩니다.</p>
        <form className="form" onSubmit={handleSubmit}>
          <label>
            아이디
            <input
              value={form.username}
              onChange={(e) => handleChange('username', e.target.value)}
              required
              minLength={4}
              maxLength={60}
            />
          </label>
          <label>
            비밀번호
            <input
              type="password"
              value={form.password}
              onChange={(e) => handleChange('password', e.target.value)}
              required
              minLength={8}
              maxLength={72}
            />
          </label>
          <label>
            닉네임
            <input
              value={form.nickname}
              onChange={(e) => handleChange('nickname', e.target.value)}
              required
              minLength={2}
              maxLength={60}
            />
          </label>
          <label>
            아바타 URL (선택)
            <input
              value={form.avatarUrl}
              onChange={(e) => handleChange('avatarUrl', e.target.value)}
              maxLength={255}
              placeholder="https://example.com/avatar.png"
            />
          </label>
          {error && <p className="error">{error}</p>}
          <button className="button" type="submit" disabled={loading || status === 'loading'}>
            {loading ? '가입 중...' : '가입하기'}
          </button>
        </form>
        <p>
          이미 계정이 있다면 <Link to="/login">로그인</Link>
        </p>
      </section>
    </main>
  )
}
