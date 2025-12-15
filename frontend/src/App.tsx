import { Link, Route, Routes } from 'react-router-dom'
import { LandingPage } from './pages/LandingPage'
import { LobbyPage } from './pages/LobbyPage'
import { GamePage } from './pages/GamePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { ProfilePage } from './pages/ProfilePage'
import { LeaderboardPage } from './pages/LeaderboardPage'
import { FriendsPage } from './pages/FriendsPage'
import { TournamentPage } from './pages/TournamentPage'
import { SpectatorPage } from './pages/SpectatorPage'
import { AdminPage } from './pages/AdminPage'
import { MyReplaysPage } from './pages/MyReplaysPage'
import { ReplayViewerPage } from './pages/ReplayViewerPage'
import { JobsPage } from './pages/JobsPage'
import { useAuth } from './features/auth/AuthProvider'
import { ProtectedRoute } from './features/auth/ProtectedRoute'

/**
 * [루트] frontend/src/App.tsx
 * 설명:
 *   - 기본 네비게이션과 페이지 라우팅을 설정한다.
 *   - v0.8.0에서는 관전 전용 경로를 추가해 실시간 관전 목록으로 이동한다.
 *   - v0.11.0에서는 리플레이 목록/뷰어 경로를 추가한다.
 *   - v0.12.0에서는 리플레이 내보내기 잡 대시보드를 위한 경로를 추가한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 *   - design/frontend/v0.11.0-replay-browser-and-viewer.md
 *   - design/frontend/v0.12.0-replay-export-and-jobs-ui.md
 * 변경 이력:
 *   - v0.1.0: React Router 기반 기본 라우팅 추가
 *   - v0.2.0: 인증 라우팅 및 네비게이션 확장
 *   - v0.3.0: 게임 전용 보호 라우트 추가
 *   - v0.4.0: 리더보드 라우트와 랭크 네비게이션 추가
 *   - v0.5.0: 친구 관리 라우트와 네비게이션 추가
 *   - v0.8.0: 관전 경로와 네비게이션 추가
 *   - v0.11.0: 리플레이 경로 추가
 */
function App() {
  const { user, status, logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="header">
        <Link to="/" className="brand">
          Codex Pong
        </Link>
        <nav className="nav">
          <Link to="/lobby">로비</Link>
          <Link to="/game">게임</Link>
          <Link to="/leaderboard">리더보드</Link>
          <Link to="/tournaments">토너먼트</Link>
          <Link to="/spectate">관전</Link>
          {status === 'authenticated' && (
            <>
              <Link to="/replays">리플레이</Link>
              <Link to="/jobs">작업</Link>
            </>
          )}
          {status === 'authenticated' ? (
            <>
              <Link to="/admin">운영</Link>
              <Link to="/friends">친구</Link>
              <Link to="/profile">내 프로필</Link>
              <button className="link-button" type="button" onClick={logout}>
                로그아웃
              </button>
              <span className="nickname">{user?.nickname}</span>
            </>
          ) : (
            <>
              <Link to="/login">로그인</Link>
              <Link to="/register">회원가입</Link>
            </>
          )}
        </nav>
      </header>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/lobby"
          element={(
            <ProtectedRoute>
              <LobbyPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/game"
          element={(
            <ProtectedRoute>
              <GamePage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/profile"
          element={(
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/leaderboard"
          element={(
            <ProtectedRoute>
              <LeaderboardPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/tournaments"
          element={(
            <ProtectedRoute>
              <TournamentPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/friends"
          element={(
            <ProtectedRoute>
              <FriendsPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/spectate"
          element={(
            <ProtectedRoute>
              <SpectatorPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/replays"
          element={(
            <ProtectedRoute>
              <MyReplaysPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/replays/:replayId"
          element={(
            <ProtectedRoute>
              <ReplayViewerPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/jobs"
          element={(
            <ProtectedRoute>
              <JobsPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/admin"
          element={(
            <ProtectedRoute>
              <AdminPage />
            </ProtectedRoute>
          )}
        />
      </Routes>
    </div>
  )
}

export default App
