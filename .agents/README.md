# Optimus Cirrus in Amp orbs

`.agents/setup` installs Debian's OpenJDK 17 headless JDK and the official,
checksum-verified Scala 2.12.20 distribution, including its compiler, standard
library, and reflection library. The Scala version comes from
`optimus/platform/projects/oss-utils/docs/external/optimus_buildtool_app_main_deps.txt`.
JDK 17 is a baseline for standalone checks, not a declared repository-wide JDK pin.
Some source trees target Scala 2.13; the installed compiler is for 2.12 sources only.

Amp snapshots the installed tools. Warm setup checks installed packages and the
Scala installation before downloading anything. `java`, `javac`, `scala`, and
`scalac` are on the standard PATH; new login shells also receive `JAVA_HOME` and
`SCALA_HOME` via `/etc/profile.d/optimus-cirrus.sh`. Setup owns that profile file
and the Scala symlinks in `/usr/local/bin` inside the orb.

`.agents/resume` only checks that the tools are present. It performs no downloads
or authentication. This checkout defines no public dev server or database setup,
so no orb services are started.

## Full builds are not available from this checkout

The public README (`.github/README.md`, Upcoming Work) says that bootstrapping and
publishing the Optimus Build Tool (OBT) is unfinished. The `.obt` files are not
Maven or sbt build definitions. The external dependency inventory also includes
private `msjava` artifacts and patched libraries, and Stratosphere configuration
relies on internal infrastructure. It is an inventory, not a usable lockfile.
The contributing guide says the test suite is not public.

Consequently, setup does **not** claim to resolve all project dependencies or
enable a full build/test run. It does not install unrelated build tools, attempt
internal repository access, or substitute public artifacts for private ones.
Agents can compile dependency-free files or run focused standalone checks. A full
build requires a working public OBT bootstrap and dependency configuration, or
separately supplied access to the internal build environment.

## Verify lifecycle changes

From the repository root:

```bash
bash -n .agents/setup && bash -n .agents/resume
time .agents/setup
time .agents/setup # should perform no package downloads
time .agents/resume
env -i HOME="$HOME" USER="$USER" PATH=/usr/local/bin:/usr/bin:/bin \
  /bin/bash -lc 'printf "%s\n" "$JAVA_HOME" "$SCALA_HOME"; javac -version; scalac -version'
```

These files must reach the Amp project's default branch before future orbs use
them. Exact snapshots skip setup; stale snapshots retain installed packages and
run setup again. No snapshot deletion is needed for ordinary setup changes.
