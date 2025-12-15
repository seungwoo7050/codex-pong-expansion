#!/usr/bin/env bash
set -euo pipefail

echo "[DRILL] Redis 장애/지연 재현 (v1.2.0)"
echo "- 대상: docker compose redis 서비스"
echo "- 단계: pause -> 대기 -> unpause"

duration=${REDIS_DRILL_DURATION:-20}

echo "1) Redis 일시중지"
docker compose pause redis

echo "2) ${duration}s 동안 게이트웨이/샤드 메시지 정체 관찰"
sleep "${duration}"

echo "3) Redis 재개"
docker compose unpause redis

echo "4) 메트릭 확인: realtime.ws.connections(status=failure), realtime.match.start(status=failure), traceId=REDIS-DRILL"
