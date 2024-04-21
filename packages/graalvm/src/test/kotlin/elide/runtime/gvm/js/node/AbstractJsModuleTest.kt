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

@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.js.node

import org.graalvm.polyglot.Value
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.plugins.env.EnvConfig
import elide.runtime.plugins.env.environment
import elide.runtime.plugins.js.JavaScript
import elide.runtime.plugins.js.javascript
import elide.runtime.plugins.vfs.VfsConfig
import elide.runtime.plugins.vfs.vfs

internal abstract class AbstractJsModuleTest<T: GuestIntrinsic> : AbstractJsIntrinsicTest<T>() {
  abstract val moduleName: String

  open fun EnvConfig.configureEnvironment() {
    // nothing
  }

  open fun VfsConfig.configureVfs() {
    // nothing
  }

  protected val polyglotEngine by lazy {
    PolyglotEngine {
      vfs { configureVfs() }
      environment { configureEnvironment() }
      install(JavaScript)
    }.acquire()
  }

  open val supportsEsm: Boolean get() = true
  open val supportsCjs: Boolean get() = true

  protected fun require(module: String = moduleName): Value {
    return polyglotEngine.javascript(
      // language=js
      """require("$module");"""
    )
  }

  protected fun import(module: String = moduleName): Value {
    val modname = module.split(":").last()
    return polyglotEngine.javascript(
      // language=js
      """
        import $modname from "$modname";
        $modname;
      """.trimIndent(),
      esm = true,
    )
  }

  protected fun load(module: String = moduleName): Value = import(module)
}
