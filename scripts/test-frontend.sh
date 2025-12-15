#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -d frontend ]; then
  cd frontend
  npm install
  npm test
else
  echo "frontend 디렉터리가 없습니다" >&2
  exit 1
fi
