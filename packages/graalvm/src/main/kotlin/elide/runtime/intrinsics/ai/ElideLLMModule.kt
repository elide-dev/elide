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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.lang.ref.WeakReference
import java.net.URI
import java.nio.file.Paths
import elide.annotations.Singleton
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.lang.javascript.SyntheticJSModule
import elide.runtime.localai.Parameters
import elide.runtime.localai.Model
import elide.vm.annotations.Polyglot

// Module names.
private const val ELIDE_AI_LLM_MODULE_NAME = "llm"
private const val ELIDE_AI_LLM_LOCAL_MODULE = "local"
private const val ELIDE_AI_LLM_REMOTE_MODULE = "remote"
private const val ELIDE_AI_LLM_LOCAL_MODULE_NAME = "$ELIDE_AI_LLM_MODULE_NAME/$ELIDE_AI_LLM_LOCAL_MODULE"
private const val ELIDE_AI_LLM_REMOTE_MODULE_NAME = "$ELIDE_AI_LLM_MODULE_NAME/$ELIDE_AI_LLM_REMOTE_MODULE"
private const val ELIDE_AI_MODULE_VERSION = "v1"
private const val ELIDE_AI_MODULE_VERSION_PROP = "version"

// Properties and methods.
private const val ELIDE_AI_LLM_PARAMS_FN = "params"
private const val ELIDE_AI_LLM_MODEL_FN = "localModel"
private const val ELIDE_AI_LLM_HFACE_FN = "huggingface"

// Configuration parameter names.
private const val PARAMS_VERBOSE = "verbose"
private const val PARAMS_GPU_LAYERS = "gpuLayers"
private const val PARAMS_DISABLE_GPU = "disableGpu"
private const val PARAMS_CONTEXT_SIZE = "contextSize"
private const val PARAMS_ALLOW_DOWNLOAD = "allowDownload"
private const val PARAMS_THREAD_COUNT = "threadCount"
private const val PARAMS_THREAD_BATCH_COUNT = "threadBatchCount"
private const val PARAMS_HFACE_TOKEN = "huggingFaceToken"

// All uniform module properties.
private val moduleSurface = arrayOf(
  ELIDE_AI_MODULE_VERSION_PROP,
  ELIDE_AI_LLM_PARAMS_FN,
)

// Properties that only exist on the top-level module.
private val topLevelProps = arrayOf(
  ELIDE_AI_LLM_MODEL_FN,
  ELIDE_AI_LLM_HFACE_FN,
  ELIDE_AI_LLM_LOCAL_MODULE,
  ELIDE_AI_LLM_REMOTE_MODULE,
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
@Intrinsic public class ElideLLMModule (private val guestExec: GuestExecutorProvider) :
  SyntheticJSModule<LLMAPI>,
  AbstractJsIntrinsic() {
  // Top-level module at `elide:llm`.
  private val top by lazy { ElideLLMImpl(this) }

  // Local module at `elide:llm/local`.
  private val local by lazy { ElideLocalLLMImpl(guestExec) }

  // Remote module at `elide:llm/remote`.
  private val remote by lazy { ElideRemoteLLMImpl(guestExec) }

  // Access to local/remote modules from top-level module.
  internal fun localLlm(): LLMAPI = local
  internal fun remoteLlm(): LLMAPI = remote

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(ELIDE_AI_LLM_MODULE_NAME)) { top }
    ModuleRegistry.deferred(ModuleInfo.of(ELIDE_AI_LLM_LOCAL_MODULE_NAME)) { local }
    ModuleRegistry.deferred(ModuleInfo.of(ELIDE_AI_LLM_REMOTE_MODULE_NAME)) { remote }
  }

  // Expose the top-level module.
  override fun provide(): LLMAPI = top
}

