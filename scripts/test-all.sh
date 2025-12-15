#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/test-backend.sh
if [ -d frontend ]; then
  ./scripts/test-frontend.sh
fi
