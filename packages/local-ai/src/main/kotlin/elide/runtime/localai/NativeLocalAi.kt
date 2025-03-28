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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.collections.asSequence
import kotlin.coroutines.coroutineContext
import kotlin.streams.asSequence
import elide.core.api.Symbolic
import elide.runtime.core.lib.NativeLibraries

/**
 * Represents an integer which acts as the return code for an asynchronous native inference operation. Codes returned
 * from the native layer should be interpreted via [NativeLocalAi.ReturnCode].
 */
public typealias InferenceReturnCode = Int

// Return code for a queued inference operation.
private const val INFERENCE_RETURN_CODE_OK = 0

// Return code for a failed inference initialization.
private const val INFERENCE_RETURN_CODE_INIT_FAILED = 1

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
 * Results are made uniform via the [NativeLocalAi.InferenceResults] API, which is sealed to include:
 *
 * - [NativeLocalAi.InferenceResults.Error] for failed inference operations.
 * - [NativeLocalAi.InferenceResults.Streamed] for streamed results (i.e. via Java [Stream]s).
 * - [NativeLocalAi.InferenceResults.Suspending] for suspending results (i.e. via Kotlin [Flow]s).
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

  /**
   * ### Parameters
   *
   * Parameters for configuring local AI inference; these are consumed statically, and passed to the native layer for
   * use by the inference engine.
   *
   * @property verbose Flag to emit native inference logs to stderr.
   * @property allowDownload Flag to allow downloading of models from HuggingFace.
   * @property disableGpu Flag to forcibly disable GPU support.
   * @property gpuLayers The number of GPU layers to use for local AI inference; if GPU layers are not supported by the
   *   native layer, this parameter is ignored.
   * @property contextSize The context window size to use for inference.
   * @property huggingFaceToken The HuggingFace API token to use for downloading models, or `null` if not applicable.
   * @property threadCount The number of threads to use for inference.
   * @property threadBatchCount The number of threads to use for batching.
   * @property length Explicit token length (prompt + desired output) for generation, or `null` to default (`42`).
   * @constructor Create a set of local AI parameters.
   */
  @JvmRecord public data class Parameters private constructor (
    public val verbose: Boolean = DEFAULT_VERBOSE,
    public val allowDownload: Boolean = DEFAULT_ALLOW_DOWNLOAD,
    public val disableGpu: Boolean = DISABLE_GPU,
    public val huggingFaceToken: String? = System.getenv("HUGGINGFACE_TOKEN"),
    public val gpuLayers: UInt = DEFAULT_GPU_LAYERS,
    public val contextSize: UInt = DEFAULT_CONTEXT_SIZE,
    public val threadCount: UInt = DEFAULT_THREAD_COUNT,
    public val threadBatchCount: UInt = DEFAULT_THREAD_BATCH_COUNT,
    public val length: UInt? = null,
  ) {
    public companion object {
      /** Kill-switch for GPU support. */
      public const val DISABLE_GPU: Boolean = false

      /** Default flag to emit native inference logs to stderr. */
      public const val DEFAULT_VERBOSE: Boolean = false

      /** Default flag to allow downloading of models from HuggingFace. */
      public const val DEFAULT_ALLOW_DOWNLOAD: Boolean = true

      /** Default GPU layer count, where supported. */
      public const val DEFAULT_GPU_LAYERS: UInt = 1000u

      /** Default context window size. */
      public const val DEFAULT_CONTEXT_SIZE: UInt = 2048u

      /** Default thread count. */
      public const val DEFAULT_THREAD_COUNT: UInt = 16u

      /** Default thread batch value. */
      public const val DEFAULT_THREAD_BATCH_COUNT: UInt = 4u

      /** @return Parameters with the provided values. */
      @JvmStatic public fun create(
        verbose: Boolean = DEFAULT_VERBOSE,
        gpuLayers: UInt = DEFAULT_GPU_LAYERS,
        disableGpu: Boolean = DISABLE_GPU,
        contextSize: UInt = DEFAULT_CONTEXT_SIZE,
        allowDownload: Boolean = DEFAULT_ALLOW_DOWNLOAD,
        threadCount: UInt = DEFAULT_THREAD_COUNT,
        threadBatchCount: UInt = DEFAULT_THREAD_BATCH_COUNT,
        huggingFaceToken: String? = System.getenv("HUGGINGFACE_TOKEN"),
      ): Parameters = Parameters(
        verbose = verbose,
        disableGpu = disableGpu,
        gpuLayers = gpuLayers,
        contextSize = contextSize,
        huggingFaceToken = huggingFaceToken,
        allowDownload = allowDownload,
        threadCount = threadCount,
        threadBatchCount = threadBatchCount,
      )

      /** @return Default inference parameters. */
      @JvmStatic public fun defaults(): Parameters = create()
    }
  }

  /**
   * ### Model Specification
   *
   * Model specification objects describe the AI model which is used for inference. The engine supports local models
   * which are available on-disk, as well as remote models hosted on platforms like HuggingFace. If a remote model is
   * selected, it will be downloaded and cached locally for use.
   *
   * See [OnDiskModel] and [HuggingFaceModel] for more information.
   *
   * When selecting a model, certain parameters or other inputs may need alignment with expected values. Consult the
   * model's documentation for more information.
   */
  public sealed interface Model {
    /**
     * Local (On-disk) Model
     *
     * Describes a model which already resides on-disk at a known path.
     *
     * @property path The path to the model on disk.
     * @constructor Create a local model specification.
     */
    @JvmInline public value class OnDiskModel internal constructor (public val path: Path) : Model

    /**
     * Remote (HuggingFace) model.
     *
     * Describes a model which is hosted on HuggingFace, and must be downloaded and cached for local use; the parameters
     * [repo] and [name] are used to identify the model, and [path] is the local path where the model is cached.
     *
     * @property repo The HuggingFace repository where the model is hosted.
     * @property name The name of the model within the repository.
     * @property path The local path where the model is cached; if `null`, one will be calculated at runtime.
     * @constructor Create a HuggingFace model specification.
     */
    @JvmRecord public data class HuggingFaceModel internal constructor (
      public val repo: String,
      public val name: String,
      public val path: Path? = null,
    ) : Model

    /** Factories for model specifications. */
    public companion object {
      /** @return Configured local path for a model. */
      @JvmStatic public fun atPath(path: Path): OnDiskModel = OnDiskModel(path)

      /** @return Configured HuggingFace parameters for a model. */
      @JvmStatic public fun huggingface(repo: String, name: String, path: Path? = null): HuggingFaceModel {
        return HuggingFaceModel(
          repo = repo,
          name = name,
          path = path,
        )
      }
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

  /**
   * ## Inference Return Code
   *
   * Decodes the return codes offered by [inferAsync]; the raw return value is symbolized by [InferenceReturnCode].
   */
  public enum class ReturnCode (override val symbol: InferenceReturnCode) : Symbolic<InferenceReturnCode> {
    /** Inference is proceeding. */
    OK(INFERENCE_RETURN_CODE_OK),

    /** Inference layer initialization failed. */
    INIT_FAILED(INFERENCE_RETURN_CODE_INIT_FAILED);

    /** Resolution tools for [InferenceReturnCode] values. */
    public companion object : Symbolic.SealedResolver<InferenceReturnCode, ReturnCode> {
      override fun resolve(symbol: InferenceReturnCode): ReturnCode = when (symbol) {
        OK.symbol -> OK
        INIT_FAILED.symbol -> INIT_FAILED
        else -> error("Unknown inference return code: $symbol")
      }
    }
  }

  /**
   * ## Inference Results
   *
   * Specifies the uniform API defined by each inference result container type. Inference containers can hold their
   * result data ahead of time, or buffer it as it arrives, or provide it to callers in chunked/streaming form.
   *
   * @property success Whether the operation succeeded.
   */
  public sealed interface InferenceResults {
    /** Whether the operation succeeded. */
    public val success: Boolean

    /**
     * ### Inference Results (Error)
     *
     * Static result which indicates a failed inference operation; if possible, a message is specified.
     *
     * @property message Error message describing what caused inference to fail.
     */
    @JvmInline public value class Error internal constructor(private val msg: String) : InferenceResults {
      override val success: Boolean get() = false

      /** Error message which caused inference to fail. */
      public val message: String get() = msg
    }

    /**
     * ### Inference Results (Async Error)
     *
     * Static result which indicates a failed inference operation; if possible, a message is specified.
     *
     * @property code Error code indicating the reason for the failure.
     * @property message Error message describing what caused inference to fail.
     */
    @JvmInline public value class AsyncError internal constructor(
      public val code: ReturnCode,
      private val msg: String
    ) : InferenceResults {
      override val success: Boolean get() = false

      /** Error message which caused inference to fail. */
      public val message: String get() = msg
    }

    /**
     * ### Inference Results (Streamed)
     *
     * Streamed results of a local AI inference operation.
     *
     * @param stream Stream of tokens generated by the inference operation.
     */
    @JvmInline public value class Streamed internal constructor(public val stream: Stream<String>) : InferenceResults {
      override val success: Boolean get() = true

      /** Resulting token stream as a Kotlin sequence. */
      public val sequence: Sequence<String> get() = stream.asSequence()
    }

    /**
     * ### Inference Results (Suspending)
     *
     * Streamed results of a local AI inference operation.
     *
     * @param flow Flow of tokens generated by the inference operation.
     */
    @JvmInline public value class Suspending @PublishedApi internal constructor(
      private val flow: Flow<String>,
    ) : InferenceResults {
      override val success: Boolean get() = true

      /** Resulting token flow. */
      public fun asFlow(): Flow<String> = flow

      /** Collect the results into a collection. */
      public suspend fun collect(): Collection<String> = flow.toList()
    }

    /**
     * ### Inference Results (Synchronous)
     *
     * Synchronous results of a local AI inference operation.
     *
     * @param value The results buffered as a string.
     */
    @JvmInline public value class Sync internal constructor(public val value: Collection<String>) : InferenceResults {
      override val success: Boolean get() = true

      /** Resulting token stream as a Kotlin sequence. */
      public val sequence: Sequence<String> get() = value.asSequence()
    }

    /** Factories for inference results. */
    public companion object {
      /** @return Streamed inference results. */
      @JvmStatic
      public inline fun suspending(crossinline producer: suspend FlowCollector<String>.() -> Unit): InferenceResults {
        return flow {
          producer()
        }.let {
          Suspending(it)
        }
      }

      /** @return Streamed inference results. */
      @JvmStatic public fun streamed(stream: Stream<String>): InferenceResults = Streamed(stream)

      /** @return Synchronous inference results [Collection] of [String]s. */
      @JvmStatic public fun of(value: Collection<String>): InferenceResults = Sync(value)

      /** @return Synchronous inference results from a [String]. */
      @JvmStatic public fun of(value: String): InferenceResults = Sync(listOf(value))
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
   * ## Inference Chunk Callback
   *
   * Callback type which is used to consume chunks of generated inference output. An object of this type is defined by
   * the engine or developer, and is called from the native inference layer (via JNI) as inference output is generated
   * from background threads.
   *
   * See [infer] for more information.
   */
  @FunctionalInterface public fun interface InferenceChunkCallback {
    /**
     * Receive a chunk of generated output from an inference operation.
     *
     * This method is called from the native layer, so it must be available for use via reflection and JNI.
     *
     * @param chunk The chunk of output generated by the inference operation.
     */
    public fun onChunk(chunk: String)
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
    return when (inferSync(
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
      false -> error("Failed to perform local inference: Could not execute `inferSync`")
      true -> (results() ?: error("No local inference results available")).let {
        InferenceResults.of(it.lines().filter { it.isNotEmpty() })
      }
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
  @JvmStatic public fun infer(params: Parameters, model: Model, prompt: String): InferenceResults {
    ensureAvailable()
    return InferenceResults.suspending {
      val (huggingFace, path) = resolveModelInfo(model)
      val (huggingFaceRepo, huggingFaceName) = huggingFace
      coroutineContext.let { ctx ->
        when (inferAsync(
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
          callback = InferenceChunkCallback { chunk ->
            runBlocking(ctx) { emit(chunk) }
          },
        ).let { ReturnCode.resolve(it) }) {
          // let the flow proceed to caller use
          ReturnCode.OK -> {}

          // we couldn't enqueue the inference operation; fail eagerly
          ReturnCode.INIT_FAILED -> error("Failed to initialize local AI inference")
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
   * @return Whether inference was successful; gather results via [results].
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
  ): Boolean

  /**
   * ### Native Inference (Asynchronous)
   *
   * Apply local AI configuration parameters, prepare a callback facility, and then fire a call to perform inference via
   * background execution; the resulting call object controls the execution flow. The user-provided callback receives
   * chunks of inference results as they are generated.
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
   * @param callback The callback to receive chunks of inference results.
   * @return Whether inference was successful; gather results via [results].
   */
  @Suppress("LongParameterList")
  @JvmStatic @JvmName("inferSync") private external fun inferAsync(
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
    callback: InferenceChunkCallback,
  ): InferenceReturnCode

  /**
   * ### Results (Synchronous)
   *
   * Retrieve the buffered results of a synchronous local inference operation; if no results are available at all,
   * `null` is returned. Otherwise, an instance of [InferenceResults] is returned.
   *
   * For errors arising from synchronous use, an object also may be returned in the form of [InferenceResults.Error].
   * [inferSync] should always be called before this method.
   *
   * @return The results of the inference operation, or `null` if no results are available.
   */
  @JvmStatic @JvmName("results") private external fun results(): String?

  /**
   * ### De-initialize
   *
   * Unloads support for local AI features.
   */
  @JvmStatic @JvmName("deinitialize") private external fun cleanup()
}
