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
package elide.runtime.intrinsics.js.node.util

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.runtime.gvm.js.JsError
import elide.runtime.interop.ReadOnlyProxyObject
import elide.vm.annotations.Polyglot

/**
 * ## MIME Params
 *
 * Augments [MIMEType] with structured access to parameters specified within a MIME string; this includes the `a=b`
 * portion of the string `text/html; a=b`, for example.
 *
 * @see MIMEType MIME type parsing and formatting
 */
public interface MIMEParams : ReadOnlyProxyObject, ProxyHashMap {
  /**
   * Express these MIME params as a simple host-side map of strings.
   *
   * @return A map of string keys to string values, representing the MIME parameters
   */
  public fun toMap(): Map<String, String>

  /**
   * Retrieve the value of the specified [key], if it exists, or return `null` otherwise.
   *
   * @param key The key to retrieve from the MIME parameters
   * @return The value associated with the key, or `null` if the key does not exist
   */
  @Polyglot public fun get(key: String): String?

  /**
   * Indicate whether the specified [key] exists in the MIME parameters.
   *
   * @param key The key to check.
   * @return `true` if the key exists, `false` otherwise.
   */
  @Polyglot public fun has(key: String): Boolean

  /**
   * ### MIME Params Factory
   *
   * Creates instances of [MIMEParams] from maps of parameters or as parsed instances from strings.
   */
  public interface Factory : ProxyInstantiable {
    /**
     * Parse the provided [params] string to create a [MIMEParams] instance.
     *
     * @param params String containing MIME parameters, e.g. `a=b; c=d`
     * @return A new [MIMEParams] instance containing the parsed parameters
     */
    public fun parse(params: String): MIMEParams

    /**
     * Create a [MIMEParams] instance from a map.
     *
     * @param params Map of strings.
     * @return A new [MIMEParams] instance containing the parameters
     */
    public fun create(params: Map<String, String>): MIMEParams

    override fun newInstance(vararg arguments: Value?): Any? {
      val first = arguments.firstOrNull()?.takeIf { it.isString }?.asString()
        ?: throw JsError.typeError("Parsing MIME params requires a string")
      return parse(first)
    }
  }
}
