#!/usr/bin/env bash
#
# Bootstrap OBT (Optimus Build Tool) from a clean checkout, in two stages:
#
#   stage 1 (sbt)  sbt builds the entity agent and the OBT application fat jar.
#   stage 2 (obt)  that OBT jar builds this workspace -- including OBT itself.
#
# Stage 2 is the self-hosting check: OBT built by sbt must be able to rebuild
# OBT. See BOOTSTRAP.md for what each piece is and why it is needed.
#
# Usage:
#   ./bootstrap.sh                     # both stages
#   ./bootstrap.sh --sbt-only          # stage 1 only
#   ./bootstrap.sh --obt-only          # stage 2 only (reuses existing jars)
#   ./bootstrap.sh -- <extra obt args> # pass extra args through to OBT
#   ./bootstrap.sh --print-cmd         # print the stage 2 invocation, run nothing
#   ./bootstrap.sh --print-workspace   # print the resolved workspace root, run nothing
#
# To run the tests OBT built, see ./test.sh.
#
# Environment:
#   OBT_WORKSPACE   workspace root for stage 2 (default: <repo>/../.obt-workspace)
#   OBT_HEAP        heap for the OBT JVM (default: 4g)
#   OBT_JAVA_OPTS   extra JVM options for the OBT JVM
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

RUN_SBT=1
RUN_OBT=1
PRINT_CMD=0
PRINT_WORKSPACE=0
OBT_EXTRA_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sbt-only) RUN_OBT=0; shift ;;
    --obt-only) RUN_SBT=0; shift ;;
    --print-cmd) RUN_SBT=0; PRINT_CMD=1; shift ;;
    --print-workspace) RUN_SBT=0; PRINT_WORKSPACE=1; shift ;;
    --) shift; OBT_EXTRA_ARGS=("$@"); break ;;
    -h|--help) sed -n '2,25p' "$0" | sed 's/^# \?//'; exit 0 ;;
    *) echo "Unknown argument: $1 (see --help)" >&2; exit 2 ;;
  esac
done

# ---------------------------------------------------------------------------
# JDK
#
# Both stages must run on the JDK pinned in .sdkmanrc. The entity agent does
# ASM bytecode transformation at class-load time and the graph runtime pokes at
# internal JDK classes; a newer JDK (25, say) breaks agent class loading.
# ---------------------------------------------------------------------------
if [[ -f .sdkmanrc ]]; then
  JAVA_VER="$(grep -E '^[[:space:]]*java=' .sdkmanrc | tail -1 | cut -d= -f2 | tr -d '[:space:]')"
  CAND="$HOME/.sdkman/candidates/java/$JAVA_VER"
  if [[ -n "$JAVA_VER" && -d "$CAND" ]]; then
    export JAVA_HOME="$CAND"
    export PATH="$JAVA_HOME/bin:$PATH"
  else
    echo "WARNING: JDK '$JAVA_VER' from .sdkmanrc not found at $CAND;" \
         "using java on PATH ($(java -version 2>&1 | head -1))." >&2
  fi
fi
# Progress goes to stderr so --print-cmd's stdout stays machine-readable.
echo ">> JDK: $(java -version 2>&1 | head -1)" >&2

# ---------------------------------------------------------------------------
# Jar locations.
#
# The agent jar must be the `platformEntityAgent` assembly, NOT the
# `platformEntityAgentJar` one: the Premain-Class/Can-Retransform-Classes
# manifest attributes are baked into the former (see project/Platform.scala).
# Assembling the latter produces a fresh manifest without them, and the JVM
# then refuses to start with "Failed to find Premain-Class manifest attribute".
# ---------------------------------------------------------------------------
#
# The target directory carries the scala binary version, so read it out of the
# build rather than hardcoding it here -- they drift apart otherwise.
SCALA_BIN="$(sed -n 's/.*val scala2Version *= *"\([0-9]*\.[0-9]*\)\..*/\1/p' "$ROOT/project/Dependencies.scala" | head -1)"
SCALA_BIN="${SCALA_BIN:-2.13}"
AGENT_JAR="$ROOT/optimus/platform/projects/entityagent/target/scala-$SCALA_BIN/platformEntityAgent-assembly-0.1.0-SNAPSHOT.jar"
OBT_JAR="$ROOT/optimus/buildtool/projects/app-jar/target/scala-$SCALA_BIN/buildToolAppJar-assembly-0.1.0-SNAPSHOT.jar"

if [[ $RUN_SBT -eq 1 ]]; then
  echo ">> Stage 1/2: building OBT with sbt"
  sbt "platformEntityAgent/assembly" "buildToolAppJar/assembly"
fi

if [[ $RUN_OBT -eq 0 ]]; then
  echo ">> Stage 1 done (--sbt-only); skipping stage 2."
  exit 0
fi

if [[ $PRINT_WORKSPACE -eq 0 ]]; then
  for jar in "$AGENT_JAR" "$OBT_JAR"; do
    if [[ ! -f "$jar" ]]; then
      echo "ERROR: missing $jar -- run without --obt-only to build it." >&2
      exit 1
    fi
  done
fi

