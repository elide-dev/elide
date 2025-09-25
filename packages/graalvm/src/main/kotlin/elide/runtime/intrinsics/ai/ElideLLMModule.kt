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

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import elide.annotations.Singleton
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.runtime.lang.javascript.SyntheticJSModule
import elide.runtime.localai.InferenceResults
import elide.runtime.localai.Parameters
import elide.runtime.localai.Model
import elide.runtime.localai.NativeLocalAi
import elide.runtime.node.events.EventAwareProxy
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
private const val ELIDE_AI_LLM_INFER_SYNC_FN = "inferSync"
private const val ELIDE_AI_LLM_INFER_FN = "infer"

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
  ELIDE_AI_LLM_INFER_SYNC_FN,
  ELIDE_AI_LLM_INFER_FN,
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
  internal fun localLlm(): ElideLocalLLMImpl = local
  internal fun remoteLlm(): ElideRemoteLLMImpl = remote

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

    value.isHostObject -> value.`as`<Parameters>(Parameters::class.java)

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
      name = value.getMember("model")?.asString() ?: value.getMember("name")?.asString() ?: "",
    )

    // fallback to hash members
    value.hasHashEntries() -> huggingface(
      repo = value.getHashValue("repo")?.asString() ?: "",
      name = value.getMember("model")?.asString() ?: value.getMember("name")?.asString() ?: "",
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
  protected val execPool: ListeningExecutorService = MoreExecutors.listeningDecorator(
    Executors.newVirtualThreadPerTaskExecutor()
  )

  override fun getMemberKeys(): Array<String> = moduleSurface

  // Unpack guest model info.
  private fun decodeModel(modelGuest: Value): Model = when {
    modelGuest.isHostObject -> modelGuest.`as`<Model>(Model::class.java)
    else -> throw JsError.typeError("Please provide a valid model spec via `model` or `huggingface`")
  }

  // Unpack guest input.
  private fun decodeInput(inputGuest: Value): PromptInput = when {
    inputGuest.isString -> PromptInput.of(inputGuest.asString())
    inputGuest.isHostObject -> inputGuest.`as`<PromptInput>(PromptInput::class.java)
    else -> throw JsError.typeError("Please provide a valid prompt input (string, function, or via `prompt`)")
  }

  // Unpack guest args into parameters, model, and input.
  private fun prepareInferenceArgs(args: Array<Value>): Triple<Parameters, Model, PromptInput> {
    return when (args.size) {
      // `model, input`
      2 -> {
        val params = Parameters.defaults()
        val modelGuest = args[0]
        val inputGuest = args[1]
        val model = decodeModel(modelGuest)
        val input = decodeInput(inputGuest)
        Triple(params, model, input)
      }

      // `params, model, input`
      3 -> {
        val paramsGuest = args[0]
        val modelGuest = args[1]
        val inputGuest = args[2]
        val params = Parameters.from(paramsGuest)
        val model = decodeModel(modelGuest)
        val input = decodeInput(inputGuest)
        Triple(params, model, input)
      }

      else -> throw JsError.typeError(
        "Invalid number of arguments to `infer` (expected 2 or 3, got ${args.size})",
      )
    }
  }

  @Polyglot override fun version(): String = ELIDE_AI_MODULE_VERSION
  @Polyglot override fun params(options: Value?): Parameters = Parameters.from(options)
  @Polyglot override fun localModel(options: Value): Model = Model.local(options)
  @Polyglot override fun huggingface(options: Value): Model = Model.huggingface(options)

  @Polyglot override fun infer(args: Array<Value>): JsPromise<String> {
    val (params, model, input) = prepareInferenceArgs(args)
    return infer(params, model, input)
  }

  @Polyglot override fun inferSync(args: Array<Value>): String {
    val (params, model, input) = prepareInferenceArgs(args)
    return inferSync(params, model, input)
  }

  override fun getMember(key: String?): Any? = when (key) {
    ELIDE_AI_MODULE_VERSION_PROP -> ProxyExecutable { version() }
    ELIDE_AI_LLM_PARAMS_FN -> ProxyExecutable { params(it.firstOrNull()) }
    ELIDE_AI_LLM_MODEL_FN -> ProxyExecutable { localModel(it.first()) }
    ELIDE_AI_LLM_HFACE_FN -> ProxyExecutable { huggingface(it.first()) }
    ELIDE_AI_LLM_INFER_SYNC_FN -> ProxyExecutable { inferSync(it) }
    ELIDE_AI_LLM_INFER_FN -> ProxyExecutable { infer(it) }
    else -> null
  }
}

