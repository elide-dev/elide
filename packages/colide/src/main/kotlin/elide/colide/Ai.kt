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
     * High-level chat interface.
     * @param message User message
     * @return AI response
     */
    public fun chat(message: String): String {
        if (!initialized) {
            // Try default model path
            initialized = init("/zip/share/tinyllama.gguf")
            if (!initialized) {
                return "[AI not available - model not loaded]"
            }
        }
        return complete(message)
    }
}
