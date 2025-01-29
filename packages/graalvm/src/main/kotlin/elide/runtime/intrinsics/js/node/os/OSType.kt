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
package elide.runtime.intrinsics.js.node.os

import elide.core.api.Symbolic

/**
 * # Operating System: Type
 *
 * Enumerates supported types of operating systems, including POSIX and Win32.
 */
public enum class OSType (override val symbol: String) : Symbolic<String> {
  POSIX("posix"),
  WIN32("win32");

  public companion object {
    /** The current operating system type. */
    @JvmStatic public fun current(): OSType = when (System.getProperty("os.name").lowercase()) {
      "windows" -> WIN32
      else -> POSIX
    }
  }
}