// Implements the main Elide LLM module.
internal class ElideLLMImpl internal constructor (private val mod: ElideLLMModule) : BaseLLMImpl() {
  // Resolve the configured/default implementation of the LLM API (local or remote).
  private fun resolveImpl(): BaseLLMImpl = mod.localLlm() // TODO: resolve this based on config

  @Polyglot override fun infer(params: Parameters, model: Model, input: PromptInput): JsPromise<String> {
    return resolveImpl().infer(params, model, input)
  }

  @Polyglot override fun inferSync(params: Parameters, model: Model, input: PromptInput): String {
    return resolveImpl().inferSync(params, model, input)
  }

  override fun getMemberKeys(): Array<String> = topLevelProps.plus(moduleSurface)

  override fun getMember(key: String?): Any? = when (key) {
    ELIDE_AI_LLM_LOCAL_MODULE -> mod.localLlm()
    ELIDE_AI_LLM_REMOTE_MODULE -> mod.remoteLlm()
    else -> super.getMember(key)
  }
}

// Implements an async inference operation handle.
internal class InferenceOperationImpl internal constructor (
  private val task: ListenableFuture<InferenceResults>,
  private val guestExec: GuestExecutorProvider,
  private val events: EventAwareProxy = EventAwareProxy.create(),
) : InferenceOperation, EventTarget by events {
  // Provide as a JS promise.
  @get:Polyglot override val promise: JsPromise<InferenceResults> get() = JsPromise.wrap(task)

  override fun putMember(key: String?, value: Value?) {
    if (key != null) {
      events.putMember(key, value)
    }
  }

  override fun removeMember(key: String?): Boolean = if (key == null) false else events.removeMember(key)

  override fun getMember(key: String?): Any? {
    if (key == null) return null
    return super.getMember(key) ?: events.getMember(key)
  }

  override fun getMemberKeys(): Array<String> = memberKeys.plus(events.memberKeys)
}

// Implements the local LLM module.
internal class ElideLocalLLMImpl internal constructor (private val guestExec: GuestExecutorProvider) : BaseLLMImpl() {
  @Polyglot override fun infer(params: Parameters, model: Model, input: PromptInput): JsPromise<String> {
    return try {
      // @TODO disgusting
      JsPromise.wrap(execPool.submit<String> {
        when (val result = NativeLocalAi.inferSync(params, model, input.render())) {
          is InferenceResults.Error -> throw JsError.of(
            "Inference failed: ${result.message}",
          )

          is InferenceResults.Sync -> result.value.joinToString()
          else -> error("Unexpected inference result")
        }
      })
    } catch (err: ExecutionException) {
      throw JsError.of(
        "Inference failed: ${err.message}",
        cause = err.cause,
      )
    }
  }

  @Suppress("TooGenericExceptionCaught")
  @Polyglot override fun inferSync(params: Parameters, model: Model, input: PromptInput): String {
    return try {
      NativeLocalAi.inferSync(params, model, input.render())
    } catch (rxe: RuntimeException) {
      throw JsError.of(
        "Inference failed: ${rxe.message}",
        cause = rxe,
      )
    }.let { results ->
      when (results.success) {
        false -> throw JsError.of(
          "Inference failed: ${(results as InferenceResults.Error).message}",
        )
        true -> when (results) {
          is InferenceResults.Sync -> results.sequence.toList().joinToString()
          else -> error("Invalid results for synchronous inference: $results")
        }
      }
    }
  }
}

// Implements the remote LLM module.
internal class ElideRemoteLLMImpl internal constructor (private val guestExec: GuestExecutorProvider) : BaseLLMImpl() {
  @Polyglot override fun infer(params: Parameters, model: Model, input: PromptInput): JsPromise<String> {
    TODO("Not yet implemented: `ElideRemoteLLM.infer`")
  }

  @Polyglot override fun inferSync(params: Parameters, model: Model, input: PromptInput): String {
    TODO("Not yet implemented: `ElideRemoteLLM.inferSync`")
  }
}
