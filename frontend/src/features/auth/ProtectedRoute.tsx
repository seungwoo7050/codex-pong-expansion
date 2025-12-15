import { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthProvider'

/**
 * [가드] frontend/src/features/auth/ProtectedRoute.tsx
 * 설명:
 *   - 인증되지 않은 사용자를 로그인 페이지로 리다이렉트하여 보호된 화면을 감싼다.
 *   - 로딩 중에는 단순한 안내 메시지를 보여준다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/frontend/v0.2.0-auth-and-profile-ui.md
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <p className="page">인증 정보를 불러오는 중입니다...</p>
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
