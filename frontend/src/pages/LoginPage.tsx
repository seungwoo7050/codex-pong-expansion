import { FormEvent, useState } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { OAuthProvider } from '../features/auth/api'

/**
 * [페이지] frontend/src/pages/LoginPage.tsx
 * 설명:
 *   - 아이디/비밀번호를 입력받아 로그인하고 보호된 라우트로 복귀한다.
 *   - 실패 시 한국어 메시지를 표시하며, 회원가입 링크를 제공한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/frontend/v0.10.0-kor-ux.md
 */
export function LoginPage() {
  const { login, loginWithOAuth, status } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const redirectTo = (location.state as { from?: string })?.from ?? '/lobby'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [providerToken, setProviderToken] = useState('')
  const [provider, setProvider] = useState<OAuthProvider>('kakao')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(username, password)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError('로그인에 실패했습니다. 아이디와 비밀번호를 확인하세요.')
    } finally {
      setLoading(false)
    }
  }

  const handleOAuthLogin = async () => {
    setError(null)
    if (!providerToken) {
      setError('발급받은 소셜 액세스 토큰을 입력하세요.')
      return
    }
    setLoading(true)
    try {
      await loginWithOAuth(provider, providerToken)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError('소셜 로그인을 처리하지 못했습니다. 토큰과 제공자를 다시 확인하세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="page">
      <section className="panel">
        <h2>로그인</h2>
        <p>이메일/아이디 로그인과 카카오·네이버 연동을 모두 지원합니다. 모든 시간은 KST 기준으로 표시됩니다.</p>
        <form className="form" onSubmit={handleSubmit}>
          <label>
            아이디
            <input value={username} onChange={(e) => setUsername(e.target.value)} required minLength={4} maxLength={60} />
          </label>
          <label>
            비밀번호
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              maxLength={72}
            />
          </label>
          {error && <p className="error">{error}</p>}
          <button className="button" type="submit" disabled={loading || status === 'loading'}>
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>
        <div className="panel" style={{ marginTop: '12px' }}>
          <h3>소셜 로그인 (카카오/네이버)</h3>
          <p>리다이렉트로 받은 액세스 토큰을 입력한 뒤 제공자를 선택하세요.</p>
          <div className="form">
            <label>
              제공자 선택
              <select value={provider} onChange={(e) => setProvider(e.target.value as OAuthProvider)}>
                <option value="kakao">카카오</option>
                <option value="naver">네이버</option>
              </select>
            </label>
            <label>
              액세스 토큰
              <input
                value={providerToken}
                onChange={(e) => setProviderToken(e.target.value)}
                placeholder="인가 코드 교환 후 받은 토큰을 입력"
              />
            </label>
            <button className="button" type="button" onClick={handleOAuthLogin} disabled={loading}>
              {loading ? '연동 중...' : `${provider === 'kakao' ? '카카오' : '네이버'}로 로그인`}
            </button>
          </div>
        </div>
        <p>
          아직 계정이 없다면 <Link to="/register">회원가입</Link>
        </p>
      </section>
    </main>
  )
}
