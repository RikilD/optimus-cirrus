package msjava.zkapi

/** Compile-time stand-in for msjava.zkapi.ZkaPathContext; see the module README. */
class ZkaPathContext {
  def getBasePath: String = ???
  def paths(node: String): String = ???
  def exists(node: String): AnyRef = ???
  def getNodeData(node: String): Array[Byte] = ???
  def close(): Unit = ???
}
