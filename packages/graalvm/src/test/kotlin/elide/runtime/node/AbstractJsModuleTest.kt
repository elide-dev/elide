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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.node

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.gvm.internals.intrinsics.js.base64.Base64Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.console.ConsoleIntrinsic
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.Symbol
import elide.runtime.lang.javascript.JavaScriptLang
import elide.runtime.node.asserts.NodeAssertModule
import elide.runtime.node.asserts.NodeAssertStrictModule
import elide.runtime.node.buffer.NodeBufferModule
import elide.runtime.plugins.env.EnvConfig
import elide.runtime.plugins.env.environment
import elide.runtime.plugins.js.JavaScript
import elide.runtime.plugins.js.javascript
import elide.runtime.plugins.vfs.VfsConfig
import elide.runtime.plugins.vfs.vfs

internal abstract class AbstractJsModuleTest<T : GuestIntrinsic> : AbstractJsIntrinsicTest<T>() {
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
      configure(JavaScript)
      environment { configureEnvironment() }
      vfs { configureVfs() }
    }
  }

  private fun beforeExec(
    bind: Boolean,
    bindAssert: Boolean = true,
    bindConsole: Boolean = true,
    bindBase64: Boolean = true,
    bindBuffer: Boolean = true,
  ) {
    // install bindings under test, if directed
    JavaScriptLang.initialize()
    val target = polyglotContext.bindings(JavaScript)

    // prep intrinsic bindings under test
    val langBindings = if (bind) {
      val group = HashMap<Symbol, Any>()
      val binding = GuestIntrinsic.MutableIntrinsicBindings.Factory.wrap(group)
      provide().install(binding)
      if (bindBase64 && !group.any { it.key.symbol.contains("Base64") }) {
        Base64Intrinsic().install(binding)
      }
      if (bindConsole && !group.any { it.key.symbol.contains("console") }) {
        ConsoleIntrinsic().install(binding)
      }
      if (bindAssert && !group.any { it.key.symbol.contains("assert") }) {
        //NodeAssertModule().install(binding)
        NodeAssertStrictModule().install(binding)
      }
      if (bindBuffer && !group.any { it.key.symbol.contains("buffer") }) {
        NodeBufferModule().install(binding)
      }
      group
    } else {
      emptyMap()
    }

    // prep internal bindings
    val internalBindings: MutableMap<String, Any?> = mutableMapOf()
    langBindings.forEach {
      if (it.key.isInternal) {
        internalBindings[it.key.internalSymbol] = it.value
      }
    }

    // install bindings under test (public only so far)
    for (binding in langBindings) {
      if (binding.key.isInternal) {
        continue
      }
      target.putMember(binding.key.symbol, binding.value)
    }

    // shim primordials
    val primordialsProxy = object : ProxyObject, ProxyHashMap {
      override fun getMemberKeys(): Array<String> = internalBindings.keys.toTypedArray()
      override fun hasMember(key: String?): Boolean = key != null && key in internalBindings
      override fun hasHashEntry(key: Value?): Boolean = key != null && key.asString() in internalBindings
      override fun getHashSize(): Long = internalBindings.size.toLong()
      override fun getHashEntriesIterator(): Any = internalBindings.entries.iterator()
      override fun putMember(key: String?, value: Value?) {
        // no-op
      }

      override fun putHashEntry(key: Value?, value: Value?) {
        // no-op
      }

      override fun getMember(key: String?): Any? = when (key) {
        null -> null
        else -> internalBindings[key]
      }

      override fun getHashValue(key: Value?): Any? = getMember(key?.asString())
    }

    target.putMember("primordials", primordialsProxy)
  }

  protected fun require(module: String = moduleName, bind: Boolean = true): Value {
    beforeExec(bind)
    return polyglotContext.javascript(
      // language=js
      """require("$module");""",
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
