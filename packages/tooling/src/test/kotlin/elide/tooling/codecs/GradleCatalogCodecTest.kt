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
package elide.tooling.codecs

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import java.io.InputStream
import jakarta.inject.Inject
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.tooling.project.codecs.GradleCatalogCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.GradleCatalogManifest

@MicronautTest class GradleCatalogCodecTest {
  @Inject lateinit var codec: GradleCatalogCodec

  @Test fun `should use standard default path`() {
    assertEquals(
      expected = Path("gradle/libs.versions.toml"),
      actual = codec.defaultPath(),
    )
  }

  @Test fun `should accept valid manifest paths`() {
    val validCases = mapOf(
      "gradle/libs.versions.toml" to "standard catalog path and name",
      "gradle/custom.versions.toml" to "standard catalog path, custom name",
      "example.versions.toml" to "custom catalog path and name",
    )

    validCases.forEach { value, reason ->
      assertTrue(codec.supported(Path(value)), "expected $reason '$value' to be supported")
    }
  }

  @Test fun `should reject invalid manifest paths`() {
    val invalidCases = mapOf(
      "libs.versions.json" to "wrong extension",
      "example.txt" to "wrong extension",
      "another.toml" to "no extension prefix",
    )

    invalidCases.forEach { value, reason ->
      assertFalse(codec.supported(Path(value)), "expected $reason '$value' to be rejected")
    }
  }

  @Test fun `should read manifest file`() {
    val resource = sampleManifestResource()
    assertEquals(
      expected = SampleManifest,
      actual = codec.parse(resource, defaultManifestState),
    )
  }

  companion object {
    fun sampleManifestResource(): InputStream {
      return GradleCatalogCodecTest::class.java.getResourceAsStream("/manifests/libs.versions.toml")!!
    }

    val SampleManifest = GradleCatalogManifest(
      versions = mapOf(
        "guava" to "33.4.8-jre",
      ),
      libraries = emptyMap(),
      plugins = emptyMap(),
      bundles = emptyMap(),
    )

    val SampleElideManifest = ElidePackageManifest(
      name = "elide-gradle-sample",
      version = "1.0.0",
      description = "A sample Elide app",
      dependencies = ElidePackageManifest.DependencyResolution(
        maven = ElidePackageManifest.MavenDependencies(),
      ),
    )
  }
}
