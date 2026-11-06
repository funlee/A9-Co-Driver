#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -n "${A9_JAVA_HOME:-}" ]; then export JAVA_HOME="$A9_JAVA_HOME"; fi
case "${1:-}" in
  "") exec ./gradlew :core:checkPublic --console=plain ;;
  --private) exec ./gradlew -PwithPrivateAssets=true :core:checkPrivate --console=plain ;;
  *) echo 'Usage: tools/check-core.sh [--private]' >&2; exit 2 ;;
esac
