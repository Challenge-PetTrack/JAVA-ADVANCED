#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_DIR="$(cd "$SCRIPT_DIR/../../DISRUPTIVE-ARCHITECTURES-IOT-IOB-GENERATIVE-IA/py" && pwd)"
PYTHON_BIN="$PYTHON_DIR/.venv/bin/python"

if [ ! -f "$PYTHON_BIN" ]; then
    PYTHON_BIN="python3"
fi

exec "$PYTHON_BIN" "$PYTHON_DIR/scripts/iniciar_java.py" "$@"
