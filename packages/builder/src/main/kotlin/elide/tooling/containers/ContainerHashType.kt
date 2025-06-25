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
package elide.tooling.containers

import elide.core.api.Symbolic

// Container hash type: sha256.
private const val CONTAINER_HASH_TYPE_SHA256: String = "sha256"

/**
 * ## Container Hash Type
 *
 * Enumerates types of hashes that may be used to identify container images.
 */
public enum class ContainerHashType (override val symbol: String) : Symbolic<String>, ContainerComponent {
  /** SHA256 hash type. */
  SHA256(CONTAINER_HASH_TYPE_SHA256);

  override fun asString(): String = symbol

  /** Resolves container hash types. **/
  public companion object : Symbolic.SealedResolver<String, ContainerHashType> {
    override fun resolve(symbol: String): ContainerHashType = when (symbol) {
      CONTAINER_HASH_TYPE_SHA256 -> SHA256
      else -> throw unresolved(symbol)
    }
  }
}
