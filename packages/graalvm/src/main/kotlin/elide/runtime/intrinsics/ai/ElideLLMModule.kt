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
package elide.runtime.intrinsics.ai

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.annotations.Singleton
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic

// Module names.
private const val ELIDE_AI_LLM_MODULE_NAME = "llm"
private const val ELIDE_AI_LLM_LOCAL_MODULE_NAME = "$ELIDE_AI_LLM_MODULE_NAME/local"
private const val ELIDE_AI_LLM_REMOTE_MODULE_NAME = "$ELIDE_AI_LLM_MODULE_NAME/remote"
private const val ELIDE_AI_MODULE_VERSION = "v1"
private const val ELIDE_AI_MODULE_VERSION_PROP = "version"

// All uniform module properties.
private val moduleSurface = arrayOf(
  ELIDE_AI_MODULE_VERSION_PROP,
)

/**
 * ## Elide LLM Builtins
 *
 * Implements the user-visible module surface for Elide's built-in LLM module(s). This includes the top-level module,
 * which is used to obtain an [LLMEngine] instance, as well as the `local` and `remote` modules which live underneath
 * the top-level module and provide access to specific types of engines.
 *
 * All such modules (except the top-level module) comply with the exact same API. The top-level module differs in that
 * methods are provided to resolve an engine, through default or configurable means. Once an engine is resolved, the API
 * is the same across implementation modules.
 *
 * ## Usage from JavaScript
 *
 * For example:
 * ```javascript
 * import llm from "elide:llm"
 * // ...
 * await llm.something(/* ... */);  // uses the default/configured engine
 * ```
 *
 * While importing a specific module chooses an API directly:
 * ```javascript
 * import llm from "elide:llm/local"
 * // ...
 * await llm.something(/* ... */);  // uses the local engine only
 * ```
 */
@Singleton
@Intrinsic public class ElideLLMModule (private val guestExec: GuestExecutorProvider) : AbstractJsIntrinsic() {
  // Top-level module at `elide:llm`.
  private val top by lazy { ElideLLMImpl() }

  // Local module at `elide:llm/local`.
  private val local by lazy { ElideLocalLLMImpl(guestExec) }

  // Remote module at `elide:llm/remote`.
  private val remote by lazy { ElideRemoteLLMImpl(guestExec) }

  // Expose the top-level module.
  internal fun module(): ElideLLMImpl = top

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(ELIDE_AI_LLM_MODULE_NAME)) { top }
    ModuleRegistry.deferred(ModuleInfo.of(ELIDE_AI_LLM_LOCAL_MODULE_NAME)) { local }
    ModuleRegistry.deferred(ModuleInfo.of(ELIDE_AI_LLM_REMOTE_MODULE_NAME)) { remote }
  }
}

// Base LLM implementation stuff.
internal sealed class BaseLLMImpl : LLMAPI, ReadOnlyProxyObject {
  override fun getMemberKeys(): Array<String> = moduleSurface

  override fun version(): String = ELIDE_AI_MODULE_VERSION

  override fun getMember(key: String?): Any? = when (key) {
    ELIDE_AI_MODULE_VERSION_PROP -> ProxyExecutable { version() }
    else -> null
  }
}

// Implements the main Elide LLM module.
internal class ElideLLMImpl : BaseLLMImpl()

// Implements the local LLM module.
internal class ElideLocalLLMImpl internal constructor (private val guestExec: GuestExecutorProvider) : BaseLLMImpl()

// Implements the remote LLM module.
internal class ElideRemoteLLMImpl internal constructor (private val guestExec: GuestExecutorProvider) : BaseLLMImpl()
