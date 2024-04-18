/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

// Recognized token value for Linux.
private const val TOKEN_LINUX: String = "linux"

// Recognized token value for Darwin/macOS.
private const val TOKEN_MACOS: String = "darwin"

// Recognized token value for Windows.
private const val TOKEN_WINDOWS: String = "win32"

/**
 * # Process Platform
 *
 * Describes, in terms familiar to Node, the current operating system ("platform").
 *
 * @param symbol Node API symbol expected for this platform type.
 */
public enum class ProcessPlatform (override val symbol: String) : Symbolic<String> {
  /**
   * The Linux platform.
   */
  LINUX(TOKEN_LINUX),

  /**
   * The macOS platform.
   */
  MACOS(TOKEN_MACOS),

  /**
   * The Windows platform.
   */
  WINDOWS(TOKEN_WINDOWS);

  /** Static methods for dealing with Node-style platform declarations. */
  public companion object : Symbolic.SealedResolver<String, ProcessPlatform> {
    /**
     * # Host Platform
     *
     * Returns the platform on which the current process is running.
     *
     * @return The platform on which the current process is running.
     */
    @JvmStatic public fun host(): ProcessPlatform = when (System.getProperty("os.name").lowercase()) {
      "linux" -> LINUX
      "mac os x" -> MACOS
      "windows" -> WINDOWS
      else -> throw IllegalStateException("Unsupported platform: ${System.getProperty("os.name")}")
    }

    override fun resolve(symbol: String): ProcessPlatform = when (symbol) {
      TOKEN_LINUX -> LINUX
      TOKEN_MACOS -> MACOS
      TOKEN_WINDOWS -> WINDOWS
      else -> throw unresolved(symbol)
    }
  }
}
