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
package elide.runtime.javascript

import org.graalvm.polyglot.proxy.ProxyExecutable
import jakarta.inject.Singleton
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic

// Name of the `alert` function in the global scope.
private const val ALERT_NAME = "alert"

/**
 * ## Browser Stubs
 *
 * Adds support for various browser or browser-related JS methods; each method is stubbed to be a no-op or a method
 * which prints its arguments.
 */
@Singleton
public class BrowserStubs: AbstractJsIntrinsic() {
  private companion object {
    // Callable function which does nothing and returns nothing.
    private val NO_OP_STUB = ProxyExecutable { /* */ }
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[ALERT_NAME.asPublicJsSymbol()] = NO_OP_STUB
  }
}
