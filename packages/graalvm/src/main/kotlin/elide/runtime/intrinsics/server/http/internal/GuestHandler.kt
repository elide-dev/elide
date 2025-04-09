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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * ## Guest Handler
 *
 * Root of the hierarchy of supported types for guest server handlers; guests typically accept a request and produce
 * a response via calls to other methods, or by returning a future (one of [GuestSimpleHandler] or [GuestAsyncHandler]).
 */
internal sealed interface GuestHandler {
  companion object {
    /** Wraps a [PolyglotValue] and returns it as a [GuestHandler]. The [value] must be executable. */
    infix fun simple(value: PolyglotValue): GuestHandler {
      // we can only verify whether the value is function-like
      require(value.canExecute()) { "Guest handlers must be executable values." }
      return GuestSimpleHandler(value)
    }

    /** Wraps a [PolyglotValue] and returns it as a [GuestHandler]. The [value] must be executable. */
    infix fun async(value: PolyglotValue): GuestHandler {
      // we can only verify whether the value is function-like
      require(value.canExecute()) { "Guest handlers must be executable values." }
      return GuestAsyncHandler(value)
    }
  }
}
