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

package elide.colide

/**
 * # AI Inference
 *
 * llamafile integration for bare metal AI inference.
 *
 * ## Example
 * ```kotlin
 * Ai.init("/zip/share/tinyllama.gguf")
 * val response = Ai.complete("Hello, ")
 * println(response)
 * Ai.shutdown()
 * ```
 */
public object Ai {
    private var initialized = false
    private var currentModelPath: String? = null
    private var currentModelName: String? = null

    /**
     * Initialize AI with the given model.
     * @param modelPath Path to GGUF model file (can be in /zip/)
     * @return true if initialization succeeded
     */
    @JvmStatic
    public external fun init(modelPath: String): Boolean

    /**
     * Complete a prompt using the AI model.
     * @param prompt The input prompt
     * @param maxLen Maximum output length
     * @return Generated text
     */
    @JvmStatic
    public external fun complete(prompt: String, maxLen: Int = 256): String

    /**
     * Shutdown AI and release resources.
     */
    @JvmStatic
    public external fun shutdown()

    /**
     * Load a model from path or HuggingFace.
     * 
     * Supports:
     * - Local paths: /path/to/model.gguf
     * - Bundled: /zip/share/tinyllama.gguf
     * - HuggingFace (hosted mode): hf:openai/gpt-oss-20b
     * 
     * @param pathOrHf Model path or HuggingFace ID (hf:owner/repo)
     * @return Result message
     */
    public fun loadModel(pathOrHf: String): String {
        // Shutdown current model if any
        if (initialized) {
            shutdown()
            initialized = false
        }
        
        val actualPath = if (pathOrHf.startsWith("hf:")) {
            // HuggingFace model - requires network (hosted mode only)
            val hfId = pathOrHf.removePrefix("hf:")
            // In hosted mode, Elide can download from HF
            // For now, we'll simulate this path resolution
            val localCache = "/tmp/hf-models/${hfId.replace("/", "_")}.gguf"
            // TODO: Actually download from HuggingFace
            // For now, return error if not cached
            if (!java.io.File(localCache).exists()) {
                return "HuggingFace download not yet implemented. Please download model manually:\n" +
                       "  huggingface-cli download $hfId --local-dir /tmp/hf-models\n" +
                       "Then: model load $localCache"
            }
            localCache
        } else {
            pathOrHf
        }
        
        // Try to load the model
        initialized = init(actualPath)
        return if (initialized) {
            currentModelPath = actualPath
            currentModelName = actualPath.substringAfterLast("/").removeSuffix(".gguf")
            "Model loaded: $currentModelName\nPath: $actualPath"
        } else {
            "Failed to load model: $actualPath\nCheck path exists and is valid GGUF."
        }
    }
    
    /**
     * Get info about currently loaded model.
     */
    public fun modelInfo(): String {
        return if (initialized && currentModelPath != null) {
            """
            |Current Model: ${currentModelName ?: "Unknown"}
            |Path: $currentModelPath
            |Status: Loaded and ready
            """.trimMargin()
        } else {
            """
            |No model loaded.
            |
            |Load a model with:
            |  model load /path/to/model.gguf
            |  model load hf:openai/gpt-oss-20b
            """.trimMargin()
        }
    }
    
    /**
     * Check if a model is currently loaded.
     */
    public fun isLoaded(): Boolean = initialized

    /**
     * High-level chat interface.
     * @param message User message
     * @return AI response
     */
    public fun chat(message: String): String {
        if (!initialized) {
            // Try default model path
            initialized = init("/zip/share/tinyllama.gguf")
            if (initialized) {
                currentModelPath = "/zip/share/tinyllama.gguf"
                currentModelName = "tinyllama"
            }
            if (!initialized) {
                return "[AI not available - use 'model load <path>' first]"
            }
        }
        return complete(message)
    }
}
