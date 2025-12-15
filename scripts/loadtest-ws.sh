#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export WS_URI="${WS_URI:-ws://localhost:8080/ws/game}"
export WS_TOKEN="${WS_TOKEN:-test-token}"
export WS_CONCURRENCY="${WS_CONCURRENCY:-20}"
export WS_MESSAGES="${WS_MESSAGES:-10}"
python tools/loadtest/ws_connect_storm.py
