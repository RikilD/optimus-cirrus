package optimus

object ScalacOptions {
  val common = Seq(
    "-language:postfixOps",
    //"-Yimports:java.lang,scala,scala.Predef,optimus.scala212.DefaultSeq"
  )
  val macros = Seq("-language:experimental.macros")
  val dynamics = Seq("-language:dynamics")
  val entityPlugin = Seq("-Xplugin-require:entity", "-Xplugin:/Users/cyrus/git/optimus-cirrus/optimus/platform/projects/entityplugin-jar/target/scala-2.12/platformEntityPluginJar-assembly-0.1.0-SNAPSHOT.jar")
}
