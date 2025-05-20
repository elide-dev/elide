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
package elide.runtime.gvm.kotlin.resources

import java.nio.charset.StandardCharsets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
public sealed interface KotlinResource {
  public val artifact: String
  public val coordinate: String
  public val sha256: String
}

@Serializable
public data class KotlinDependencyResource(
  override val coordinate: String,
  override val artifact: String,
  override val sha256: String,
) : KotlinResource

@Serializable
public data class KotlinBuiltinResource(
  override val artifact: String,
  override val sha256: String,
) : KotlinResource {
  override val coordinate: String get() = "elide:${artifact.replace(".jar", "")}"
}

@Serializable
public data class KotlinResourceIndex(
  val resources: List<KotlinResource>,
) {
  public operator fun plus(other: KotlinResourceIndex): KotlinResourceIndex {
    return KotlinResourceIndex(
      resources = (this.resources + other.resources).distinct().sortedBy { it.coordinate },
    )
  }

  public companion object {
    private const val INDEX = "/META-INF/elide/embedded/runtime/kt/kotlin-resources.json"

    @JvmStatic public fun load(): KotlinResourceIndex = KotlinResourceIndex::class.java.getResourceAsStream(INDEX).use {
      requireNotNull(it) {
        "Failed to locate Kotlin resource index at path: '$INDEX'"
      }.bufferedReader(StandardCharsets.UTF_8).readText().let {
        Json.decodeFromString(it)
      }
    }
  }
}
