/*
 * Morgan Stanley makes this available to you under the Apache License, Version 2.0 (the "License").
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package msjava.base.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Compile-time stand-in for msjava.base.dns.CachingNameService. */
public class CachingNameService implements MSNameService {
  @Override
  public InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getHostByAddr(byte[] addr) throws UnknownHostException {
    throw new UnsupportedOperationException();
  }

  public static class Builder {
    public Builder delegate(MSNameService delegate) {
      throw new UnsupportedOperationException();
    }

    public CachingNameService build() {
      throw new UnsupportedOperationException();
    }
  }
}
