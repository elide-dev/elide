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

import io.micronaut.http.HttpMessage
import java.util.Optional
import elide.http.Body
import elide.http.ContentLengthValue

// Implements an HTTP body container via Micronaut.
@JvmInline public value class MicronautBody<T> (
  private val pair: Pair<ContentLengthValue, Optional<T>>
): PlatformBody<T> {
  public constructor(contentLength: ContentLengthValue, body: Optional<T>) : this(contentLength to body)

  override val isPresent: Boolean get() = pair.second.isPresent
  override val contentLength: ContentLengthValue get() = pair.first
  override fun unwrap(): T & Any = pair.second.get()

  /** Factories for [MicronautBody] objects. */
  public companion object {
    /** @return Wrapped Micronaut body value from a Micronaut HTTP message. */
    @JvmStatic public fun <T> of(msg: HttpMessage<T>): Body = when (msg.body.isPresent) {
      false -> Body.Empty
      true -> MicronautBody(msg.contentLength.toULong(), msg.body)
    }

    /** @return Wrapped Micronaut body value. */
    @JvmStatic public fun string(value: String): MicronautBody<String> =
      MicronautBody(value.length.toULong(), Optional.of(value))
  }
}
