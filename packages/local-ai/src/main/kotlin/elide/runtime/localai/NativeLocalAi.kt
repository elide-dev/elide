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
package elide.runtime.localai

import org.graalvm.nativeimage.ImageInfo
import java.lang.AutoCloseable
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.random.Random
import elide.runtime.core.lib.NativeLibraries

/**
 * ## Native Local AI
 *
 * Implements a native bridge over JNI to llama.cpp, which supports Elide's local AI features; these symbols are
 * ultimately mounted for guest use via surfaces like the `elide:ai` built-in JavaScript module.
 *
 * Inference is performed using local AI models, which either reside on-disk ahead of time, or models which are fetched
 * from HuggingFace and cached locally for use. The local AI layer is responsible for managing these models, performing
 * downloads, executing inference, and providing results back to the caller.
 *
 * ## Usage
 *
 * The AI layers is initialized in all cases, and then inference can occur through several methods:
 * - [inferSync] is the simplest case; it waits until inference fully completes, and returns string-like results.
 *
 * - [infer] is much more efficient; it returns a [InferenceResults.Suspending] object immediately, and inference takes
 *   place in the background through a hot stream.
 *
 * Results are made uniform via the [InferenceResults] API, which is sealed to include:
 *
 * - [InferenceResults.Error] for failed inference operations.
 * - [InferenceResults.Streamed] for streamed results (i.e. via Java [Stream]s).
 * - [InferenceResults.Suspending] for suspending results (i.e. via Kotlin [Flow]s).
 *
 * ## Native Code
 *
 * The counterpart native code for this object is available in the `local-ai` crate. That crate provides a static API
 * surface which is consumed by this object's external (JNI) methods.
 */
public object NativeLocalAi : AutoCloseable {
  // Local AI library to load.
  private const val LIB_LOCALAI = "local_ai"

  // Whether native libraries have loaded.
  @Volatile private var libsLoaded = false

  // Whether the local AI layer has initialized yet.
  @Volatile private var initialized = false

  // Clean up local AI resources.
  override fun close() {
    cleanup().also {
      initialized = false
    }
  }

  // Determine a default path to store a model at.
  @JvmStatic private fun defaultModelPath(model: Model.HuggingFaceModel): Path =
    Files.createTempDirectory("elide-localai-${model.hashCode()}")

  // Compute the prompt length + desired output length or fallback to default.
  @JvmStatic private fun computePromptLength(params: Parameters, prompt: String): Int {
    when (val explicitLength = params.length?.toInt()) {
      null -> {
        val promptLength = prompt.split(" ").size
        return if (promptLength > 0) promptLength + (params.contextSize.toInt() / 2) else params.contextSize.toInt()
      }
      else -> return explicitLength
    }
  }

  // Resolve model info from parameters; designed to be extracted as `(huggingFace, path)`.
  private fun resolveModelInfo(model: Model): Pair<Pair<String, String>, Path?> {
    return when (model) {
      is Model.OnDiskModel -> null to model.path
      is Model.HuggingFaceModel ->
        (model.repo to model.name) to (model.path ?: defaultModelPath(model)).toAbsolutePath()
    }.let { (l, r) ->
      resolveHuggingFaceModel(l) to r
    }
  }

  // Resolve HuggingFace model info or fail gracefully; designed to be extracted as `(repo, name)`.
  private fun resolveHuggingFaceModel(huggingFace: Pair<String, String>?): Pair<String, String> {
    return when (huggingFace) {
      null -> "" to ""
      else -> huggingFace
    }
  }

  /**
   * ### Inference Chunk Callback Implementation
   *
   * Exposed for use via JNI.
   */
  public class InferenceChunkCallbackImpl internal constructor (
    internal val coroutineContext: CoroutineContext,
    internal val flow: FlowCollector<String>,
  ) : InferenceChunkCallback, AutoCloseable {
    private val buf: StringBuilder = StringBuilder()

    override fun onChunk(chunk: String) {
      buf.append(chunk)
    }

    internal suspend fun complete(id: Int) {
      InferenceCallbackRegistry.notifyComplete(id)
      flow.emit(buf.toString())
    }

    internal fun activate(id: Int) {
      InferenceCallbackRegistry.register(id, this)
    }

    override fun close() {
      // nothing at this time
    }
  }

