package com.ms.infra.zookeeper.utils.curator;

import org.apache.curator.RetryPolicy;

/** Compile-time stand-in for com.ms.infra.zookeeper.utils.curator.ExponentialBackoffRetryForever. */
public class ExponentialBackoffRetryForever {
  public static Builder builder() {
    throw new UnsupportedOperationException();
  }

  public static class Builder {
    public RetryPolicy build() {
      throw new UnsupportedOperationException();
    }
  }
}
