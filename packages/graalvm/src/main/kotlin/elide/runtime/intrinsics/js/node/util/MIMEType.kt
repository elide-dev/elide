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
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.js.JsError
import elide.vm.annotations.Polyglot

/**
 * ## MIME Type
 *
 * Utility class which is capable of parsing and representing MIME type strings, such as `text/html`; the left side of
 * the slash is referred to as the "type," and the right side is referred to as the "subtype." An effort is made to
 * convert parsed input arguments to strings.
 *
 * [Node.js Documentation](https://nodejs.org/docs/latest/api/util.html#class-utilmimetype)
 */
public interface MIMEType : ProxyObject {
  /**
   * ### MIME Type
   *
   * Returns the portion of the MIME type string before the slash, such as `text` in `text/html`. This property can be
   * mutated to adjust the MIME type string.
   */
  @get:Polyglot @set:Polyglot public var type: String

  /**
   * ### MIME Subtype
   *
   * Returns the portion of the MIME type string after the slash, such as `html` in `text/html`. This property can be
   * mutated to adjust the MIME type string.
   */
  @get:Polyglot @set:Polyglot public var subtype: String

  /**
   * ### MIME Essence String
   *
   * Returns the combined "full" MIME type string, such as `text/html` in `text/html`. This property is read-only.
   */
  @get:Polyglot public val essence: String

  /**
   * ### MIME Params
   *
   * Returns structured access to this MIME type's parameters, if any. This is a read-only property.
   */
  @get:Polyglot public val params: MIMEParams?

  /**
   * ### MIME Type Factory
   *
   * Models the static interface expected when constructing new [MIMEType] instances.
   */
  public interface Factory : ProxyInstantiable {
    /**
     * Create a new [MIMEType] by parsing a string.
     *
     * @param mimeType String to parse as a MIME type.
     * @return A new [MIMEType] instance representing the parsed MIME type.
     */
    public fun parse(mimeType: String): MIMEType

    /**
     * Create a new [MIMEType] with constituent parts.
     *
     * @param type Type portion of the MIME type (before the slash).
     * @param subtype Subtype portion of the MIME type (after the slash).
     * @return A new [MIMEType] instance representing the specified type and subtype.
     */
    public fun create(type: String, subtype: String): MIMEType

    override fun newInstance(vararg arguments: Value?): Any? {
      val first = arguments.firstOrNull()
      return when {
        first == null || first.isNull -> throw JsError.typeError("Parsing a MIME type requires a string argument")
        first.isString -> parse(first.asString())
        first.hasMember("toString") && first.canInvokeMember("toString") -> {
          val str = runCatching { first.invokeMember("toString") }.getOrNull()
            ?: throw JsError.typeError(
              "Parsing a MIME type requires a string argument, but could not convert to string: $first"
            )
          parse(str.asString())
        }
        else -> throw JsError.typeError("MIME type must be a string, but got: $first")
      }
    }
  }
}
