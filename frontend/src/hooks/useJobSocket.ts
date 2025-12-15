import { useEffect, useRef, useState } from 'react'
import { JobSocketEvent, startJobSocket } from '../shared/realtime/jobSocketClient'

/**
 * [훅] frontend/src/hooks/useJobSocket.ts
 * 설명:
 *   - v0.14.0 기준 Promise 기반 래퍼를 이용해 잡 진행률/완료 WebSocket 이벤트를 수신한다.
 *   - 재연결 횟수 제한과 상태 이벤트를 함께 반환해 UI에서 품질을 측정할 수 있도록 한다.
 * 버전: v0.14.0
 * 관련 설계문서:
 *   - design/realtime/v0.12.0-job-progress-events.md
 *   - design/realtime/v0.14.0-async-ws-client-patterns.md
 */
export function useJobSocket(token?: string | null, onEvent?: (event: JobSocketEvent) => void) {
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState('')
  const handlerRef = useRef<typeof onEvent>()

  useEffect(() => {
    handlerRef.current = onEvent
  }, [onEvent])

  useEffect(() => {
    if (!token || typeof WebSocket === 'undefined') return () => undefined

    const client = startJobSocket(
      token,
      (event) => {
        if (handlerRef.current) {
          handlerRef.current(event)
        }
      },
      (state, attempt) => {
        if (state === 'connected') {
          setConnected(true)
          setError('')
        } else if (state === 'reconnecting' || state === 'connecting') {
          setConnected(false)
          if (attempt > 0) {
            setError(`잡 알림 재연결 시도 중 (${attempt}회)입니다.`)
          }
        } else if (state === 'disconnected') {
          setConnected(false)
          setError('잡 진행 알림 연결이 끊어졌습니다. 새로고침 후 다시 시도하세요.')
        }
      },
      { maxRetries: 3, retryDelayMs: 900 },
    )

    return () => {
      client.close()
    }
  }, [token])

  return { connected, error }
}
