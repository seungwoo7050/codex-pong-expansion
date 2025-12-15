#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "[SMOKE] 백엔드 헬스 체크"
curl -fsSL http://localhost:8080/api/health || echo "백엔드가 실행 중이 아닙니다."

echo "[SMOKE] WebSocket 게이트웨이 연결 가이드"
echo "- ws://localhost:8080/ws/game?sessionId=test&token=<JWT> 로 접속하여 SESSION_ACK 수신 여부를 확인하십시오."
