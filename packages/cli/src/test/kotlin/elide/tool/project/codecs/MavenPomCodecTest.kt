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
package elide.tool.project.codecs

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.apache.maven.model.Build
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import jakarta.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.tooling.project.codecs.MavenPomManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.MavenPomManifest

@MicronautTest class MavenPomCodecTest {
  @Inject lateinit var codec: MavenPomManifestCodec

  @Test fun `should use standard default path`() {
    assertEquals(
      expected = Path("pom.xml"),
      actual = codec.defaultPath(),
    )
  }

  @Test fun `should accept valid manifest paths`() {
    val validCases = mapOf(
      "pom.xml" to "standard pom path",
      "mod/pom.xml" to "pom path in a subdirectory",
    )

    validCases.forEach { value, reason ->
      assertTrue(codec.supported(Path(value)), "expected $reason '$value' to be supported")
    }
  }

  @Test fun `should reject invalid manifest paths`() {
    val invalidCases = mapOf(
      "pom.json" to "wrong extension",
      "example.xml" to "wrong name",
    )

    invalidCases.forEach { value, reason ->
      assertFalse(codec.supported(Path(value)), "expected $reason '$value' to be rejected")
    }
  }

  @Test fun `should read manifest file`() {
    val resource = sampleManifestResource()
    val ref = SampleManifest.model
    val pom = codec.parseAsFile(resource.toPath()).model

    assertEquals(ref.groupId, pom.groupId)
    assertEquals(ref.artifactId, pom.artifactId)
    assertEquals(ref.version, pom.version)
    assertEquals(ref.dependencies.size, pom.dependencies.size)
    assertEquals(ref.build?.plugins?.size, pom.build?.plugins?.size)
    val dep = ref.dependencies.first()
    val pomDep = pom.dependencies.first()
    assertEquals(dep.groupId, pomDep.groupId)
    assertEquals(dep.artifactId, pomDep.artifactId)
    assertEquals(dep.version, pomDep.version)
    val pluginFirst = ref.build?.plugins?.first()
    val pomPluginFirst = pom.build?.plugins?.first()
    assertNotNull(pluginFirst)
    assertNotNull(pomPluginFirst)
    assertEquals(pluginFirst.groupId, pomPluginFirst.groupId)
    assertEquals(pluginFirst.artifactId, pomPluginFirst.artifactId)
    assertEquals(pluginFirst.version, pomPluginFirst.version)
  }

  companion object {
    fun sampleManifestResource(): File {
      val stream = MavenPomCodecTest::class.java.getResourceAsStream("/manifests/pom.xml")!!
      val tmpdir = Files.createTempDirectory("elide-test-maven")
      stream.bufferedReader(StandardCharsets.UTF_8).use { inbuf ->
        tmpdir.resolve("pom.xml").bufferedWriter(StandardCharsets.UTF_8).use { outbuf ->
          inbuf.copyTo(outbuf)
        }
      }
      val target = tmpdir.resolve("pom.xml").toFile()
      target.deleteOnExit()
      return target
    }

    val SampleManifest = MavenPomManifest(
      model = Model().apply {
        groupId = "com.example"
        artifactId = "my-app"
        version = "1"

        build = Build().apply {
          plugins = listOf(Plugin().apply {
            groupId = "org.apache.maven.plugins"
            artifactId = "maven-compiler-plugin"
            version = "3.14.0"
            configuration = mapOf(
              "source" to "21",
              "target" to "21",
            )
          })
        }

        dependencies = listOf(Dependency().apply {
          groupId = "com.google.guava"
          artifactId = "guava"
          version = "33.4.8-jre"
        }, Dependency().apply {
          groupId = "org.junit.jupiter"
          artifactId = "junit-jupiter-engine"
          version = "5.9.1"
          scope = "test"
        }, Dependency().apply {
          groupId = "org.junit.jupiter"
          artifactId = "junit-jupiter-api"
          version = "5.9.1"
          scope = "test"
        })
      }
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
