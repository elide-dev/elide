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
package elide.runtime.gvm.internals.node.events

import jakarta.inject.Singleton
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.EventsAPI

// Internal symbol where the Node built-in module is installed.
private const val EVENTS_MODULE_SYMBOL = "__Elide_node_events__"

// Installs the Node `events` built-in module.
@Intrinsic internal class NodeEventsModule : AbstractNodeBuiltinModule() {
  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[EVENTS_MODULE_SYMBOL.asJsSymbol()] = NodeEventsModuleFacade()
  }
}

// Module facade which satisfies the built-in `events` module.
@Singleton internal class NodeEventsModuleFacade : EventsAPI {
  // TBD.
}
