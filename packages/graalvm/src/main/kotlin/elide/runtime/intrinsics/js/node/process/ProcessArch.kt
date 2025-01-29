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
package elide.runtime.intrinsics.js.node.process

import elide.core.api.Symbolic

// Recognized token value for x86.
private const val TOKEN_X86: String = "x86"

// Recognized token value for x64.
private const val TOKEN_X64: String = "x64"

// Recognized token value for ARM.
private const val TOKEN_ARM: String = "arm"

// Recognized token value for ARM64.
private const val TOKEN_ARM64: String = "arm64"

/**
 * # Process Architecture
 *
 * Describes, in terms familiar to Node, the current processor architecture.
 *
 * @param symbol Node API symbol expected for this architecture type.
 */
public enum class ProcessArch (override val symbol: String) : Symbolic<String> {
  /**
   * The x86 architecture.
   */
  X86(TOKEN_X86),

  /**
   * The x64 architecture.
   */
  X64(TOKEN_X64),

  /**
   * The ARM architecture.
   */
  ARM(TOKEN_ARM),

  /**
   * The ARM64 architecture.
   */
  ARM64(TOKEN_ARM64);

  /** Static methods for dealing with Node-style architecture declarations. */
  public companion object : Symbolic.SealedResolver<String, ProcessArch> {
    /**
     * # Host Architecture
     *
     * Returns the architecture on which the current process is running.
     *
     * @return The architecture on which the current process is running.
     */
    @JvmStatic public fun host(): ProcessArch = when (System.getProperty("os.arch").lowercase()) {
      "x86" -> X86
      "x86_64", "amd64" -> X64
      "arm" -> ARM
      "aarch64", "arm64" -> ARM64
      else -> throw IllegalStateException("Unsupported architecture: ${System.getProperty("os.arch")}")
    }

    override fun resolve(symbol: String): ProcessArch = when (symbol) {
      TOKEN_X86 -> X86
      TOKEN_X64 -> X64
      TOKEN_ARM -> ARM
      TOKEN_ARM64 -> ARM64
      else -> throw unresolved(symbol)
    }
  }
}
