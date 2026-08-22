# Bootstrapping OBT

This branch makes the workspace build with two toolchains in sequence:

1. **stage 1 — sbt builds OBT.** `build.sbt` plus `project/*.scala` describe just
   enough of the tree (the platform runtime, the entity agent/plugin, and the
   buildtool itself) for sbt to produce a runnable OBT fat jar.
2. **stage 2 — OBT builds the workspace.** That jar reads the `.obt` files and
   builds all 46 scopes, OBT among them.

Stage 2 succeeding is the self-hosting check: OBT compiled by sbt rebuilds OBT.

```sh
./bootstrap.sh          # both stages
./bootstrap.sh --sbt-only
./bootstrap.sh --obt-only
```

## Prerequisites

| Requirement | Notes |
| --- | --- |
| JDK 21 (`21.0.5-tem`) | Pinned in `.sdkmanrc`; `bootstrap.sh` puts it on `PATH` when installed via sdkman. A newer JDK breaks entity-agent class loading. |
| sbt | Any recent 1.x; the launcher picks up `project/build.properties`. |
| Network access to Maven Central | Both stages resolve everything anonymously from `repo1.maven.org` — see `resolvers.obt`. |
| Linux, macOS or Windows on x86_64/aarch64 | Stage 2 needs a protoc binary for the host (below); the five platforms protoc publishes to Maven Central are covered. |

Roughly 2 GB of build output: two ~600 MB assembly jars under `optimus/**/target`,
plus ~200 MB in the OBT workspace.

## Stage 1: sbt

```sh
sbt platformEntityAgent/assembly buildToolAppJar/assembly
```

produces the two jars stage 2 runs with:

- `optimus/platform/projects/entityagent/target/scala-2.12/platformEntityAgent-assembly-0.1.0-SNAPSHOT.jar`
- `optimus/buildtool/projects/app-jar/target/scala-2.12/buildToolAppJar-assembly-0.1.0-SNAPSHOT.jar`

The agent jar must be the one built by the **`platformEntityAgent`** project, not
`platformEntityAgentJar`. `Premain-Class` and friends are attached to the former's
assembly (`project/Platform.scala`); `platformEntityAgentJar` overrides
`Compile / packageBin` to hand that jar to sbt consumers, but assembling *it*
writes a fresh manifest without those attributes, and the JVM then refuses to
start:

```
Failed to find Premain-Class manifest attribute in .../platformEntityAgentJar-assembly-0.1.0-SNAPSHOT.jar
Error occurred during initialization of VM
```

## Stage 2: OBT

`bootstrap.sh` sets up three things that OBT assumes and this repo does not
provide on its own.

### Workspace layout

`optimus.stratosphere.bootstrap.WorkspaceRoot` finds a workspace by walking up
from the working directory looking for `<dir>/src/stratosphere.conf` — sources
are expected to live in a `src` subdirectory. This repo *is* the source root, so
the script builds the expected shape with a symlink:

```
<OBT_WORKSPACE>/
├── src -> <repo>
├── build_obt/          # artifacts
├── depcopy/
├── logs/obt/           # logs and timing reports
└── tools/
```

`OBT_WORKSPACE` defaults to `<repo>/../.obt-workspace`. It deliberately sits
*outside* the repo: a workspace inside it would make `src` a symlink pointing at
its own ancestor, and anything walking the source tree would recurse forever.

Without this, OBT stops at:

```
Cannot infer the src directory from the current working directory (...); please run from <workspace>/src
```

### Maven credentials

`OptimusBuildToolImpl.validateCredentials` refuses to resolve maven dependencies
unless credentials, a remote cache, or a local depcopy cache is configured:

```
Build failed: no credential and cache available, unable to download 83 maven libraries!
```

Maven Central needs no credentials, so the script creates `depcopy/https` and
passes `--depCopyDir`. OBT logs `Maven server offline mode: no credential found!`
and then resolves through coursier as usual.

### protoc

`PortableAfsExecutable.AfsExecutable.file()` is hardcoded on this branch to
`Paths.get(s"protoc-3.21.1-${AfsExecutable.hostClassifier}.exe").toAbsolutePath`
— the internal AFS lookup it replaced is not available here. The script
downloads that exact binary (the version matches `protobuf-java` in
`dependencies/jvm-dependencies.obt`) into `<workspace>/tools` and symlinks it
into the source root, where the generator resolves it relative to the working
directory. The symlink is gitignored.

`hostClassifier` is protoc's Maven Central classifier for the host, e.g.
`osx-aarch_64`; `bootstrap.sh` derives the same name from `uname`, and the two
must stay in step — a binary for the wrong platform fails the build with
`cannot execute binary file`.

