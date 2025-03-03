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
package elide.runtime.gvm.builtins.ai

import io.micronaut.context.annotation.Factory
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import jakarta.inject.Singleton
import elide.runtime.core.lib.NativeLibraries
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.intrinsics.GuestIntrinsic

// Symbol where the internal module implementation is installed.
private const val LOCAL_AI_LIBRARY: String = "elideai"

// Symbol where the internal module implementation is installed.
private const val AI_LLM_MODULE_NAME: String = "llm"

// Symbol where the AI LLM module is installed.
private const val AI_LLM_MODULE_SYMBOL: String = "elide_ai_$AI_LLM_MODULE_NAME"

@Intrinsic
@Factory internal class ElideAiModule : AbstractNodeBuiltinModule() {
  @Singleton fun provide(): AiLlmTools = AiLlmTools.obtain()

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[AI_LLM_MODULE_SYMBOL.asJsSymbol()] = provide()
  }

  init {
    ModuleRegistry.deferred(ModuleInfo.of(AI_LLM_MODULE_NAME)) { provide() }
  }
}

public interface AiLlmAPI {
  // Nothing yet.
}

internal class AiLlmTools private constructor () : AiLlmAPI, ProxyObject {
  internal companion object {
    @Volatile private var libReady: Boolean = false

    init {
      NativeLibraries.resolve(LOCAL_AI_LIBRARY) {
        libReady = true
      }
    }

    private val SINGLETON = AiLlmTools()
    @JvmStatic internal fun obtain(): AiLlmTools = SINGLETON.also {
      if (!libReady) {
        error("AI LLM module is not ready.")
      }
    }
  }

  override fun getMemberKeys(): Array<String> = arrayOf("sample")
  override fun removeMember(key: String?): Boolean = false
  override fun hasMember(key: String): Boolean = key in memberKeys
  override fun putMember(key: String?, value: Value?) = Unit

  override fun getMember(key: String): Any? = when (key) {
    "sample" -> ProxyExecutable { "hello from module" }
    else -> null
  }
}
