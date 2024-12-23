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
	  )

  lazy val app = Project("buildToolApp", projectsDir / "app")
    .settings(
      scalacOptions ++= ScalacOptions.common,
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
        scalaxb,
        scalaXml,
        zinc,
      ),
		libraryDependencySchemes ++= Seq(
			"org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always,
			"org.scala-lang.modules" %% "scala-java8-compat" % VersionScheme.Always,
			"org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
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
      scalacOptions ++= ScalacOptions.common,
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
        sprayJson,
      )
    )
    .dependsOn(core, Platform.annotations)

  lazy val core = Project("buildToolCore", projectsDir / "core")
    .settings(
      libraryDependencies ++= Seq(
		jacksonDatabind,
		jacksonModuleScala,
        scalaParserCombinators,
        sprayJson,
        typesafeConfig,
		  scalaCollectionCompat,
		  scalaReflect,
	  ),
		libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % VersionScheme.Always,
    )
}
