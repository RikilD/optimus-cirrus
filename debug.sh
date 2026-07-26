#!/usr/bin/env bash
#
# Run OBT under jdb. Same invocation as ./run.sh, remapped to jdb's argument
# syntax: -D options and -classpath are jdb's own, everything else destined for
# the debuggee VM needs an -R prefix.
#
# For IDE debugging use ./remote-debug.sh instead.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

JDB_ARGS=()
in_jvm_args=1
while IFS= read -r arg; do
  if [[ $in_jvm_args -eq 1 ]]; then
    case "$arg" in
      --) in_jvm_args=0 ;;
      -D*) JDB_ARGS+=("$arg") ;;
      *) JDB_ARGS+=("-R$arg") ;;
    esac
  elif [[ "$arg" == "-cp" ]]; then
    JDB_ARGS+=("-classpath")
  else
    JDB_ARGS+=("$arg")
  fi
done < <("$ROOT/bootstrap.sh" --print-cmd -- "$@")

exec jdb "${JDB_ARGS[@]}"
