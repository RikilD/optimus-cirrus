#!/usr/bin/env bash
#
# Run OBT against this workspace, using the jars sbt produced in stage 1 of
# ./bootstrap.sh. Arguments are passed through to OBT.
#
#   ./run.sh                          # build every local scope
#   ./run.sh optimus.buildtool.app    # build a single scope
#
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/bootstrap.sh" --obt-only -- "$@"
