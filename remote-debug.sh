#!/usr/bin/env bash
#
# Run OBT with a jdwp agent and wait for a debugger to attach on port 5005
# (override with OBT_DEBUG_PORT). Arguments are passed through to OBT.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${OBT_DEBUG_PORT:-5005}"

echo ">> Waiting for a debugger on localhost:$PORT"
export OBT_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:$PORT -Doptimus.debug.assist=false ${OBT_JAVA_OPTS:-}"
exec "$ROOT/run.sh" "$@"
