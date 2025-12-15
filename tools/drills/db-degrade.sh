#!/usr/bin/env bash
set -euo pipefail

echo "[DRILL] MariaDB 장애/지연 재현 (v1.2.0)"
echo "- 대상: docker compose db 서비스"
echo "- 단계: stop -> 대기 -> start"

duration=${DB_DRILL_DURATION:-30}

echo "1) DB 중지..."
docker compose stop db

echo "2) ${duration}s 동안 서비스 지연 상태 유지"
sleep "${duration}"

echo "3) DB 재기동"
docker compose start db

echo "4) 관측 지표 확인: http.server.requests p95, realtime.ws.connections(status=failure), traceId=DB-DRILL"
echo "   backend 로그/트레이스에서 traceId=X-Trace-Id 값을 전달해 상관관계 확인"
