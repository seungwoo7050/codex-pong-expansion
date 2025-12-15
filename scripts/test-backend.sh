#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cd backend
if [ -x ./gradlew ]; then
  ./gradlew test
else
  gradle test
fi
