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

package elide.runtime.javascript

import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.GuestIntrinsic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.gvm.js.undefined

// Name of the `queueMicrotask` function in the global scope.
private const val QUEUE_MICROTASK_NAME = "queueMicrotask"

// Public JavaScript symbol for the `queueMicrotask` function.
private val QUEUE_MICROTASK_SYMBOL = QUEUE_MICROTASK_NAME.asPublicJsSymbol()

/**
 * ## Queue Microtask Callable
 *
 * Mounts a callable intrinsic function at the name `queueMicrotask`, in compliance with Web JavaScript standards which
 * expect this function to be available in the global scope. The `queueMicrotask` function is used to queue a chunk of
 * code to execute safely on the JavaScript event loop.
 *
 * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/Window/queueMicrotask)
 */
@Singleton
@Intrinsic(QUEUE_MICROTASK_NAME) public class QueueMicrotaskCallable @Inject constructor (
    private val executorProvider: GuestExecutorProvider,
) : ProxyExecutable, AbstractJsIntrinsic() {
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[QUEUE_MICROTASK_SYMBOL] = this
  }

  internal operator fun invoke(callable: () -> Unit) {
    executorProvider.executor().execute {
      callable.invoke()
    }
  }

  override fun execute(vararg arguments: Value?): Any? {
    val first = arguments.firstOrNull() ?: throw JsError.typeError("First argument to `queueMicrotask` is required")
    if (!first.canExecute()) throw JsError.typeError("First argument to `queueMicrotask` must be a function")
    invoke(first::executeVoid)
    return undefined()
  }
}
