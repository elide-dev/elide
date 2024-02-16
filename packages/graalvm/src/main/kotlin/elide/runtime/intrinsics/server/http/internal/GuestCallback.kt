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

package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * Lightweight wrapper around a [PolyglotValue] that represents an executable request handler, allowing calls to the
 * underlying guest callable as a normal Kotlin function without arguments or return type.
 *
 * @see GuestCallback.of
 */
@DelicateElideApi @JvmInline internal value class GuestCallback private constructor(
  private val value: PolyglotValue
) : () -> Unit {
  override fun invoke() {
    value.executeVoid()
  }

  internal companion object {
    /** Wraps a [PolyglotValue] and returns it as a [GuestCallback]. The [value] must be executable. */
    infix fun of(value: PolyglotValue): GuestCallback {
      // we can only verify whether the value is function-like
      require(value.canExecute()) { "Guest handlers must be executable values." }
      return GuestCallback(value)
    }
  }
}
