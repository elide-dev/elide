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

import io.netty.buffer.ByteBuf
import java.util.Optional

/**
 * ## HTTP Bodies
 *
 * Utilities for creating universal HTTP body types.
 */
public object HttpBody {
  /**
   * Create a Micronaut HTTP body from the provided [size] and [body] value.
   *
   * @param size Size of the body.
   * @param body Body value.
   */
  @JvmStatic public fun <T> micronaut(size: ULong, body: T & Any): MicronautBody<T & Any> =
    MicronautBody(size, Optional.of(body))

  /**
   * Create a Netty HTTP body from the provided [buf] value.
   *
   * @param buf Netty buffer to use as the body.
   * @return A [NettyBody] instance wrapping the provided buffer.
   */
  @JvmStatic public fun netty(buf: ByteBuf): NettyBody = NettyBody(buf)

  /**
   * Create a string HTTP body from the provided [value].
   *
   * @param value String to create a body from.
   * @return A [PrimitiveBody.StringBody] instance wrapping the provided string.
   */
  @JvmStatic public fun string(value: String): PrimitiveBody.StringBody = PrimitiveBody.string(value)

  /**
   * Create a raw byte array HTTP body from the provided [value].
   *
   * @param value Bytes to create a body from.
   * @return A [PrimitiveBody.Bytes] instance wrapping the provided bytes.
   */
  @JvmStatic public fun bytes(value: ByteArray): PrimitiveBody.Bytes = PrimitiveBody.bytes(value)
}
