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

/**
 * ## Prompt Input
 *
 * Sealed class hierarchy describing acceptable prompt inputs; prompts usually take the form of simple strings (host or
 * guest side), but can also be objects or executables.
 */
public sealed interface PromptInput {
  /**
   * Render the prompt represented by this input, producing a final string to pass to the LLM.
   *
   * @return String to use to prompt the LLM.
   */
  public fun render(): String

  /**
   * Implements a prompt input wrapping a simple [string].
   *
   * This is the simplest form of prompt input. Construct one with [PromptInput.of].
   */
  @JvmInline public value class StringPrompt internal constructor(private val string: String) : PromptInput {
    override fun render(): String = string
  }

  /** Factories for [PromptInput] values. */
  public companion object {
    /** @return Simple string prompt. */
    @JvmStatic public fun of(prompt: String): PromptInput = StringPrompt(prompt)
  }
}
