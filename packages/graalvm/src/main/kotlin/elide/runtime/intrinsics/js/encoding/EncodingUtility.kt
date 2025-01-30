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
package elide.runtime.intrinsics.js.encoding

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API
import elide.runtime.gvm.js.JsError
import elide.vm.annotations.Polyglot

/**
 * # Encoding Utility
 *
 * Base interface, used in an abstract capacity for methods shared by [TextEncoder] and [TextDecoder]; this symbol is
 * not installed in the guest context.
 *
 * @see TextEncoder the `TextEncoder` interface
 * @see TextDecoder the `TextDecoder` interface
 */
@API public interface EncodingUtility: ProxyObject {
  /**
   * ## Encoding
   *
   * Specifies the encoding implemented by this utility instance.
   */
  @get:Polyglot public val encoding: String

  /**
   * ### Encoding Utility: Factory
   *
   * Describes constructors available for encoders; this factory is used to enforce the API for a sub-interface's
   * companion.
   */
  @API public interface Factory<T>: ProxyInstantiable where T: EncodingUtility {
    /**
     * ## Create
     *
     * Creates a new instance of the encoding utility without any parameters.
     *
     * @return The new instance.
     */
    public fun create(): T

    /**
     * ## Create with Label
     *
     * Creates a new instance of the encoding utility with a specific named encoding.
     *
     * @param encoding The encoding to use.
     * @return The new instance.
     */
    public fun create(encoding: Value?): T

    override fun newInstance(vararg arguments: Value?): Any = when (arguments.size) {
      0 -> create()
      1 -> create(arguments.getOrNull(0))
      else -> throw JsError.typeError("Invalid number of arguments")
    }
  }
}
