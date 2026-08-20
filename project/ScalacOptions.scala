package optimus

object ScalacOptions {
  val common = Seq(
    "-language:postfixOps",
    //"-Yimports:java.lang,scala,scala.Predef,optimus.scala212.DefaultSeq"
  )
  val macros = Seq("-language:experimental.macros")
  val dynamics = Seq("-language:dynamics")
  // The entity compiler plugin jar is supplied to dependent modules via sbt's
  // `entityPluginJar % "plugin"` dependency (see project/Platform.scala), so no
  // absolute -Xplugin path is needed here. We only require that the plugin be present.
  val entityPlugin = Seq("-Xplugin-require:entity")
}
