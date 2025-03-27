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
package elide.runtime.node.diagnostics

import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.DiagnosticsChannelAPI
import elide.runtime.lang.javascript.NodeModuleName

// Constants for the Node `diagnostics_channel` module.
private const val HAS_SUBSCRIBERS_FN = "hasSubscribers"
private const val CHANNEL_FN = "channel"
private const val SUBSCRIBE_FN = "subscribe"
private const val UNSUBSCRIBE_FN = "unsubscribe"
private const val TRACING_CHANNEL_FN = "tracingChannel"
private const val CHANNEL_CTOR = "Channel"
private const val TRACING_CHANNEL_CTOR = "TracingChannel"

// All props for the `diagnostics_channel` module.
private val DIAGNOSTICS_CHANNEL_PROPS = arrayOf(
  HAS_SUBSCRIBERS_FN,
  CHANNEL_FN,
  SUBSCRIBE_FN,
  UNSUBSCRIBE_FN,
  TRACING_CHANNEL_FN,
  CHANNEL_CTOR,
  TRACING_CHANNEL_CTOR,
)

// Installs the Node `diagnostics_channel` module into the intrinsic bindings.
@Intrinsic internal class NodeDiagnosticsChannelModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDiagnosticsChannel.create() }
  internal fun provide(): DiagnosticsChannelAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DIAGNOSTICS_CHANNEL)) { singleton }
  }
}

/**
 * # Node API: `diagnostics_channel`
 */
internal class NodeDiagnosticsChannel private constructor () : ReadOnlyProxyObject, DiagnosticsChannelAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeDiagnosticsChannel = NodeDiagnosticsChannel()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = DIAGNOSTICS_CHANNEL_PROPS
  override fun getMember(key: String?): Any? = when (key) {
    HAS_SUBSCRIBERS_FN,
    CHANNEL_FN,
    SUBSCRIBE_FN,
    UNSUBSCRIBE_FN,
    TRACING_CHANNEL_FN -> ProxyExecutable { TODO("Function '$key' is not implemented") }
    CHANNEL_CTOR,
    TRACING_CHANNEL_CTOR -> ProxyInstantiable { TODO("Ctor '$key' is not implemented") }  // @TODO implement module
    else -> null
  }
}
