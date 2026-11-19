#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -n "${A9_JAVA_HOME:-}" ]; then export JAVA_HOME="$A9_JAVA_HOME"; fi
case "${1:-}" in
  "") exec ./gradlew :core:checkPublic :core:checkCore --console=plain ;;
  --local-screenshots|--private) exec ./gradlew :core:checkLocalScreenshots --console=plain ;;
  *) echo 'Usage: tools/check-core.sh [--local-screenshots]' >&2; exit 2 ;;
esac
