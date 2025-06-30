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
package elide.tooling.jvm.gradle

import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlin.test.*

class GradleMetadataCodecTest {
  private companion object {
    private const val METADATA_PATH = "/gradleMetadata"
    private const val JVM_METADATA_NAME = "elide-server-1.0.0-beta7.module"
    private const val MACOS_METADATA_NAME = "elide-base-macosx64-1.0.0-alpha11.module"
    private const val JVM_METADATA = "$METADATA_PATH/$JVM_METADATA_NAME"
    private const val MACOS_METADATA = "$METADATA_PATH/$MACOS_METADATA_NAME"
  }

  private fun readMetadataAt(path: String): GradleModuleMetadata {
    return requireNotNull(this::class.java.getResourceAsStream(path)) {
      "Failed to locate metadata for testing at path '$path'"
    }.bufferedReader(StandardCharsets.UTF_8).use { stream ->
      val json = stream.readText()
      Json.decodeFromString(json)
    }
  }

  @Test fun `gradle metadata - decode jvm library metadata`() {
    val metadata = readMetadataAt(JVM_METADATA)
    assertNotNull(metadata)
  }

  @Test fun `gradle metadata - decode macos native library metadata`() {
    val metadata = readMetadataAt(MACOS_METADATA)
    assertNotNull(metadata)
  }
}
