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
package elide.runtime.node.querystring

import org.graalvm.polyglot.Value
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.QuerystringAPI
import elide.runtime.lang.javascript.NodeModuleName

private val moduleMembers = arrayOf(
  "decode",
  "encode",
  "escape",
  "parse",
  "stringify",
  "unescape"
)
// Installs the Node query-string module into the intrinsic bindings.
@Intrinsic internal class NodeQuerystringModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeQuerystring.create() }

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.QUERYSTRING)) { singleton }
  }
}

/**
 * # Node API: `querystring`
 */
internal class NodeQuerystring : ReadOnlyProxyObject, QuerystringAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeQuerystring = NodeQuerystring()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = moduleMembers
  override fun getMember(key: String?): Any? = null
  override fun decode(
    str: String,
    sep: String?,
    eq: String?,
    options: Value?
  ): Value {
    TODO("Not yet implemented")
  }

  override fun encode(
    obj: Value,
    sep: String?,
    eq: String?,
    options: Value?
  ): String {
    TODO("Not yet implemented")
  }

  override fun escape(str: String): String {
    TODO("Not yet implemented")
  }

  override fun parse(
    str: String,
    sep: String?,
    eq: String?,
    options: Value?
  ): Value {
    TODO("Not yet implemented")
  }

  override fun stringify(
    obj: Value,
    sep: String?,
    eq: String?,
    options: Value?
  ): String {
    TODO("Not yet implemented")
  }

  override fun unescape(str: String): String {
    TODO("Not yet implemented")
  }
}
