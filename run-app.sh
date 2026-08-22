#!/usr/bin/env bash
#
# Build + run an Optimus example app end-to-end under sbt.
#
# The entity agent (-javaagent) and the required JDK module flags are wired into
# project/Platform.scala (platformExamples), so this just pins the right JDK and
# hands the app off to sbt's runMain. Building the agent jar happens automatically
# as a task dependency of the run.
#
# Usage:
#   ./run-app.sh <fully.qualified.MainClass> [program args...]
#   ./run-app.sh --project <sbtProject> <fully.qualified.MainClass> [args...]
#
# Example:
#   ./run-app.sh optimus.examples.platform02.graph.BasicTweaks
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

PROJECT="platformExamples"
if [[ "${1:-}" == "--project" ]]; then
  PROJECT="$2"; shift 2
fi

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 [--project <sbtProject>] <fully.qualified.MainClass> [program args...]" >&2
  echo "Example: $0 optimus.examples.platform02.graph.BasicTweaks" >&2
  exit 2
fi

# Pin the JDK to the version declared in .sdkmanrc. The entity agent and graph
# runtime are compiled/instrumented against it; running on a newer JDK (e.g. 25)
# breaks agent class loading and ASM bytecode transformation.
if [[ -f .sdkmanrc ]]; then
  JAVA_VER="$(grep -E '^[[:space:]]*java=' .sdkmanrc | tail -1 | cut -d= -f2 | tr -d '[:space:]')"
  CAND="$HOME/.sdkman/candidates/java/$JAVA_VER"
  if [[ -n "$JAVA_VER" && -d "$CAND" ]]; then
    export JAVA_HOME="$CAND"
    export PATH="$JAVA_HOME/bin:$PATH"
  else
    echo "WARNING: JDK '$JAVA_VER' from .sdkmanrc not found at $CAND; using java on PATH ($(java -version 2>&1 | head -1))." >&2
  fi
fi

APP="$1"; shift
echo ">> JDK: $(java -version 2>&1 | head -1)"
echo ">> Running $PROJECT/runMain $APP $*"
exec sbt "$PROJECT/runMain $APP $*"
