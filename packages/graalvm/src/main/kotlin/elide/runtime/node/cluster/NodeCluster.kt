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
package elide.runtime.node.cluster

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ClusterAPI
import elide.runtime.lang.javascript.NodeModuleName

// Installs the Node cluster module into the intrinsic bindings.
@Intrinsic
internal class NodeClusterModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeCluster.create() }
  internal fun provide(): NodeCluster = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.CLUSTER)) { singleton }
  }
}

/**
 * # Node API: `cluster`
 */
internal class NodeCluster private constructor () : ReadOnlyProxyObject, ClusterAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeCluster = NodeCluster()
  }

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
