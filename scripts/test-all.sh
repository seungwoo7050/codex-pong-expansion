#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/contract-test.sh
./scripts/migration-test.sh
./scripts/test-backend.sh
if [ -d frontend ]; then
  ./scripts/test-frontend.sh
fi
./scripts/test-infra.sh
