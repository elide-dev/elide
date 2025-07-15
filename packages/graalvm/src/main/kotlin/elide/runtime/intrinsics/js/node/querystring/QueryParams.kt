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
package elide.runtime.intrinsics.js.node.querystring

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject
import elide.vm.annotations.Polyglot

/**
 * ## Type: `StringOrArray`
 *
 * Describes a `String` or `Array` type for each query parameter, depending on how many values there are.
 */
public typealias StringOrArray = Any

/**
 * ## Querystring: Query Parameters
 *
 * Represents the result of parsing a URL query string into a JavaScript object.
 * This object provides access to query parameter keys and values while maintaining
 * read-only semantics consistent with Node.js querystring.parse() behavior.
 *
 * The returned object does not prototype inherit from the JavaScript Object,
 * meaning typical Object methods such as `obj.toString()`, `obj.hasOwnProperty()`,
 * and others are not defined and will not work.
 *
 * When a query parameter appears multiple times (e.g., `"foo=bar&foo=quux"`),
 * the value is returned as a proxy array that supports `.length` and array indexing.
 */
public interface QueryParams : ProxyObject {
  /**
   * The underlying query parameter data as a map.
   */
  @get:Polyglot public val data: Map<String, StringOrArray>

  @Suppress("UNCHECKED_CAST")
  override fun getMember(key: String): Any? {
    val value = data[key]
    return when (value) {
      is List<*> -> ArrayValueProxy(value as List<String>)
      else -> value
    }
  }

  override fun getMemberKeys(): Array<String> = data.keys.toTypedArray()

  override fun hasMember(key: String): Boolean = data.containsKey(key)

  override fun putMember(key: String, value: Value) {
    throw UnsupportedOperationException("Cannot modify querystring parse result")
  }

  override fun removeMember(key: String): Boolean = false

  public companion object {
    /**
     * Creates a new instance of [QueryParams] from the given data map.
     *
     * @param data The query parameter data as a map of string keys to values.
     * @return A new [QueryParams] instance wrapping the provided data.
     */

    @JvmStatic public fun of(data: Map<String, StringOrArray>): QueryParams = object : QueryParams {
      override val data: Map<String, StringOrArray> = data
    }

    /**
     * Convert a guest data structure to a [QueryParams] structure on a best-effort basis.
     *
     * @param params The guest data structure to convert.
     * @return The converted [QueryParams] structure, or null if the input is not convertible.
     */
    @JvmStatic public fun fromGuest(params: Value?): QueryParams? {
      return when {
        params == null || params.isNull-> of(emptyMap())

        params.isHostObject -> {
          val hostObject = params.asHostObject<Any>()
          if (hostObject is Map<*, *>) {
            val data = mutableMapOf<String, StringOrArray>()
            hostObject.forEach { (key, value) ->
              if (key is String && value != null) {
                data[key] = when (value) {
                  is Array<*> -> value.map { it.toString() }
                  is List<*> -> value.map { it.toString() }
                  else -> value.toString()
                }
              }
            }
            of(data)
          } else {
            null
          }
        }

        params.hasMembers() -> {
          val data = mutableMapOf<String, StringOrArray>()
          params.memberKeys.forEach { key ->
            val param = params.getMember(key)
            if (param != null && !param.isNull) {
              data[key] = when {
                param.hasArrayElements() -> {
                  (0 until param.arraySize).map { param.getArrayElement(it).toString() }
                }
                else -> param.toString()
              }
            }
          }
          of(data)
        }
        else -> null
      }
    }

    /**
     * Proxy array implementation for handling multiple values for the same query parameter key.
     * This allows JavaScript code to access array properties like `.length` and use array indexing.
     */
    internal class ArrayValueProxy(private val list: List<String>) : ProxyArray {
      override fun get(index: Long): Any? = if (index in 0 until list.size) list[index.toInt()] else null
      override fun set(index: Long, value: Value?) = throw UnsupportedOperationException("Cannot modify querystring parse result")
      override fun getSize(): Long = list.size.toLong()
      override fun remove(index: Long) = throw UnsupportedOperationException("Cannot modify querystring parse result")
    }
  }
}
