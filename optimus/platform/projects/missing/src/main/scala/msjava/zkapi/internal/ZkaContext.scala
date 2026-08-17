package msjava.zkapi.internal

import java.io.Closeable
import msjava.base.slr.internal.ServiceEnvironment
import org.apache.curator.framework.CuratorFramework

class ZkaContext(x: Any) extends Closeable {
  def getRootNode: String = ???
  def getData(p: String): ZkaData = ???
  def getNodeData(mode: String): Array[Byte] = ???
  def getCurator: CuratorFramework = ???
  def getEnvironment: ServiceEnvironment = ???
  def close(): Unit = ???
}

object ZkaContext {
  def contextForSubPath(context: ZkaContext, p: String): ZkaContext = ???
}