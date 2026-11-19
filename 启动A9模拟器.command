#!/bin/zsh
SCRIPT_DIR="${0:A:h}"
exec python3 "$SCRIPT_DIR/tools/start-a9-emulator.py" --restart "$@"
