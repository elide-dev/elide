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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import jakarta.inject.Inject
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.tooling.project.codecs.PyProjectManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.PyProjectManifest

@MicronautTest @Disabled("pyproject.toml is not fully supported yet") class PyProjectCodecTest {
  @Inject lateinit var codec: PyProjectManifestCodec

  @Test fun `should use standard default path`() {
    assertEquals(
      expected = Path("pyproject.toml"),
      actual = codec.defaultPath(),
    )
  }

  @Test fun `should accept valid manifest paths`() {
    val validCases = mapOf(
      "pyproject.toml" to "plain package path",
      "./my/project/pyproject.toml" to "relative package path",
      "/home/me/projects/elide/pyproject.toml" to "absolute package path",
    )

    validCases.forEach { value, reason ->
      assertTrue(codec.supported(Path(value)), "expected $reason '$value' to be supported")
    }
  }

  @Test fun `should reject invalid manifest paths`() {
    val invalidCases = mapOf(
      "pyproject.py" to "wrong extension",
      "pyproject.tomcat" to "wrong extension",
      "hello.toml" to "wrong name",
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
      expected = reference.trim(),
      actual = output.toByteArray().decodeToString().trim(),
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
      return PyProjectCodecTest::class.java.getResourceAsStream("/manifests/pyproject.toml")!!
    }

    val SampleManifest = PyProjectManifest(
      buildSystem = PyProjectManifest.BuildSystemConfig(),
      project = PyProjectManifest.ProjectConfig(
        name = "elide-py-sample",
        version = "1.0.0",
        description = "A sample Elide Python package",
        dependencies = listOf(
          "httpx",
          "gidgethub[httpx]>4.0.0",
          "django>2.1; os_name != 'nt'",
          "django>2.0; os_name == 'nt'",
        ),
        optionalDependencies = mapOf("gui" to listOf("PyQt5")),
      ),
    )

    val SampleElideManifest = ElidePackageManifest(
      name = "elide-py-sample",
      version = "1.0.0",
      description = "A sample Elide Python package",
      entrypoint = listOf("index.js"),
      dependencies = ElidePackageManifest.DependencyResolution(
        pip = ElidePackageManifest.PipDependencies(
          packages = listOf(
            ElidePackageManifest.PipPackage("httpx"),
            ElidePackageManifest.PipPackage("gidgethub[httpx]>4.0.0"),
            ElidePackageManifest.PipPackage("django>2.1; os_name != 'nt'"),
            ElidePackageManifest.PipPackage("django>2.0; os_name == 'nt'"),
          ),
          optionalPackages = mapOf(
            "gui" to listOf(ElidePackageManifest.PipPackage("PyQt5")),
          ),
        ),
      ),
    )
  }
}
