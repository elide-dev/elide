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

package elide.runtime.gvm.internals.intrinsics.js.abort

import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings

/** Global where the `AbortController` type is mounted. */
private const val GLOBAL_ABORT_CONTROLLER = "AbortController"

/** Global where the `AbortSignal` type is mounted. */
private const val GLOBAL_ABORT_SIGNAL = "AbortSignal"

// Mounts access to `AbortController`.
@Intrinsic(GLOBAL_ABORT_CONTROLLER, internal = false) internal class AbortControllerIntrinsic : AbstractJsIntrinsic() {
  override fun install(bindings: MutableIntrinsicBindings) {
    // mounts `AbortController` and constructors.
    bindings[GLOBAL_ABORT_CONTROLLER.asPublicJsSymbol()] = AbortController.Factory
  }
}

// Mounts access to `AbortSignal`.
@Intrinsic(GLOBAL_ABORT_SIGNAL, internal = false) internal class AbortSignalIntrinsic @Inject constructor(
  private val guestExecutor: GuestExecutorProvider,
) : AbstractJsIntrinsic() {
  override fun install(bindings: MutableIntrinsicBindings) {
    // mounts `AbortController` and constructors.
    bindings[GLOBAL_ABORT_SIGNAL.asPublicJsSymbol()] = AbortSignal.factory(guestExecutor)
  }
}
