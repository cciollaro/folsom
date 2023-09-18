/*
 * Copyright (c) 2018 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.spotify.folsom.client.ascii;

import com.spotify.folsom.MemcacheAuthenticationException;
import com.spotify.folsom.MemcacheStatus;
import com.spotify.folsom.client.AllRequest;
import com.spotify.folsom.client.Request;
import com.spotify.folsom.client.Utils;
import com.spotify.folsom.guava.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class FlushRequest extends AsciiRequest<MemcacheStatus>
    implements AllRequest<MemcacheStatus> {

  private static final byte[] CMD = "flush_all ".getBytes();
  private static final byte[] NO_KEY = new byte[0];
  private final int delay;

  public FlushRequest(int delay) {
    super(NO_KEY);
    this.delay = delay;
  }

  @Override
  public ByteBuf writeRequest(ByteBufAllocator alloc, ByteBuffer dst) {
    dst.put(CMD);
    dst.put(String.valueOf(Utils.ttlToExpiration(delay)).getBytes());
    dst.put(NEWLINE_BYTES);
    return toBuffer(alloc, dst);
  }

  @Override
  protected void handle(AsciiResponse response, final HostAndPort server) throws IOException {
    AsciiResponse.Type type = response.type;
    if (type == AsciiResponse.Type.OK) {
      succeed(MemcacheStatus.OK);
    } else if (type == AsciiResponse.Type.CLIENT_ERROR) {
      MemcacheAuthenticationException exception =
          new MemcacheAuthenticationException(
              "Authentication required by server. Client not authenticated.");
      fail(exception, server);
    } else {
      throw new IOException("Unexpected line: " + type);
    }
  }

  @Override
  public MemcacheStatus merge(List<MemcacheStatus> results) {
    return AllRequest.mergeMemcacheStatus(results);
  }

  @Override
  public Request<MemcacheStatus> duplicate() {
    return new FlushRequest(delay);
  }
}
