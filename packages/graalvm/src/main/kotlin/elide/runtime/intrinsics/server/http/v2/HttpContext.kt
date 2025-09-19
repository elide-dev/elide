/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.intrinsics.server.http.v2

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse

/**
 * Context encapsulating a single HTTP call handled by the server.
 *
 * Implementations are expected to interact with the low-level Netty types to provide the APIs expected by specific
 * languages and frameworks.
 */
public abstract class HttpContext : AutoCloseable {
  /**
   * Incoming request decoded by the HTTP engine. The [request] object is immutable and provides access to the headers,
   * method, URI, and other basic fields, and optionally a handle to read the incoming request's body.
   */
  public abstract val request: HttpRequest

  /**
   * A source for reading the incoming request's body; if the request does not have a body, this source will be empty.
   */
  public abstract val requestBody: HttpContentSource

  /** Outgoing [response] object, built as the context passes through middleware on its way to and from the handler. */
  public abstract val response: HttpResponse

  /**
   * A sink for writing the outgoing response's body; to send an empty response, simply close the sink without writing
   * any data to it.
   */
  public abstract val responseBody: HttpContentSink

  /**
   * Arbitrary type-safe storage for values bound to this context, can be used to store framework-specific data and
   * cache values.
   */
  public abstract val session: HttpSession
}

