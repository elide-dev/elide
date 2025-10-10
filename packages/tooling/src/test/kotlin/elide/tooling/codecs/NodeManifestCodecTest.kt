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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import jakarta.inject.Inject
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.tooling.project.codecs.NodeManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.NodePackageManifest

@MicronautTest class NodeManifestCodecTest {
  @Inject lateinit var codec: NodeManifestCodec

  @Test fun `should use standard default path`() {
    assertEquals(
      expected = Path("package.json"),
      actual = codec.defaultPath(),
    )
  }

  @Test fun `should accept valid manifest paths`() {
    val validCases = mapOf(
      "package.json" to "plain package path",
      "./my/project/package.json" to "relative package path",
      "/home/me/projects/elide/package.json" to "absolute package path",
    )

    validCases.forEach { value, reason ->
      assertTrue(codec.supported(Path(value)), "expected $reason '$value' to be supported")
    }
  }

  @Test fun `should reject invalid manifest paths`() {
    val invalidCases = mapOf(
      "package.js" to "wrong extension",
      "package.jsonfile" to "wrong extension",
      "hello.json" to "wrong name",
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

  @Test fun `should write manifest file`() {
    val reference = sampleManifestResource().bufferedReader().use { it.readText() }
    val output = ByteArrayOutputStream()

    codec.write(SampleManifest, output)

    assertEquals(
      expected = reference.trim().replace(" ", "").replace("\n", "").replace("\t", ""),
      actual = output.toByteArray().decodeToString().trim().replace(" ", "").replace("\n", "").replace("\t", ""),
    )
  }

  @Test fun fromElidePackage() {
    assertEquals(
      expected = SampleManifest,
      actual = codec.fromElidePackage(SampleElideManifest),
    )
  }

  @Test fun toElidePackage() {
    assertEquals(
      expected = SampleElideManifest,
      actual = codec.toElidePackage(SampleManifest),
    )
  }

  companion object {
    fun sampleManifestResource(): InputStream {
      return NodeManifestCodecTest::class.java.getResourceAsStream("/manifests/package.json")!!
    }

    val SampleManifest = NodePackageManifest(
      name = "elide-js-sample",
      version = "1.0.0",
      description = "A sample Elide app",
      main = "index.js",
      scripts = mapOf("start" to "elide index.js"),
      dependencies = mapOf("foo" to "^1.2.3"),
      devDependencies = mapOf("bar" to "^0.1.1"),
    )

    val SampleElideManifest = ElidePackageManifest(
      name = "elide-js-sample",
      version = "1.0.0",
      description = "A sample Elide app",
      entrypoint = listOf("index.js"),
      scripts = mapOf("start" to "elide index.js"),
      dependencies = ElidePackageManifest.DependencyResolution(
        npm = ElidePackageManifest.NpmDependencies(
          packages = listOf(
            ElidePackageManifest.NpmPackage("foo", "^1.2.3"),
          ),
          devPackages = listOf(
            ElidePackageManifest.NpmPackage("bar", "^0.1.1"),
          ),
        ),
      ),
    )
  }
}
