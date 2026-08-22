package com.ms.infra.zookeeper.utils;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

/** Compile-time stand-in for com.ms.infra.zookeeper.utils.ZkClientFactory. */
public class ZkClientFactory {
  public static ZkClientFactory create() {
    throw new UnsupportedOperationException();
  }

  public ZkClientFactory watcher(Watcher watcher) {
    throw new UnsupportedOperationException();
  }

  public ZkClientFactory sessionTimeout(int sessionTimeout) {
    throw new UnsupportedOperationException();
  }

  public ZooKeeper newZooKeeper(ConnectionInfo info) {
    throw new UnsupportedOperationException();
  }
}