# ---------------------------------------------------------------------------
# Workspace layout.
#
# OBT locates a workspace by walking up from the working directory looking for
# <dir>/src/stratosphere.conf (optimus.stratosphere.bootstrap.WorkspaceRoot),
# i.e. it expects the sources to sit in a `src` subdirectory of the workspace
# root. This repo is itself the source root, so stage 2 builds that layout by
# symlinking <workspace>/src at the repo.
#
# The workspace lives OUTSIDE the repo on purpose: putting it inside would make
# <repo>/.../src a symlink back to <repo>, and anything walking the source tree
# would recurse forever. OBT writes build_obt/ and logs/ under the workspace
# root, so keeping it out of the repo also keeps the checkout clean.
# ---------------------------------------------------------------------------
WORKSPACE="${OBT_WORKSPACE:-$(dirname "$ROOT")/.obt-workspace}"
mkdir -p "$WORKSPACE"
WORKSPACE="$(cd "$WORKSPACE" && pwd)"
if [[ "$WORKSPACE" == "$ROOT"/* || "$WORKSPACE" == "$ROOT" ]]; then
  echo "ERROR: OBT_WORKSPACE ($WORKSPACE) is inside the repo ($ROOT)." >&2
  echo "       Its src symlink would point at its own ancestor and any walk of" >&2
  echo "       the source tree would recurse forever. Pick a path outside." >&2
  exit 2
fi
ln -sfn "$ROOT" "$WORKSPACE/src"

# --print-workspace resolves the workspace (creating it if needed) and stops, so
# other scripts can find build_obt without duplicating the logic above.
if [[ $PRINT_WORKSPACE -eq 1 ]]; then
  echo "$WORKSPACE"
  exit 0
fi

# OBT refuses to build maven dependencies with neither credentials nor a cache
# (OptimusBuildToolImpl.validateCredentials). This workspace resolves everything
# from Maven Central anonymously, so the depcopy cache directory stands in for
# the credentials an internal artifact server would need; OBT then logs
# "Maven server offline mode" and resolves through coursier as normal.
DEPCOPY="$WORKSPACE/depcopy"
mkdir -p "$DEPCOPY/https"

# The protobuf source generator resolves its executable as the relative path
# "protoc-<version>-<classifier>.exe" against the working directory -- see
# PortableAfsExecutable.AfsExecutable.file(), which stands in for the internal
# AFS lookup. Fetch that exact binary (the version matches protobuf-java in
# dependencies/jvm-dependencies.obt) and link it where the generator will look.
# The classifier must match AfsExecutable.hostClassifier: a binary for the wrong
# platform fails the build with "cannot execute binary file".
PROTOC_VERSION=3.21.1
case "$(uname -s)" in
  Darwin) PROTOC_OS=osx ;;
  MINGW*|MSYS*|CYGWIN*) PROTOC_OS=windows ;;
  *) PROTOC_OS=linux ;;
esac
case "$(uname -m)" in
  arm64|aarch64) PROTOC_ARCH=aarch_64 ;;
  *) PROTOC_ARCH=x86_64 ;;
esac
PROTOC_EXE="protoc-$PROTOC_VERSION-$PROTOC_OS-$PROTOC_ARCH.exe"
PROTOC_CACHED="$WORKSPACE/tools/$PROTOC_EXE"
if [[ ! -x "$PROTOC_CACHED" ]]; then
  echo ">> Fetching $PROTOC_EXE" >&2
  mkdir -p "$WORKSPACE/tools"
  curl -fsSL -o "$PROTOC_CACHED" \
    "https://repo1.maven.org/maven2/com/google/protobuf/protoc/$PROTOC_VERSION/$PROTOC_EXE"
  chmod +x "$PROTOC_CACHED"
fi
ln -sfn "$PROTOC_CACHED" "$ROOT/$PROTOC_EXE"

# The --add-exports flags open the JDK internals the graph runtime reaches into;
# without them OBT dies during scheduler startup.
# -e none is OptimusApp's DAL environment flag -- run without a DAL.
JVM_ARGS=(
  -Doptimus.gthread.ideal=-1
  -Doptimus.logging.checkAsync=false
  -Dlogback.configurationFile="$ROOT/optimus/platform/projects/entityplugin/src/main/resources/logback.xml"
  --add-exports=java.base/jdk.internal.vm=ALL-UNNAMED
  --add-exports=java.management/sun.management=ALL-UNNAMED
  "-Xmx${OBT_HEAP:-4g}"
  -javaagent:"$AGENT_JAR"
)
# Extra JVM options, e.g. a jdwp agent -- see remote-debug.sh.
if [[ -n "${OBT_JAVA_OPTS:-}" ]]; then
  # shellcheck disable=SC2206  # deliberate word splitting of caller-supplied opts
  JVM_ARGS+=($OBT_JAVA_OPTS)
fi

OBT_ARGS=(
  -cp "$OBT_JAR"
  optimus.buildtool.OptimusBuildTool
  -e none
  --workspaceDir "$WORKSPACE"
  --depCopyDir "$DEPCOPY"
  "${OBT_EXTRA_ARGS[@]}"
)

# --print-cmd emits the resolved invocation, one argument per line, after doing
# the workspace setup above: JVM arguments, a lone "--", then the arguments from
# -cp onwards. Debug wrappers build their own command from it (see debug.sh).
if [[ $PRINT_CMD -eq 1 ]]; then
  printf '%s\n' "${JVM_ARGS[@]}" "--" "${OBT_ARGS[@]}"
  exit 0
fi

echo ">> Stage 2/2: building workspace with OBT"
echo ">> Workspace: $WORKSPACE (src -> $ROOT)"

exec java "${JVM_ARGS[@]}" "${OBT_ARGS[@]}"
