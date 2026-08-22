package optimus

import optimus.Dependencies.*
import sbt.*
import sbt.Keys.*
import sbtassembly.AssemblyPlugin.autoImport._

object BuildTool {
  private val projectsDir = file("optimus/buildtool/projects")

  lazy val appJar = Project("buildToolAppJar", projectsDir / "app-jar")
	  .settings(
		exportJars := true,
		  Compile / packageBin := (app / assembly).value,
		  // packageBin above is already the app's fat jar, so assembling this project
		  // merges that jar with scala-library a second time and every scala/** entry
		  // collides. Take the first copy rather than failing on the duplicates.
		  assemblyMergeStrategy := {
			  case x if x.endsWith(".SF") || x.endsWith(".DSA") || x.endsWith(".RSA") => MergeStrategy.discard
			  case _                                                                 => MergeStrategy.first
		  },
	  )

  lazy val app = Project("buildToolApp", projectsDir / "app")
    .settings(
      scalacOptions ++= ScalacOptions.common ++ ScalacOptions.defaultSeqImports,
      // The fingerprintdiffing package is not fully exported: BuildArtifactComparatorApp
      // references FingerprintDirComparison, ArtifactsSearchStrategy, JarHashingStrategy,
      // DiffMode, FingerprintPaths and hashBuildArtifacts, none of which are defined
      // anywhere in this repository. It is a standalone diffing app that OBT itself does
      // not call, so leave it out of the build until the missing sources are published.
      Compile / unmanagedSources / excludeFilter :=
        (Compile / unmanagedSources / excludeFilter).value ||
          new SimpleFileFilter(_.getPath.contains("/fingerprintdiffing/")),
		assemblyMergeStrategy := {
			case x if x.endsWith(".SF") || x.endsWith(".DSA") || x.endsWith(".RSA") => MergeStrategy.discard
				case x =>  MergeStrategy.first
		},
      libraryDependencies ++= Seq(
		avroCompiler,
        bsp4j,
		  coursier,
        cxfTools,
        cxfToolsWsdlto,
        jgit,
        jmustache,
        jsonSchema2Pojo,
        jsoniterScalaCore,
        jsoniterScalaMacros,
        scalaxb,
        scalaXml,
        zinc,
      ),
		libraryDependencySchemes ++= Seq(
			"org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always,
			"org.scala-lang.modules" %% "scala-java8-compat" % VersionScheme.Always,
			"org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
			// coursier 2.1.x pulls plexus-archiver, which wants a newer zstd-jni than
			// kafka-clients and platform/utils do.
			"com.github.luben" % "zstd-jni" % VersionScheme.Always,
		),
	)
    .dependsOn(
      format,
      rest,
      runConf,
      DHT.client3,
//      Platform.entityPlugin,
      Platform.entityPluginJar % "plugin",
      Platform.gitUtils,
      Platform.platform,
      Stratosphere.common,
    )

  lazy val rest = Project("buildToolRest", projectsDir / "rest")
    .settings(libraryDependencies ++= Seq(args4j))
    .dependsOn(Platform.platform)

  lazy val runConf = Project("buildToolRunConf", projectsDir / "runconf")
    .settings(
      scalacOptions ++= ScalacOptions.common ++ ScalacOptions.defaultSeqImports,
      libraryDependencies ++= Seq(slf4j, typesafeConfig)
    )
    .dependsOn(core, Platform.scalaCompat, Platform.utils)

  lazy val format = Project("buildToolFormat", projectsDir / "format")
    .settings(
      scalacOptions ++= ScalacOptions.common,
      libraryDependencies ++= Seq(
        args4j,
		jgit,
		jibCore,
		typesafeConfig,
		zstdJni,
        jacksonModuleScala,
		  //"ossscala.scala",
        jsoniterScalaCore,
        jsoniterScalaMacros,
        openCSV,
        sprayJson,
      )
    )
    .dependsOn(core, Platform.annotations)

  lazy val core = Project("buildToolCore", projectsDir / "core")
    .settings(
      libraryDependencies ++= Seq(
		jacksonDatabind,
		jacksonModuleScala,
        jsoniterScalaCore,
        jsoniterScalaMacros,
        scalaParserCombinators,
        sprayJson,
        typesafeConfig,
		  scalaCollectionCompat,
		  scalaReflect,
	  ),
		libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always,
    )
}
