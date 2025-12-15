#!/usr/bin/env bash
# v0.10.0 유지용 통합 검증 스크립트
set -euo pipefail

export CI=true

if ! command -v ffmpeg >/dev/null 2>&1 || ! command -v ffprobe >/dev/null 2>&1; then
  echo "ffmpeg/ffprobe가 필요합니다." >&2
  exit 1
fi

( cd backend && [ -f gradle/wrapper/gradle-wrapper.jar ] || gradle wrapper )

( cd backend && ./gradlew test --console=plain --no-daemon )
( cd frontend && npm test )
python -m pip install --upgrade pip
python -m pip install -r worker/requirements.txt
REQUIRE_FFMPEG=1 python -m unittest discover -s worker -p "test_*.py"
