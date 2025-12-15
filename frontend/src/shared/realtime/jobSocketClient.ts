import { WS_BASE_URL } from '../../constants'

export interface JobSocketEvent {
  type: string
  payload: unknown
}

export type JobSocketState = 'connecting' | 'connected' | 'reconnecting' | 'disconnected'

export interface JobSocketClient {
  close: () => void
}

/**
 * [클라이언트] frontend/src/shared/realtime/jobSocketClient.ts
 * 설명:
 *   - v0.14.0 잡 진행률 WebSocket을 Promise 기반으로 연결하고, 제한된 재시도 정책을 적용한다.
 *   - onStateChange 훅을 통해 연결/재시도/종료 이벤트를 UI에서 일관되게 표시할 수 있다.
 * 버전: v0.14.0
 * 관련 설계문서:
 *   - design/realtime/v0.14.0-async-ws-client-patterns.md
 */
export function startJobSocket(
  token: string,
  onEvent: (event: JobSocketEvent) => void,
  onStateChange?: (state: JobSocketState, attempt: number) => void,
  options?: { maxRetries?: number; retryDelayMs?: number },
): JobSocketClient {
  const maxRetries = options?.maxRetries ?? 3
  const retryDelayMs = options?.retryDelayMs ?? 800
  let closed = false
  let socket: WebSocket | null = null
  let attempts = 0

  const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

  const listenOnce = () =>
    new Promise<void>((resolve, reject) => {
      const ws = new WebSocket(`${WS_BASE_URL}/ws/jobs?token=${encodeURIComponent(token)}`)
      socket = ws
      onStateChange?.('connecting', attempts)

      let opened = false

      ws.onopen = () => {
        opened = true
        attempts = 0
        onStateChange?.('connected', attempts)
      }

      ws.onmessage = (messageEvent) => {
        try {
          const parsed = JSON.parse(messageEvent.data) as JobSocketEvent
          onEvent(parsed)
        } catch (error) {
          console.warn('잡 소켓 메시지 파싱 오류', error)
        }
      }

      ws.onerror = () => {
        if (!opened) {
          reject(new Error('잡 소켓 연결 실패'))
        }
      }

      ws.onclose = () => {
        if (closed) {
          resolve()
        } else {
          reject(new Error('잡 소켓 연결 종료'))
        }
      }
    })

  const connectLoop = async () => {
    while (!closed) {
      try {
        await listenOnce()
        if (!closed) {
          break
        }
      } catch (err) {
        attempts += 1
        if (closed) {
          break
        }
        if (attempts > maxRetries) {
          onStateChange?.('disconnected', attempts)
          break
        }
        onStateChange?.('reconnecting', attempts)
        await wait(retryDelayMs * attempts)
      }
    }
  }

  connectLoop()

  return {
    close: () => {
      closed = true
      socket?.close()
    },
  }
}
