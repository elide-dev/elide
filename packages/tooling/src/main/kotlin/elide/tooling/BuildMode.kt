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
package elide.tooling

import kotlinx.serialization.Serializable

/**
 * ## Build Mode
 *
 * Enumerates modes recognized for an Elide project build.
 */
@Serializable public enum class BuildMode {
  /** Build mode for development builds. */
  Development,

  /** Build mode for production builds. */
  Debug,

  /** Build mode for release builds. */
  Release;

  public companion object {
    public fun fromString(name: String): BuildMode? = when (name.lowercase()) {
      "development", "dev" -> Development
      "production", "release", "opt" -> Release
      "debug" -> Debug
      else -> null
    }

    public fun default(): BuildMode = Development
  }
}
