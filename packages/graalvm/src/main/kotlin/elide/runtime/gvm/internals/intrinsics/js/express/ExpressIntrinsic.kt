/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.intrinsics.js.express

import elide.runtime.gvm.internals.context.NativeContextManagerImpl
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.express.Express
import elide.runtime.intrinsics.js.express.ExpressApp
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.concurrent.Phaser

/**
 * Implementation for the [Express] intrinsic, capable of managing the VM context from which the route handlers are
 * passed and guarantee safe multithreaded execution of said handlers.
 */
@Intrinsic(global = ExpressIntrinsic.GLOBAL_EXPRESS)
internal class ExpressIntrinsic(
  private val contextManager: NativeContextManagerImpl
) : Express, ExpressContext, AbstractJsIntrinsic() {
  private lateinit var phaser: Phaser

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[EXPRESS_SYMBOL] = ProxyExecutable { create() }
  }
  
  override fun initialize(contextHandle: Context, phaserHandle: Phaser) {
    phaser = phaserHandle
  }

  private fun create(): ExpressApp {
    return ExpressAppIntrinsic(this)
  }

  override fun pin() {
    phaser.register()
  }

  override fun unpin() {
    phaser.arriveAndDeregister()
  }

  override fun <T> useGuest(block: Context.() -> T): T {
    return contextManager.acquire(operation = block)
  }
  
  companion object {
    const val GLOBAL_EXPRESS = "express"
    val EXPRESS_SYMBOL = GLOBAL_EXPRESS.asJsSymbol()
  }
}
