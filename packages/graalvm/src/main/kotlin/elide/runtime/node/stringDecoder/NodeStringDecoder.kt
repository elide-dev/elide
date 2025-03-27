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
package elide.runtime.node.stringDecoder

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.StringDecoderAPI
import elide.runtime.lang.javascript.NodeModuleName

// Properties and methods.
private const val STRING_DECODER_CONSTRUCTOR_FN = "StringDecoder"

// All properties on the Node string decoder module.
private val STRING_DECODER_PROPS = arrayOf(
  STRING_DECODER_CONSTRUCTOR_FN,
)

// Installs the Node string decoder module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeStringDecoderModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): StringDecoderAPI = NodeStringDecoder.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.STRING_DECODER)) { provide() }
  }
}

/**
 * # Node API: `string_decoder`
 */
internal class NodeStringDecoder : ReadOnlyProxyObject, StringDecoderAPI {
  //

  override fun getMemberKeys(): Array<String> = STRING_DECODER_PROPS

  override fun getMember(key: String?): Any? = when (key) {
    STRING_DECODER_CONSTRUCTOR_FN -> Companion
    else -> null
  }

  internal companion object {
    private val SINGLETON by lazy { NodeStringDecoder() }
    fun obtain(): NodeStringDecoder = SINGLETON
  }
}
