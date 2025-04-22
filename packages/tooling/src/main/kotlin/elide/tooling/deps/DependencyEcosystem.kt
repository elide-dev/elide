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

package elide.tooling.deps

/**
 * ## Dependency Ecosystem
 */
public sealed interface DependencyEcosystem {
  public val name: String

  /**
   * ### Maven
   */
  public data object Maven : DependencyEcosystem {
    override val name: String get() = "Maven"
  }

  /**
   * ### NPM
   */
  public data object NPM : DependencyEcosystem {
    override val name: String get() = "NPM"
  }

  /**
   * ### JSR
   */
  public data object JSR : DependencyEcosystem {
    override val name: String get() = "JSR"
  }

  /**
   * ### PyPI
   */
  public data object PyPI : DependencyEcosystem {
    override val name: String get() = "PyPI"
  }

  /**
   * ### Gems
   */
  public data object Gems : DependencyEcosystem {
    override val name: String get() = "Rubygems"
  }

  /**
   * ### HuggingFace
   */
  public data object HuggingFace : DependencyEcosystem {
    override val name: String get() = "HuggingFace"
  }
}
