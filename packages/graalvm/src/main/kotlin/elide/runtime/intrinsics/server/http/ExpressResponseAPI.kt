/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

/**
 * # Express Response API
 *
 * Defines methods available on an Express `Response` object; these are mixed into the standard [HttpResponse] so that
 * guests may specify responses using this API.
 */
@API @DelicateElideApi public interface ExpressResponseAPI {
  /**
   * Exported method allowing guest code to set the response [status] code.
   *
   * This method is not terminal, unlike [send].
   *
   * @param status The HTTP status code to send.
   */
  @Polyglot public fun status(status: Int)

  /**
   * Exported method allowing guest code to end the request/response cycle; equivalent to calling [send].
   *
   * This method is terminal.
   */
  @Polyglot public fun end()

  /**
   * Exported method allowing guest code to send a response to the client with the given [status] code and [body].
   *
   * @param status The HTTP status code to send.
   * @param body The body of the response to send.
   */
  @Polyglot public fun send(status: Int, body: PolyglotValue?)

  /**
   * Get a header from the response; this method is exported to guest code.
   *
   * @param name The name of the header to set.
   */
  @Polyglot public fun get(name: String): String?

  /**
   * Set a header to the response; this method is exported to guest code.
   *
   * Note that headers must be provided before [send] is called.
   * This method will set a header to the response, overwriting any existing header(s) by the same name.
   *
   * @param name The name of the header to set.
   * @param value The value of the header to set.
   */
  @Polyglot public fun set(name: String, value: String? = null)

  /**
   * Append a header to the response; this method is exported to guest code.
   *
   * Note that headers must be provided before [send] is called.
   * This method will add a header to the response even if there is already a header by the same name.
   *
   * @param name The name of the header to set.
   * @param value The value of the header to set.
   */
  @Polyglot public fun append(name: String, value: String? = null)
}
