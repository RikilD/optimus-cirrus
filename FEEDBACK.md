# Bootstrap feedback for morganstanley/optimus-cirrus

Consolidated asks to ease building `optimus-cirrus` outside Morgan Stanley,
based on four independent bootstrap attempts.

## Why this document

Four people have now reverse-engineered the same build independently:

| attempt | who | approach | size |
| --- | --- | --- | --- |
| [PR #2](https://github.com/morganstanley/optimus-cirrus/pull/2) | @adpi2, @cbwl | sbt | 53 commits, closed unmerged 2025-09-24 |
| internal | @chrisandrews-ms | sbt → OBT self-host | **full bootstrap achieved 2025-03-04**, never published |
| this fork | — | sbt → OBT self-host | 304 files, +6406/-2214 |
| `gradle-bootstrap-compat-work` | @berenyihenrik | Gradle | 204 files, +5245/-968, plus 2561 lines of compat source embedded in `build.gradle` |
| [issue #4](https://github.com/morganstanley/optimus-cirrus/issues/4) | @fred-bowker | hand-rolled `scalac` | blocked at step one, **no reply since 2026-08-10** |

The most important fact: **MS already solved this internally.** On 2025-03-04
@chrisandrews-ms reported full bootstrap — OBT(2) built by OBT(1) built by
SBT(0) — and listed three next steps (publish OBT to Maven Central, clean up
the in-OBT hacks and republish, auto-generate the external `.obt` files).
None shipped. PR #2 closed unmerged in September, `main` has not moved since
2025-12-24, and issue #4 is unanswered.

**Most of what follows is "publish what you already have," not new work.**

## Convergent evidence

The two most recent attempts share **no build tool and no code**, yet both had
to patch the same **38 files**. Same root causes, sometimes literally the same
edit — both independently added `import optimus.scala212.DefaultSeq._` to
`stratosphere/common/.../PathsOpts.scala`.

Where the fixes differ, they differ only in tactic:

| file | Gradle attempt | sbt attempt |
| --- | --- | --- |
| `GridProfiler.scala` | `getStallingReasons.asScala.toSeq` | `import optimus.scala212.DefaultSeq._` |
| `SortedPropertyValues.scala` | `names.toSeq` | `import optimus.scala212.DefaultSeq._` |

Both are working around the same thing: MS-internal collection helpers
(`asScalaUnsafeImmutable`, `toVarArgsSeq`) and the 2.12/2.13 `Seq` divergence.

Convergence across independent attempts is the signal — these 38 are genuine
upstream portability defects, not artifacts of any one build tool.

## Tier 1 — cheap, unblocks everyone at step one

### 1. Add a build target for `oss-utils/shadow`

247 real, Apache-2.0-licensed msjava sources are already published — under
`optimus/platform/projects/oss-utils/shadow/msjava/src/main/resources/`.
There is **no `.obt` scope and no build target for them anywhere**, so nothing
compiles them. They are shipped as resources.

Moving them to `src/main/java` plus one build scope deletes **22 of our 46
hand-written shims outright**, including `IOUtils`, `ZkaConfig`, `ZkaContext`,
`ZkaData`, `ServiceEnvironment`, `SystemPropertyUtils`, `BackendException`,
`MSProcess`.

Best effort-to-value ratio on this list by a wide margin. It is a directory
move.

### 2. Fix `alarms` inheriting from sealed `Profiler` (issue #4)

`optimus/platform/projects/alarms/.../entity/Profiler.scala` lines 23 and 55
extend `scala.tools.nsc.profile.Profiler`, which is `sealed` in every released
Scala 2. This blocks *step one* for anyone bootstrapping by hand — alarms gates
the compiler plugins, and the plugins gate everything else.

Three attempts, three different workarounds:

- ours: comment out `ThresholdProfiler` and `DelegatingProfiler`
- Gradle attempt: generate a replacement `PluginData.scala` into a `generated/bootstrapCompat/scala` source dir (one of 14 such codegen blocks)
- issue #4: blocked outright

Suggested fix: move the profiler hook to a `scala-2.13` source dir, or disable
it in OSS builds.

**Answering issue #4 costs one reply and unblocks someone actively trying.**

### 3. Land the EntityAgent fix

`appendToBootstrapClassLoaderSearch` at `EntityAgent.java:~258` is unchanged on
`main`. @chrisandrews-ms diagnosed it and posted the fix in January 2025:
the uberjar packaging injects all of `entityagent.jar` into the bootstrap
classloader instead of just `entityagent-ext.jar`, producing

```
java.lang.IllegalAccessError: failed to access class optimus.TPDMaskTransfomer
from class optimus.EntityAgent
```

The injection is not needed to run OBT. The same comment supplies the three
required manifest attributes (`Premain-Class`, `Can-Retransform-Classes`,
`Can-Set-Native-Method-Prefix`). Neither has landed.

## Tier 2 — promised artifacts that never arrived

### 4. The `.proto` files

Requested in PR #2 in December 2024. Still **0 of 4** present on `main`:
`dsi.proto`, `prc.proto`, `expressions.proto`, `envelope.proto`. Only six
unrelated `.proto` files exist in the tree.

This forces the worst workaround in our branch: 34 files, +1343/-987 of
hand-written `MessageLite` stand-ins whose method bodies are all `???`. It
fakes the wire format — the code compiles, but nothing DAL-related can
actually run. Every attempt has to invent its own version of this.

### 5. The 24 genuinely-absent classes

Distinct from the 22 already published under `oss-utils/shadow`. Clusters:

- `msjava/hdom/*` (6) — Attribute, Document, Element, SAXBuilder, XMLOutputter, HDOMSource
- `com/ms/**/zookeeper/**` (8) — ZkClientUtils, ZkClientFactory, ZkEnv, ZkRegion, ZkConnectionResolver, ConnectionInfo, ExponentialBackoffRetryForever
- `msjava/msxml/xpath` (2) — MSXPathExpression, MSXPathUtils
- singles — `MSUuid`, `MSUuidGenerator`, `MSKerberosConfiguration`, `SSLEngineFactory`, `MSNet`, `HashMap7`, `TestplanField`, `PackageAliases`

In December 2024 MS said these were "not actually a ton of files" and aimed to
publish in January 2025.

### 6. `RuntimeScalaCompiler` and `ScalaCompilerConfigurer`

Zero occurrences on `main`; our branch restores 3 files / +214 lines. This
reads as an **export omission rather than a licensing decision** — worth
confirming which, because the two are indistinguishable from outside.

## Tier 3 — structural, from MS's own stated next steps

- **Publish OBT to Maven Central.** Removes the entire sbt stage-1 for every downstream consumer.
- **Auto-generate the external `.obt` files.** Ours hand-edits 50 of them; @chrisandrews-ms named the hand-editing as the problem in March 2025.
- **Decouple `protoc` invocation from the MS-internal path.** Flagged March 2025; still forces patches to `AnyBufGenerator` and `ResolvableResources`.
- **Pin the toolchain in-repo.** "JDK 21 works, newer JDKs break entity-agent class loading" currently exists only as a sentence in a PR comment.

## Meta-ask: a manifest of intentional omissions

The export pipeline drops files silently. From outside, `RuntimeScalaCompiler`
and the four missing `.proto` files look identical to an export bug — there is
no way to tell "withheld deliberately" from "pipeline dropped it."

A checked-in list of intentionally-omitted paths, with a one-line reason each,
would save every future contributor the reverse-engineering that four people
have now each redone from scratch.

## Suggested single action

Publish @chrisandrews-ms's internal bootstrap branch, even unpolished. It
subsumes items 3, 7, 8 and most of Tier 3, and it is work that already exists.
