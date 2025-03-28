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
