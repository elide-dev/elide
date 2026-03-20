/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.http.server.python.asgi

import io.netty.handler.codec.http.HttpResponse
import org.graalvm.polyglot.proxy.ProxyHashMap
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpResponseBody

/**
 * ASGI-specific call context attached to each incoming HTTP request.
 *
 * Holds the ASGI scope dict, receive callable, and references to the Netty response objects needed by [AsgiSend].
 * This context is created by [AsgiServerApplication.newContext] and cleaned up when the call ends.
 */
public class AsgiCallContext(
  /** The ASGI connection scope dict passed as the first argument to the application. */
  public val scope: ProxyHashMap,

  /** The ASGI receive callable that delivers request body chunks. */
  public val receive: AsgiReceive,

  /** The Netty response header object. */
  public val response: HttpResponse,

  /** The Netty response body writer. */
  public val responseBody: HttpResponseBody,
) : CallContext {
  override fun callEnded(failure: Throwable?) {
    receive.dispose()
  }
}