// Decode parameters from a guest value.
private fun Parameters.Companion.from(value: Value?): Parameters {
  return when {
    value == null || value.isNull -> defaults()
    value.hasMembers() -> create(
      verbose = value.getMember(PARAMS_VERBOSE)?.asBoolean() == true,
      gpuLayers = (value.getMember(PARAMS_GPU_LAYERS)?.asInt() ?: 0).toUInt(),
      disableGpu = value.getMember(PARAMS_DISABLE_GPU)?.asBoolean() == true,
      contextSize = (value.getMember(PARAMS_CONTEXT_SIZE)?.asInt() ?: 0).toUInt(),
      allowDownload = value.getMember(PARAMS_ALLOW_DOWNLOAD)?.asBoolean() == true,
      threadCount = (value.getMember(PARAMS_THREAD_COUNT)?.asInt() ?: 0).toUInt(),
      threadBatchCount = (value.getMember(PARAMS_THREAD_BATCH_COUNT)?.asInt() ?: 0).toUInt(),
      huggingFaceToken = value.getMember(PARAMS_HFACE_TOKEN)?.asString(),
    )
    value.hasHashEntries() -> create(
      verbose = value.getHashValue(PARAMS_VERBOSE)?.asBoolean() == true,
      gpuLayers = (value.getHashValue(PARAMS_GPU_LAYERS)?.asInt() ?: 0).toUInt(),
      disableGpu = value.getHashValue(PARAMS_DISABLE_GPU)?.asBoolean() == true,
      contextSize = (value.getHashValue(PARAMS_CONTEXT_SIZE)?.asInt() ?: 0).toUInt(),
      allowDownload = value.getHashValue(PARAMS_ALLOW_DOWNLOAD)?.asBoolean() == true,
      threadCount = (value.getHashValue(PARAMS_THREAD_COUNT)?.asInt() ?: 0).toUInt(),
      threadBatchCount = (value.getHashValue(PARAMS_THREAD_BATCH_COUNT)?.asInt() ?: 0).toUInt(),
      huggingFaceToken = value.getHashValue(PARAMS_HFACE_TOKEN)?.asString(),
    )
    else -> throw JsError.typeError(
      "Cannot decode value of type ${value.javaClass.name} as parameters; please pass an object instead",
    )
  }
}

// Decode a local model spec info from a guest value.
private fun Model.Companion.local(value: Value?): Model {
  return when {
    value == null || value.isNull -> throw JsError.typeError(
      "Cannot decode null or undefined value as a local model spec; please path a path instead",
    )

    value.isString -> atPath(value.asString().let {
      if (it.isEmpty()) {
        throw JsError.typeError("Cannot decode empty string as a local model spec")
      } else {
        Paths.get(URI.create(it))
      }
    })

    else -> throw JsError.typeError(
      "Cannot decode value of type ${value.javaClass.name} as a local model spec; please pass a path instead",
    )
  }
}

// Decode a remote model spec info from a guest value.
private fun Model.Companion.huggingface(value: Value): Model {
  return when {
    // if it's an object with members, prefer that
    value.hasMembers() -> huggingface(
      repo = value.getMember("repo")?.asString() ?: "",
      name = value.getMember("model")?.asString() ?: "",
    )

    // fallback to hash members
    value.hasHashEntries() -> huggingface(
      repo = value.getHashValue("repo")?.asString() ?: "",
      name = value.getHashValue("model")?.asString() ?: "",
    )

    // maybe it's a two-item array?
    value.hasArrayElements() -> huggingface(
      repo = value.getArrayElement(0)?.asString() ?: "",
      name = value.getArrayElement(1)?.asString() ?: "",
    )

    // maybe it's a string and the model name is implied?
    value.isString -> huggingface(
      repo = value.asString(),
      name = value.asString(),
    )

    else -> throw JsError.typeError(
      "Unrecognized type for HuggingFace model: ${value.javaClass.name} ($value)",
    )
  }
}

// Base LLM implementation stuff.
internal sealed class BaseLLMImpl : LLMAPI, ReadOnlyProxyObject {
  override fun getMemberKeys(): Array<String> = moduleSurface

  @Polyglot override fun version(): String = ELIDE_AI_MODULE_VERSION
  @Polyglot override fun params(options: Value?): Parameters = Parameters.from(options)
  @Polyglot override fun localModel(options: Value): Model = Model.local(options)
  @Polyglot override fun huggingface(options: Value): Model = Model.huggingface(options)

  override fun getMember(key: String?): Any? = when (key) {
    ELIDE_AI_MODULE_VERSION_PROP -> ProxyExecutable { version() }
    ELIDE_AI_LLM_PARAMS_FN -> ProxyExecutable { params(it.firstOrNull()) }
    ELIDE_AI_LLM_MODEL_FN -> ProxyExecutable { localModel(it.first()) }
    ELIDE_AI_LLM_HFACE_FN -> ProxyExecutable { huggingface(it.first()) }
    else -> null
  }
}

// Implements the main Elide LLM module.
internal class ElideLLMImpl internal constructor (private val mod: WeakReference<ElideLLMModule>) : BaseLLMImpl() {
  internal constructor (mod: ElideLLMModule) : this(WeakReference(mod))

  override fun getMemberKeys(): Array<String> = topLevelProps.plus(moduleSurface)

  override fun getMember(key: String?): Any? = when (key) {
    ELIDE_AI_LLM_LOCAL_MODULE -> mod.get()?.localLlm()
    ELIDE_AI_LLM_REMOTE_MODULE -> mod.get()?.remoteLlm()
    else -> super.getMember(key)
  }
}

// Implements the local LLM module.
internal class ElideLocalLLMImpl internal constructor (private val guestExec: GuestExecutorProvider) : BaseLLMImpl()

// Implements the remote LLM module.
internal class ElideRemoteLLMImpl internal constructor (private val guestExec: GuestExecutorProvider) : BaseLLMImpl()