  /**
   * ### Load
   *
   * Load native libraries needed for local AI features; this is a no-op if the libraries are already loaded, or if the
   * user is running in a context where such libraries are implicitly available.
   */
  @JvmStatic public fun load() {
    if (!ImageInfo.inImageCode()) {
      NativeLibraries.resolve(LIB_LOCALAI) {
        libsLoaded = it
      }
    } else {
      libsLoaded = true
    }
  }

  /**
   * ### Ensure Available
   *
   * Load the native libraries needed for local AI features (if needed), and perform initialization of the local AI
   * native layer. If initialization has already taken place, this method is a no-op.
   */
  @Suppress("KotlinConstantConditions")
  @JvmStatic @Synchronized public fun ensureAvailable() {
    if (!libsLoaded) load()
    if (!initialized) initialize(Parameters.DEFAULT_VERBOSE).also { initialized = it }
  }

  /**
   * ### Inference (Synchronous)
   *
   * Perform a local AI inference operation with the given parameters; if the local AI layer is not available, cannot
   * load or initialize, or fails to perform inference, an exception is thrown.
   *
   * @param params The parameters to use for local AI inference.
   * @param prompt Primary input prompt to invoke the model with.
   * @return The results of the inference operation.
   */
  @JvmStatic public fun inferSync(params: Parameters, model: Model, prompt: String): InferenceResults {
    ensureAvailable()
    val (huggingFace, path) = resolveModelInfo(model)
    val (huggingFaceRepo, huggingFaceName) = huggingFace
    return when (val out = inferSync(
      verbose = params.verbose,
      disableGpu = params.disableGpu,
      gpuLayers = params.gpuLayers.toInt(),
      ctxSize = params.contextSize.toInt(),
      threadCount = params.threadCount.toInt(),
      threadBatchCount = params.threadBatchCount.toInt(),
      length = computePromptLength(params, prompt),
      allowDownload = params.allowDownload,
      path = path.toString(),
      prompt = prompt,
      huggingFaceRepo = huggingFaceRepo,
      huggingFaceName = huggingFaceName,
      huggingFaceToken = params.huggingFaceToken ?: "",
    )) {
      null -> error("Failed to perform local inference: Could not execute `inferSync`")
      else -> InferenceResults.of(out)
    }
  }

  /**
   * ### Inference (Suspending)
   *
   * Perform a local AI inference operation with the given parameters; if the local AI layer is not available, cannot
   * load or initialize, or fails to perform inference, an exception is thrown before any asynchronous context switch
   * occurs.
   *
   * Assuming inference proceeds, a suspending inference result is returned immediately, while inference proceeds in the
   * background. The results of the inference operation can be collected or consumed via the API and methods provided by
   * [InferenceResults.Suspending].
   *
   * To signal the end of inference, the returned flow will complete; under the hood, the native layer sends an empty
   * string.
   *
   * @param params The parameters to use for local AI inference.
   * @param model The model to use for inference.
   * @param prompt Primary input prompt to invoke the model with.
   * @return The results of the inference operation.
   * @see InferenceResults.Suspending suspending inference results
   */
  @JvmStatic public suspend fun infer(params: Parameters, model: Model, prompt: String): InferenceResults.Suspending {
    ensureAvailable()
    return InferenceResults.suspending {
      val (huggingFace, path) = resolveModelInfo(model)
      val (huggingFaceRepo, huggingFaceName) = huggingFace

      val operationId: Int = Random.nextInt()
      val callback = InferenceChunkCallbackImpl(coroutineContext, this)
      callback.activate(operationId)
      withContext(Dispatchers.IO) {
        inferSync(
          verbose = params.verbose,
          disableGpu = params.disableGpu,
          gpuLayers = params.gpuLayers.toInt(),
          ctxSize = params.contextSize.toInt(),
          threadCount = params.threadCount.toInt(),
          threadBatchCount = params.threadBatchCount.toInt(),
          length = computePromptLength(params, prompt),
          allowDownload = params.allowDownload,
          path = path.toString(),
          prompt = prompt,
          huggingFaceRepo = huggingFaceRepo,
          huggingFaceName = huggingFaceName,
          huggingFaceToken = params.huggingFaceToken ?: "",
        ).also {
          callback.complete(operationId)
          callback.close()
        }
      }
    }
  }

