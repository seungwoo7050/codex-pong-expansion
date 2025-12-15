#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python -m pip install --quiet -r infra/requirements.txt
python -m unittest discover -s infra/tests -p "test_*.py"
