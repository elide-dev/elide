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

package elide.http.body

import elide.http.Body
import elide.http.ContentLengthValue

/**
 * ## Primitive Body
 *
 * Defines a type hierarchy for "primitive" body types (primitive in this case means they are simple types defined by
 * the JDK, rather than actual primitive values as defined by the Java language standard).
 */
public sealed interface PrimitiveBody<T>: PlatformBody<T> {
  override val isPresent: Boolean get() = true
  override fun unwrap(): T & Any

  /**
   * Implements a primitive body for a [String] value.
   */
  @JvmInline public value class StringBody internal constructor (internal val value: String) : PrimitiveBody<String> {
    override fun toString(): String = value
    override val contentLength: ContentLengthValue get() = value.length.toULong()
    override fun unwrap(): String = value
  }

  /**
   * Implements a primitive body for a [ByteArray] value.
   */
  @JvmInline public value class Bytes internal constructor (internal val value: ByteArray) : PrimitiveBody<ByteArray> {
    override fun toString(): String = "Bytes(${value.size})"
    override val contentLength: ContentLengthValue get() = value.size.toULong()
    override fun unwrap(): ByteArray = value
  }

  /** Factories for obtaining [PrimitiveBody] values. */
  public companion object {
    /** @return String body wrapping the provided [value]. */
    @JvmStatic public fun string(value: String): StringBody = StringBody(value)

    /** @return String body wrapping the provided [byteArray]. */
    @JvmStatic public fun bytes(byteArray: ByteArray): Bytes = Bytes(byteArray)

    /** @return Body wrapping the provided [value]. */
    @JvmStatic public inline fun <reified T> of(value: T & Any): Body = when (value::class) {
      String::class -> string(value as String)
      ByteArray::class -> bytes(value as ByteArray)
      else -> error("Unsupported primitive body type: ${value::class}")
    }
  }
}
