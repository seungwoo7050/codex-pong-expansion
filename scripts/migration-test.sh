#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DB_SPEC="$ROOT_DIR/contracts/db/db.v2.md"

if [ ! -f "$DB_SPEC" ]; then
  echo "Database ownership spec missing: $DB_SPEC" >&2
  exit 1
fi

if [ ! -s "$DB_SPEC" ]; then
  echo "Database ownership spec is empty: $DB_SPEC" >&2
  exit 1
fi

echo "Validated presence of DB ownership summary at $DB_SPEC"
echo "Add per-service migration smoke tests once schemas are extracted to services/."