  /**
   * ### Initialize
   *
   * Initializes support for local AI features; calls into a JNI entrypoint which prepares things like HuggingFace
   * downloads, model caching on-disk, and so on.
   *
   * @property defaultVerbose Whether to default to verbose mode for native calls into the inference layer.
   * @return Whether initialization was successful.
   */
  @JvmStatic @JvmName("initialize") private external fun initialize(defaultVerbose: Boolean): Boolean

  /**
   * ### Native Inference (Synchronous)
   *
   * Apply local AI configuration parameters and then fire a call to perform inference; this call can take a significant
   * amount of time and should ideally be called from a background thread.
   *
   * @param verbose Whether to emit logs directly to stderr from the native inference layer.
   * @param gpuLayers The number of GPU layers to use for local AI inference; if GPU layers are not supported by the
   *   native layer, this parameter is ignored.
   * @param disableGpu Flag to forcibly disable GPU support.
   * @param allowDownload Flag to allow downloading of models from HuggingFace.
   * @param path On-disk path to the model, or the cache directory for a HuggingFace model.
   * @param prompt Primary input prompt to invoke the model with.
   * @param huggingFaceRepo The HuggingFace repository where the model is hosted, or `""` if not applicable.
   * @param huggingFaceName The name of the model within the repository, or `""` if not applicable.
   * @param huggingFaceToken The HuggingFace API token to use for downloading models, or `""` if not applicable.
   * @param ctxSize The context window size to use for inference.
   * @param threadCount The number of threads to use for inference.
   * @param threadBatchCount The number of threads to use for batching.
   * @param length The combined length of the prompt and output to generate, as a count of tokens.
   * @return Full results of the inference call.
   */
  @Suppress("LongParameterList")
  @JvmStatic @JvmName("inferSync") private external fun inferSync(
    verbose: Boolean,
    gpuLayers: Int,
    disableGpu: Boolean,
    allowDownload: Boolean,
    path: String,
    prompt: String,
    huggingFaceRepo: String,
    huggingFaceName: String,
    huggingFaceToken: String,
    ctxSize: Int,
    threadCount: Int,
    threadBatchCount: Int,
    length: Int,
  ): String?

  /**
   * ### Native Inference (Asynchronous)
   *
   * Apply local AI configuration parameters, prepare a callback facility, and then fire a call to perform inference via
   * background execution; the resulting call object controls the execution flow. The user-provided callback receives
   * chunks of inference results as they are generated.
   *
   * @param operationId The unique identifier for the operation.
   * @param verbose Whether to emit logs directly to stderr from the native inference layer.
   * @param gpuLayers The number of GPU layers to use for local AI inference; if GPU layers are not supported by the
   *   native layer, this parameter is ignored.
   * @param disableGpu Flag to forcibly disable GPU support.
   * @param allowDownload Flag to allow downloading of models from HuggingFace.
   * @param path On-disk path to the model, or the cache directory for a HuggingFace model.
   * @param prompt Primary input prompt to invoke the model with.
   * @param huggingFaceRepo The HuggingFace repository where the model is hosted, or `""` if not applicable.
   * @param huggingFaceName The name of the model within the repository, or `""` if not applicable.
   * @param huggingFaceToken The HuggingFace API token to use for downloading models, or `""` if not applicable.
   * @param ctxSize The context window size to use for inference.
   * @param threadCount The number of threads to use for inference.
   * @param threadBatchCount The number of threads to use for batching.
   * @param length The combined length of the prompt and output to generate, as a count of tokens.
   */
  @Suppress("LongParameterList")
  @JvmStatic @JvmName("inferAsync") private external fun inferAsync(
    operationId: Int,
    verbose: Boolean,
    gpuLayers: Int,
    disableGpu: Boolean,
    allowDownload: Boolean,
    path: String,
    prompt: String,
    huggingFaceRepo: String,
    huggingFaceName: String,
    huggingFaceToken: String,
    ctxSize: Int,
    threadCount: Int,
    threadBatchCount: Int,
    length: Int,
  )

  /**
   * ### De-initialize
   *
   * Unloads support for local AI features.
   */
  @JvmStatic @JvmName("deinitialize") private external fun cleanup()
}
