import { Link } from 'react-router-dom'

/**
 * [페이지] frontend/src/pages/LandingPage.tsx
 * 설명:
 *   - 서비스 소개와 빠른 이동 링크를 제공하는 랜딩 화면이다.
 *   - v0.2.0에서는 계정/프로필 플로우 진입 링크를 함께 노출한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/frontend/v0.2.0-auth-and-profile-ui.md
 * 변경 이력:
 *   - v0.1.0: 기본 랜딩 섹션 추가
 *   - v0.2.0: 인증 경로 안내 링크 추가
 */
export function LandingPage() {
  return (
    <main className="page">
      <section className="hero">
        <h1>Codex Pong</h1>
        <p>v0.2.0에서는 계정/로그인/프로필 편집이 가능하며, 이후 버전의 게임 흐름을 준비합니다.</p>
        <div className="actions">
          <Link className="button" to="/lobby">
            로비로 이동
          </Link>
          <Link className="secondary" to="/game">
            게임 화면 보기
          </Link>
          <Link className="secondary" to="/profile">
            내 프로필
          </Link>
        </div>
      </section>
    </main>
  )
}
