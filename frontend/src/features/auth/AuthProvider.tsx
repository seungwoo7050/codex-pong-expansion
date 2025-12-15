import { PropsWithChildren, createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { QueryClient, QueryClientProvider, useQueryClient } from '@tanstack/react-query'
import { OAuthProvider, fetchProfile, login as loginApi, loginWithProvider, logout as logoutApi, register as registerApi } from './api'
import { UserProfile } from '../../shared/types/user'

/**
 * [컨텍스트] frontend/src/features/auth/AuthProvider.tsx
 * 설명:
 *   - JWT 기반 인증 상태를 전역으로 관리하고 로그인 유지/로그아웃/프로필 갱신 기능을 제공한다.
 *   - 로컬 스토리지와 React Query를 이용해 새로고침 후에도 인증 정보를 복원한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/frontend/v0.4.0-ranking-and-leaderboard-ui.md
 */
interface AuthContextValue {
  status: 'loading' | 'authenticated' | 'guest'
  token: string | null
  user: UserProfile | null
  login: (username: string, password: string) => Promise<void>
  loginWithOAuth: (provider: OAuthProvider, accessToken: string) => Promise<void>
  register: (params: { username: string; password: string; nickname: string; avatarUrl?: string }) => Promise<void>
  logout: () => Promise<void>
  refreshProfile: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

function AuthProviderInternal({ children }: PropsWithChildren) {
  const queryClient = useQueryClient()
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('authToken'))
  const [user, setUser] = useState<UserProfile | null>(null)
  const [status, setStatus] = useState<'loading' | 'authenticated' | 'guest'>(() => (token ? 'loading' : 'guest'))

  const persistToken = useCallback((nextToken: string | null) => {
    setToken(nextToken)
    if (nextToken) {
      localStorage.setItem('authToken', nextToken)
    } else {
      localStorage.removeItem('authToken')
    }
  }, [])

  const handleAuthSuccess = useCallback(
    (nextToken: string, nextUser: UserProfile) => {
      persistToken(nextToken)
      setUser(nextUser)
      setStatus('authenticated')
    },
    [persistToken],
  )

  const refreshProfile = useCallback(async () => {
    if (!token) {
      setUser(null)
      setStatus('guest')
      return
    }
    setStatus('loading')
    try {
      const profile = await fetchProfile(token)
      setUser(profile)
      setStatus('authenticated')
    } catch (error) {
      persistToken(null)
      setUser(null)
      setStatus('guest')
    }
  }, [persistToken, token])

  useEffect(() => {
    if (token) {
      refreshProfile()
    }
  }, [token, refreshProfile])

  const login = useCallback(async (username: string, password: string) => {
    const response = await loginApi({ username, password })
    handleAuthSuccess(response.token, response.user)
  }, [handleAuthSuccess])

  const loginWithOAuth = useCallback(async (provider: OAuthProvider, accessToken: string) => {
    const response = await loginWithProvider(provider, accessToken)
    handleAuthSuccess(response.token, response.user)
  }, [handleAuthSuccess])

  const register = useCallback(
    async ({ username, password, nickname, avatarUrl }: { username: string; password: string; nickname: string; avatarUrl?: string }) => {
      const response = await registerApi({ username, password, nickname, avatarUrl })
      handleAuthSuccess(response.token, response.user)
    },
    [handleAuthSuccess],
  )

  const logout = useCallback(async () => {
    if (token) {
      try {
        await logoutApi(token)
      } catch (error) {
        // 로그아웃 실패는 클라이언트 상태 초기화로 대체
      }
    }
    persistToken(null)
    setUser(null)
    setStatus('guest')
    queryClient.clear()
  }, [persistToken, queryClient, token])

  const value = useMemo<AuthContextValue>(() => ({
    status,
    token,
    user,
    login,
    register,
    loginWithOAuth,
    logout,
    refreshProfile,
  }), [login, loginWithOAuth, logout, register, refreshProfile, status, token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

const queryClient = new QueryClient()

export function AuthProvider({ children }: PropsWithChildren) {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProviderInternal>{children}</AuthProviderInternal>
    </QueryClientProvider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('AuthProvider가 설정되지 않았습니다.')
  }
  return ctx
}
