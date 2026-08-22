package com.ms.infra.zookeeper.utils.hosted;

import com.ms.infra.zookeeper.utils.ConnectionInfo;

/** Compile-time stand-in for com.ms.infra.zookeeper.utils.hosted.ZkConnectionResolver. */
public class ZkConnectionResolver {
  public static ZkConnectionResolver fromEnvRegionChroot(ZkEnv env, ZkRegion region, String chroot) {
    throw new UnsupportedOperationException();
  }

  public ConnectionInfo resolve() {
    throw new UnsupportedOperationException();
  }
}
