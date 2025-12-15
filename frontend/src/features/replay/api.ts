import { API_BASE_URL } from '../../constants'
import { apiFetch } from '../../shared/api/client'
import { ReplayDetail, ReplayEventRecord, ReplayPage } from '../../shared/types/replay'

/**
 * [API 모듈] frontend/src/features/replay/api.ts
 * 설명:
 *   - v0.11.0 리플레이 목록/상세 조회와 이벤트 파일 다운로드를 위한 클라이언트 유틸이다.
 *   - 백엔드가 제공하는 JSONL_V1 포맷을 그대로 파싱해 재생 데이터로 변환한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/frontend/v0.11.0-replay-browser-and-viewer.md
 */
export function fetchReplays(token: string, page = 0, size = 20) {
  return apiFetch<ReplayPage>(`/api/replays?page=${page}&size=${size}`, { method: 'GET' }, token)
}

export function fetchReplayDetail(replayId: number, token: string) {
  return apiFetch<ReplayDetail>(`/api/replays/${replayId}`, { method: 'GET' }, token)
}

export async function fetchReplayEvents(downloadPath: string, token: string) {
  const response = await fetch(`${API_BASE_URL}${downloadPath}`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })
  if (!response.ok) {
    throw new Error('리플레이 이벤트를 불러오지 못했습니다.')
  }
  const text = await response.text()
  return text
    .split('\n')
    .filter((line) => line.trim().length > 0)
    .map((line) => JSON.parse(line) as ReplayEventRecord)
}
