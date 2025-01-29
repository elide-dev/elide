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
package elide.runtime.intrinsics.server.http

import elide.annotations.API
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.vm.annotations.Polyglot

/** Represents an HTTP response returned by the server, accessible from guest code. */
@API @DelicateElideApi public interface HttpResponse: ExpressResponseAPI {
  /**
   * Provide a header to the response; this method is exported to guest code.
   *
   * Note that headers must be provided before [send] is called.
   *
   * @param name The name of the header to set.
   * @param value The value of the header to set.
   */
  @Polyglot public fun header(name: String, value: String)

  /**
   * Exported method allowing guest code to send a response to the client with the given [status] code and [body].
   *
   * @param status The HTTP status code to send.
   * @param body The body of the response to send.
   */
  @Polyglot override fun send(status: Int, body: PolyglotValue?)

  /**
   * Append a header to the response; this method is exported to guest code.
   *
   * Note that headers must be provided before [send] is called.
   * This method will add a header to the response even if there is already a header by the same name.
   *
   * @param name The name of the header to set.
   * @param value The value of the header to set.
   */
  @Polyglot override fun append(name: String, value: String?) {
    header(name, value ?: "")
  }
}
