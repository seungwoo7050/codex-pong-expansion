"""
[도구] tools/loadtest/ws_connect_storm.py
설명:
  - WebSocket 게이트웨이에 대량 연결/메시지 전송을 시뮬레이션한다.
  - `pip install websockets` 필요.
버전: v1.1.0
관련 설계문서:
  - design/infra/v1.1.0-realtime-topology.md
  - design/realtime/v1.1.0-protocol.md
"""

import asyncio
import os
import random
import string
from typing import List

try:
    import websockets
except ImportError as exc:  # pragma: no cover - 도구 실행 시에만 필요
    raise SystemExit("websockets 라이브러리를 설치하세요: pip install websockets") from exc


async def worker(uri: str, token: str, messages: int) -> None:
    """단일 연결을 생성해 ping 이벤트를 전송한다."""
    session_id = "sess-" + "".join(random.choices(string.ascii_lowercase, k=8))
    connect_uri = f"{uri}?sessionId={session_id}&token={token}"
    async with websockets.connect(connect_uri) as ws:
        await ws.send('{"type":"CLIENT_INPUT","payload":{"direction":"NONE"}}')
        for _ in range(messages):
            await ws.send('{"type":"CLIENT_INPUT","payload":{"direction":"LEFT"}}')
            await asyncio.sleep(0)
        await ws.close()


def build_tasks(uri: str, token: str, concurrency: int, messages: int) -> List[asyncio.Task]:
    return [asyncio.create_task(worker(uri, token, messages)) for _ in range(concurrency)]


async def main() -> None:
    uri = os.environ.get("WS_URI", "ws://localhost:8080/ws/game")
    token = os.environ.get("WS_TOKEN", "test-token")
    concurrency = int(os.environ.get("WS_CONCURRENCY", "10"))
    messages = int(os.environ.get("WS_MESSAGES", "5"))

    tasks = build_tasks(uri, token, concurrency, messages)
    await asyncio.gather(*tasks)


if __name__ == "__main__":
    asyncio.run(main())
