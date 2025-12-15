import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './features/auth/AuthProvider'
import './index.css'

/**
 * [엔트리] frontend/src/main.tsx
 * 설명:
 *   - React 애플리케이션을 루트 DOM에 마운트하고 라우터를 설정한다.
 * 버전: v0.1.0
 * 관련 설계문서:
 *   - design/frontend/v0.1.0-core-layout-and-routing.md
 * 변경 이력:
 *   - v0.1.0: BrowserRouter 래핑 추가
 */
ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
)