Stage 1 is unaffected either way: sbt gets protoc from `sbt-protobuf` instead,
at a different version (3.25.5).

### Running OBT

```sh
./run.sh                              # build every local scope
./run.sh optimus.buildtool.app        # build a single scope
./remote-debug.sh                     # same, waiting for a debugger on :5005
./debug.sh                            # same, under jdb
./test.sh                             # run the tests OBT built (see Tests below)
```

All of these route through `bootstrap.sh --obt-only`, so they get the workspace
setup above. `-e none` in the invocation is `OptimusApp`'s DAL-environment flag —
OBT runs without a DAL. Heap defaults to 4 GB (`OBT_HEAP`); extra JVM options go
in `OBT_JAVA_OPTS`.

For scale: a verified run from a fresh clone on a 20-core machine with a warm
coursier cache took ~15 minutes — about 13 of them in stage 1, then 94 seconds
for stage 2 to compile all 48 scopes (3,721 files, 584k lines). A cold cache adds
the download of ~670 jars. Repeat stage 2 runs reuse `build_obt` and are quicker
still.

## Tests

`optimus.platform.examples_platform` is the one module with a working `test`
scope. It holds `OptimusTestRunner`, a JUnit runner that starts an Optimus
runtime (`DalEnv("none")`) around a test class so tests can evaluate `@node`
code and apply tweaks, plus `SimpleCalcsTest` exercising the graph behaviour
that `BasicTweaks` demonstrates.

Either toolchain runs them:

```sh
./test.sh                               # build + run using only OBT artifacts
./test.sh <module> <TestClass>...       # a specific module, or specific classes
SKIP_BUILD=1 ./test.sh                  # reuse what OBT already built
sbt platformExamples/test               # the same tests, under sbt
```

`./test.sh` is the fully bootstrapped path — OBT-compiled test classes, the
OBT-assembled entity agent, and an `OptimusTestRunner` that OBT itself built.
It works because of two things OBT already produces:

- **Pathing jars.** OBT writes one per scope: an empty jar whose manifest
  `Class-Path` lists that scope's entire runtime classpath.
  `build_obt/classpath-mapping.txt` maps scope id to pathing jar, so a scope's
  classpath is just its pathing jar.
- **An agent jar.** `entityagent.obt` declares `agent { agentClass }` and the
  `Premain-Class` manifest entry, so `optimus.platform.entityagent.main`'s
  pathing jar is directly usable as `-javaagent:`.

The script resolves both, discovers classes named `*Test`/`*Tests` in the test
scope's own artifacts, and hands them to `org.junit.runner.JUnitCore`. Exit
status is JUnit's, so a failing assertion fails the script.

OBT has no *built-in* test executor — `--generateTestplans` (which needs
`--install`) only emits plans for an external CI system to consume. `test.sh`
is that consumer, in miniature.

Three things had to be wired up for any of this to run:

- `workspace.obt` declared a `root` for `main` but not for `test`, so any test
  scope failed with `Source folders are empty`. It now maps `test` to `src/test`.
- sbt ships no JUnit test interface, so it silently compiled test sources and
  ran nothing — `platformDebugger`'s `CompatTests` had never once executed.
  `junit-interface` is now a test dependency of both modules.
- `bootstrap.sh` grew `--print-workspace` so `test.sh` can find `build_obt`
  without duplicating the workspace-resolution logic.

## Known gaps

Things this branch knowingly leaves broken or stubbed:

- **Debug hacks in product code**, all from commit `bdf3e31` and not yet cleaned up:
  - `format/.../Message.scala` — `println` on every `Error` construction.
  - `app/.../OptimusBuildTool.scala` — `initializeCrumbs` commented out.
  - `app/.../TimingsRecorder.scala` — `buildReport` body commented out, returns `""`.
  - `app/.../PortableAfsExecutable.scala` and friends — the protoc hardcoding above.
- **Disabled modules.** `msnet-ssl`, `talks` and `tls` are commented out in
  `bundles.obt`, and individual `.obt` files carry commented-out `libs` entries
  (mostly internal `msjava.*` dependencies with no open-source equivalent).
- **Barely any tests.** `optimus.platform.examples_platform` is the only module
  with a `test` scope that builds (see below); the only other one, `msnet-ssl`,
  is disabled. A green stage 2 is otherwise the only signal.
- **`stratosphere.conf` is a stub.** Most values are placeholder `X`; only the
  handful OBT actually reads are real.
