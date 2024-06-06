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
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.gvm.internals.intrinsics.js.console.ConsoleIntrinsic
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.Symbol
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

  @OptIn(DelicateElideApi::class)
  protected val polyglotContext by lazy {
    engine.acquire()
  }

  open val supportsEsm: Boolean get() = true
  open val supportsCjs: Boolean get() = true

  override fun configureEngine(config: PolyglotEngineConfiguration) {
    config.apply {
      install(JavaScript)
      environment { configureEnvironment() }
      vfs { configureVfs() }
    }
  }

  private fun beforeExec(bind: Boolean) {
    // install bindings under test, if directed
    val target = polyglotContext.bindings(JavaScript)

    // prep intrinsic bindings under test
    val bindings = if (bind) {
      val group = HashMap<Symbol, Any>()
      val binding = GuestIntrinsic.MutableIntrinsicBindings.Factory.wrap(group)
      ConsoleIntrinsic().install(binding)
      provide().install(binding)
      group
    } else {
      emptyMap()
    }

    bindings.forEach {
      target.putMember(it.key.symbol, it.value)
    }
  }

  protected fun require(module: String = moduleName, bind: Boolean = true): Value {
    beforeExec(bind)
    return polyglotContext.javascript(
      // language=js
      """require("$module");"""
    )
  }

  protected fun import(module: String = moduleName, bind: Boolean = true): Value {
    val modname = module.split(":").last()
    val modsymbol = if ("/" in modname) modname.split("/").first() else modname
    beforeExec(bind)
    return polyglotContext.javascript(
      // language=js
      """
        import $modsymbol from "$modname";
        $modsymbol;
      """.trimIndent(),
      esm = true,
    )
  }

  protected fun load(module: String = moduleName, bind: Boolean = true): Value = import(module, bind)
}
