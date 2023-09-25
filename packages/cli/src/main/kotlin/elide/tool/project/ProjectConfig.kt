/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress("DataClassPrivateConstructor")

package elide.tool.project

import kotlinx.serialization.Serializable
import elide.tool.project.struct.nodepkg.NodePackage

/** Information about a single project-level configuration file. */
sealed interface ProjectConfig {
  /**
   * TBD.
   */
  val path: String

  /**
   * TBD.
   */
  val name: String? get() = null

  /**
   * TBD.
   */
  val version: String? get() = null

  companion object {
    /** @return Wrapped [ProjectConfig] for the provided [path] and decoded Node package JSON [pkg] structure. */
    @JvmStatic fun packageJson(path: String, pkg: NodePackage) = PackageJsonProjectConfig.wrapping(
      path = path,
      config = pkg,
    )
  }

  /** Project configuration modeled from a `package.json` file. */
  @JvmRecord @Serializable data class PackageJsonProjectConfig private constructor (
    override val path: String,
    private val config: NodePackage,
  ): ProjectConfig {
    override val name: String? get() = config.name
    override val version: String? get() = config.version

    internal companion object {
      @JvmStatic fun wrapping(path: String, config: NodePackage): PackageJsonProjectConfig =
        PackageJsonProjectConfig(path, config)
    }
  }
}
