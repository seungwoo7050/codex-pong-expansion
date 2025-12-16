#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PROTO_DIR="$ROOT_DIR/contracts/services"

# Compile all protobuf contracts to ensure syntax correctness and deterministic
# generation. Descriptor output is discarded after compilation.
if [ -d "$PROTO_DIR" ]; then
  mapfile -t proto_files < <(find "$PROTO_DIR" -name "*.proto" | sort)
  if [ ${#proto_files[@]} -gt 0 ]; then
    tmpfile="$(mktemp)"
    protoc -I "$PROTO_DIR" --include_imports --descriptor_set_out="$tmpfile" "${proto_files[@]}"
    rm -f "$tmpfile"
    echo "Compiled ${#proto_files[@]} proto contract(s)."
  else
    echo "No proto files found under $PROTO_DIR; nothing to compile." >&2
  fi
else
  echo "Proto directory $PROTO_DIR missing" >&2
  exit 1
fi

# Validate OpenAPI specs if present using PyYAML to catch syntax issues.
OPENAPI_DIR="$ROOT_DIR/contracts/external"
if [ -d "$OPENAPI_DIR" ]; then
  if ls "$OPENAPI_DIR"/*.yaml >/dev/null 2>&1; then
    python -m pip install --quiet pyyaml
    python - <<'PY'
import pathlib, yaml
openapi_dir = pathlib.Path("contracts/external")
for path in sorted(openapi_dir.glob("*.yaml")):
    with path.open("r", encoding="utf-8") as f:
        yaml.safe_load(f)
    print(f"Validated OpenAPI syntax for {path}")
PY
  else
    echo "No OpenAPI specs found under $OPENAPI_DIR; skipping validation."
  fi
fi
