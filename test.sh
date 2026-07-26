#!/usr/bin/env bash
#
# Run a scope's JUnit tests using only OBT-built artifacts -- the classes OBT
# compiled, the entity agent OBT assembled, and the OptimusTestRunner OBT
# type-checked. No sbt involved.
#
# Usage:
#   ./test.sh                              # build + run the default module's tests
#   ./test.sh <module>                     # e.g. optimus.platform.examples_platform
#   ./test.sh <module> <TestClass>...      # run only these fully-qualified classes
#
# Environment:
#   OBT_WORKSPACE  workspace root (same default as bootstrap.sh)
#   OBT_HEAP       heap for the test JVM (default: 2g)
#   SKIP_BUILD     set to 1 to reuse the existing artifacts instead of rebuilding
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

MODULE="${1:-optimus.platform.examples_platform}"
# Whatever is left in "$@" after this is the explicit list of test classes.
[[ $# -gt 0 ]] && shift

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  "$ROOT/run.sh" "$MODULE"
fi

WORKSPACE="$("$ROOT/bootstrap.sh" --print-workspace)"

# Pin the JDK exactly as bootstrap.sh does; the entity agent instruments at
# class load and will not attach on a newer JDK.
if [[ -f .sdkmanrc ]]; then
  JAVA_VER="$(grep -E '^[[:space:]]*java=' .sdkmanrc | tail -1 | cut -d= -f2 | tr -d '[:space:]')"
  CAND="$HOME/.sdkman/candidates/java/$JAVA_VER"
  if [[ -n "$JAVA_VER" && -d "$CAND" ]]; then
    export JAVA_HOME="$CAND"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi

# ---------------------------------------------------------------------------
# Locate OBT's outputs.
#
# OBT writes one "pathing" jar per scope: an otherwise empty jar whose manifest
# Class-Path lists that scope's whole runtime classpath. build_obt's
# classpath-mapping.txt maps scope id -> pathing jar, so a scope's classpath is
# just its pathing jar.
#
# The entity agent's pathing jar doubles as the -javaagent jar: entityagent.obt
# declares agentClass and the Premain-Class manifest entry, so OBT bakes the
# agent attributes into it.
# ---------------------------------------------------------------------------
MAPPING="$WORKSPACE/build_obt/classpath-mapping.txt"
if [[ ! -f "$MAPPING" ]]; then
  echo "ERROR: $MAPPING not found -- build the workspace first (./run.sh)." >&2
  exit 1
fi

# A mapping entry is "<scope id>\t<path>[:<path>...]": the scope's own pathing
# jar first, then any bundle pathing jars, which we don't want. Paths are
# written with a leading doubled slash.
scope_jar() {
  local scope="$1" jar
  jar="$(awk -F'\t' -v s="$scope" '$1 == s { split($2, p, ":"); print p[1] }' "$MAPPING" | sed 's|^//|/|')"
  if [[ -z "$jar" || ! -f "$jar" ]]; then
    echo "ERROR: no artifact for scope '$scope' in $MAPPING." >&2
    return 1
  fi
  printf '%s' "$jar"
}

TEST_SCOPE="$MODULE.test"
CLASSPATH_JAR="$(scope_jar "$TEST_SCOPE")"
AGENT_JAR="$(scope_jar optimus.platform.entityagent.main)"

# ---------------------------------------------------------------------------
# Discover test classes.
#
# Read the pathing jar's manifest Class-Path (unfolding the 72-column wrapping
# the jar spec mandates), keep the entries belonging to this scope, and list the
# top-level classes they contain whose names end in Test or Tests.
# ---------------------------------------------------------------------------
discover_tests() {
  local manifest entry
  manifest="$(unzip -p "$CLASSPATH_JAR" META-INF/MANIFEST.MF |
    sed -e ':a' -e 'N;$!ba' -e 's/\r\{0,1\}\n //g' |
    grep '^Class-Path:' | sed 's/^Class-Path: *//')"

  for entry in $manifest; do
    entry="${entry#file:}"
    entry="/${entry#"${entry%%[!/]*}"}"
    [[ "$(basename "$entry")" == "$TEST_SCOPE".* ]] || continue
    [[ -f "$entry" ]] || continue
    unzip -Z1 "$entry" '*.class' 2>/dev/null |
      grep -vE '\$' |
      sed -e 's|\.class$||' -e 's|/|.|g' |
      grep -E '(Test|Tests)$' || true
  done | sort -u
}

TEST_CLASSES=()
if [[ $# -gt 0 ]]; then
  TEST_CLASSES=("$@")
else
  # A read loop rather than mapfile, which macOS's stock /bin/bash 3.2 lacks.
  while IFS= read -r cls; do
    TEST_CLASSES+=("$cls")
  done < <(discover_tests)
fi

if [[ ${#TEST_CLASSES[@]} -eq 0 ]]; then
  echo "ERROR: no test classes found in $TEST_SCOPE." >&2
  exit 1
fi

echo ">> Running ${#TEST_CLASSES[@]} test class(es) from $TEST_SCOPE"
printf '   %s\n' "${TEST_CLASSES[@]}"

# The --add-exports/--add-opens set matches project/Platform.scala: the graph
# runtime reaches into JDK internals and will not start without them.
exec java \
  -Doptimus.logging.checkAsync=false \
  --add-exports=java.management/sun.management=ALL-UNNAMED \
  --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-exports=java.base/jdk.internal.vm=ALL-UNNAMED \
  --add-opens=java.base/java.lang.ref=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  "-Xmx${OBT_HEAP:-2g}" \
  -javaagent:"$AGENT_JAR" \
  -cp "$CLASSPATH_JAR" \
  org.junit.runner.JUnitCore \
  "${TEST_CLASSES[@]}"
