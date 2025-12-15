#!/usr/bin/env bash
set -euo pipefail

echo "[DRILL] 게이트웨이/샤드 강제 종료 (v1.2.0)"
echo "- 대상: docker compose backend 서비스를 게이트웨이/샤드 대용으로 사용"

target=${GATEWAY_SHARD_SERVICE:-backend}
delay=${GATEWAY_REJOIN_DELAY:-5}

echo "1) ${target} 컨테이너 kill"
docker compose kill "${target}"

echo "2) ${delay}s 대기 후 재기동"
sleep "${delay}"
docker compose start "${target}"

echo "3) traceId=GATEWAY-DRILL 인 세션을 열어 termination 이벤트와 메트릭 상승을 확인"
