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
package elide.runtime.gvm.internals.intrinsics.js.abort

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import kotlinx.atomicfu.atomic
import elide.runtime.gvm.js.JsError
import elide.vm.annotations.Polyglot
import elide.runtime.intrinsics.js.AbortSignal as AbortSignalAPI
import elide.runtime.intrinsics.js.AbortController as AbortControllerAPI

private const val SIGNAL_PROP = "signal"
private const val ABORT_METHOD = "abort"
private const val TRANSFERABLE_BY_DEFAULT = false

// Implements `AbortController` and `AbortController.Factory` for use in JavaScript.
public class AbortController : AbortControllerAPI, ProxyObject {
  private val transferable = atomic(TRANSFERABLE_BY_DEFAULT)
  private val managed = AbortSignal.create()

  override fun markTransferable() {
    transferable.value = true
    signal.markTransferable()
  }

  override fun canBeTransferred(): Boolean = transferable.value

  @get:Polyglot override val signal: AbortSignalAPI get() = managed

  @Polyglot override fun abort(reason: Any?): Unit = managed.assignAborted(reason)

  override fun getMemberKeys(): Array<String> = arrayOf(SIGNAL_PROP, ABORT_METHOD)
  override fun putMember(key: String?, value: Value?): Unit = Unit
  override fun hasMember(key: String): Boolean = key == SIGNAL_PROP || key == ABORT_METHOD

  override fun getMember(key: String): Any? = when (key) {
    SIGNAL_PROP -> signal
    ABORT_METHOD -> ProxyExecutable { args ->
      when (args.size) {
        0 -> abort(null)
        else -> abort(args[0])
      }
    }
    else -> null
  }

  // Implements constructor interfaces for `AbortController`.
  public companion object Factory : AbortControllerAPI.Factory {
    /**
     * Create a new instance of an [AbortController] (polyglot).
     */
    @Polyglot override fun newInstance(vararg arguments: Value?): AbortController = when (arguments.size) {
      0 -> AbortController()
      else -> throw JsError.typeError("AbortController constructor does not accept arguments")
    }
  }
}
