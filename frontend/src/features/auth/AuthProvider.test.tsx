import { describe, expect, it, beforeEach, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import React, { useEffect } from 'react'
import { AuthProvider, useAuth } from './AuthProvider'
import { ProtectedRoute } from './ProtectedRoute'

function wrapper(ui: React.ReactElement) {
  return render(ui, { wrapper: ({ children }) => <AuthProvider>{children}</AuthProvider> })
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('로그인 성공 시 사용자 상태를 설정한다', async () => {
    const profile = {
      id: 1,
      username: 'tester',
      nickname: '테스터',
      avatarUrl: null,
      rating: 1200,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }

    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            token: 'test-token',
            expiresAt: new Date().toISOString(),
            user: profile,
          }),
          { status: 200 },
        ) as Response,
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(profile), { status: 200 }) as Response,
      )

    function Tester() {
      const { login, user, status } = useAuth()
      useEffect(() => {
        login('tester', 'password')
      }, [login])
      return (
        <div>
          <span data-testid="status">{status}</span>
          <span data-testid="nickname">{user?.nickname ?? ''}</span>
        </div>
      )
    }

    wrapper(
      <MemoryRouter>
        <Tester />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('nickname').textContent).toBe('테스터')
      expect(screen.getByTestId('status').textContent).toBe('authenticated')
    })
  })

  it('인증되지 않은 사용자는 보호된 라우트에서 로그인 페이지로 이동한다', async () => {
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/secret']}>
          <Routes>
            <Route
              path="/secret"
              element={(
                <ProtectedRoute>
                  <div>비밀</div>
                </ProtectedRoute>
              )}
            />
            <Route path="/login" element={<div>로그인 화면</div>} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>,
    )

    await waitFor(() => {
      expect(screen.getByText('로그인 화면')).toBeInTheDocument()
    })
  })
})
