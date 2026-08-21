package com.ms.zookeeper.clientutils

import org.apache.zookeeper.{ZooKeeper, Watcher}

object ZkClientUtils {
  def getConnectionString(zkEnv: ZkEnv, node: String): String = ???
  // Return type must be concrete: this is called from Java, where Scala's Nothing
  // is not convertible to the expected ZooKeeper.
  def getZooKeeper(c: String, id: String, t: Int, w: Watcher): ZooKeeper = ???
}
