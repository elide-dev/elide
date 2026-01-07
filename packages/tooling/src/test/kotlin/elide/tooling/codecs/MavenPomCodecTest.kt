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
import org.apache.maven.model.Build
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import jakarta.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import elide.tooling.project.codecs.MavenPomManifestCodec
import elide.tooling.project.codecs.PackageManifestCodec
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.manifest.MavenPomManifest

internal val defaultManifestState = object: PackageManifestCodec.ManifestBuildState {
  override val isDebug: Boolean get() = false
  override val isRelease: Boolean get() = false
}

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
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState).model

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

  @Test fun `should convert pom to elide manifest`() {
    val resource = sampleManifestResource()
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Basic project info
    assertEquals("my-app", elide.name)
    assertEquals("1", elide.version)
    assertEquals("An example Maven project", elide.description)

    // JVM target from maven.compiler.target property
    assertNotNull(elide.jvm)
    assertEquals("21", elide.jvm?.target?.argValue)

    // Maven coordinates
    assertNotNull(elide.dependencies.maven.coordinates)
    assertEquals("com.example", elide.dependencies.maven.coordinates?.group)
    assertEquals("my-app", elide.dependencies.maven.coordinates?.name)

    // Dependencies
    assertEquals(1, elide.dependencies.maven.packages.size)
    assertEquals("com.google.guava", elide.dependencies.maven.packages[0].group)
    assertEquals("guava", elide.dependencies.maven.packages[0].name)
    assertEquals(2, elide.dependencies.maven.testPackages.size)

    // Source directories
    assertEquals(2, elide.sources.size)
    assertTrue(elide.sources.containsKey("main"))
    assertTrue(elide.sources.containsKey("test"))

    // Jar artifact
    assertTrue(elide.artifacts.containsKey("jar"))
  }

  @Test fun `should parse exec-maven-plugin java goal`() {
    val resource = loadManifestResource("/manifests/pom-with-exec.xml")
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have 2 exec tasks
    assertEquals(2, elide.execTasks.size)

    // Find the java exec task
    val javaTask = elide.execTasks.find { it.type == ElidePackageManifest.ExecTaskType.JAVA }
    assertNotNull(javaTask)
    assertEquals("generate-data", javaTask.id)
    assertEquals("com.example.DataGenerator", javaTask.mainClass)
    assertEquals(ElidePackageManifest.BuildPhase.COMPILE, javaTask.phase)
    assertEquals(2, javaTask.args.size)
    assertEquals("--output", javaTask.args[0])
    assertEquals("target/data", javaTask.args[1])
    assertEquals(ElidePackageManifest.ClasspathScope.COMPILE, javaTask.classpathScope)
    assertEquals("json", javaTask.systemProperties["data.format"])
  }

  @Test fun `should parse exec-maven-plugin exec goal`() {
    val resource = loadManifestResource("/manifests/pom-with-exec.xml")
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Find the executable exec task
    val execTask = elide.execTasks.find { it.type == ElidePackageManifest.ExecTaskType.EXECUTABLE }
    assertNotNull(execTask)
    assertEquals("run-script", execTask.id)
    assertEquals("scripts/generate.sh", execTask.executable)
    assertEquals(ElidePackageManifest.BuildPhase.GENERATE_SOURCES, execTask.phase)
    assertEquals(2, execTask.args.size)
    assertEquals("--verbose", execTask.args[0])
    assertEquals("src/main", execTask.args[1])
    assertEquals("test-value", execTask.env["ENV_VAR"])
    assertNotNull(execTask.workingDirectory)
  }

  @Test fun `should prefer release flag over source target in maven-compiler-plugin`() {
    val resource = loadManifestResource("/manifests/pom-with-release.xml")
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // release=17 should take precedence over source=11, target=11 and properties
    assertNotNull(elide.jvm)
    assertEquals("17", elide.jvm?.target?.argValue)
  }

  @Test fun `should normalize legacy 1-dot-x jvm target to modern format`() {
    val resource = loadManifestResource("/manifests/pom-legacy-jvm.xml")
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // 1.8 should be normalized to 8
    assertNotNull(elide.jvm)
    assertEquals("8", elide.jvm?.target?.argValue)
  }

  @Test fun `should return empty exec tasks when no exec plugin configured`() {
    val resource = sampleManifestResource()
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Standard pom.xml has no exec plugin
    assertTrue(elide.execTasks.isEmpty())
  }

  @Test fun `should parse maven-javadoc-plugin configuration`() {
    val resource = loadManifestResource("/manifests/pom-with-javadoc.xml")
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have javadoc artifact
    val javadocArtifact = elide.artifacts["javadoc"]
    assertNotNull(javadocArtifact)
    assertTrue(javadocArtifact is ElidePackageManifest.JavadocJar)

    val javadoc = javadocArtifact as ElidePackageManifest.JavadocJar
    assertEquals("Example API", javadoc.windowTitle)
    assertEquals("Example API Documentation", javadoc.docTitle)
    assertEquals(2, javadoc.groups.size)
    assertTrue(javadoc.groups.containsKey("Core API"))
    assertTrue(javadoc.groups.containsKey("Implementation"))
    assertEquals(listOf("com.example.core", "com.example.api"), javadoc.groups["Core API"])
    assertEquals(1, javadoc.links.size)
  }

  @Test fun `should parse maven-source-plugin configuration`() {
    val resource = loadManifestResource("/manifests/pom-with-sources.xml")
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have source artifacts
    val sourcesArtifact = elide.artifacts["sources"]
    assertNotNull(sourcesArtifact)
    assertTrue(sourcesArtifact is ElidePackageManifest.SourceJar)

    val sources = sourcesArtifact as ElidePackageManifest.SourceJar
    assertEquals("sources", sources.classifier)
    assertTrue(sources.excludes.contains("**/*.properties"))

    // Should also have filtered-sources artifact
    val filteredArtifact = elide.artifacts["filtered-sources"]
    assertNotNull(filteredArtifact)
    assertTrue(filteredArtifact is ElidePackageManifest.SourceJar)

    val filtered = filteredArtifact as ElidePackageManifest.SourceJar
    assertEquals("filtered-sources", filtered.classifier)
    assertTrue(filtered.excludes.contains("com/example/internal/**"))
  }

  @Test fun `should parse maven-assembly-plugin configuration`() {
    val resource = loadManifestResource("/manifests/pom-with-assembly.xml")
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Should have assembly artifact
    val assemblyArtifact = elide.artifacts["assembly:make-assembly"]
    assertNotNull(assemblyArtifact)
    assertTrue(assemblyArtifact is ElidePackageManifest.Assembly)

    val assembly = assemblyArtifact as ElidePackageManifest.Assembly
    assertEquals("make-assembly", assembly.id)
    assertEquals("src/main/assembly/dist.xml", assembly.descriptorPath)
  }

  @Test fun `should return empty javadoc artifacts when no javadoc plugin configured`() {
    val resource = sampleManifestResource()
    val pom = codec.parseAsFile(resource.toPath(), defaultManifestState)
    val elide = codec.toElidePackage(pom)

    // Standard pom.xml has no javadoc plugin
    assertNull(elide.artifacts["javadoc"])
  }

  companion object {
    fun loadManifestResource(resourcePath: String): File {
      val stream: InputStream = MavenPomCodecTest::class.java.getResourceAsStream(resourcePath)
        ?: error("Resource not found: $resourcePath")
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

    fun sampleManifestResource(): File = loadManifestResource("/manifests/pom.xml")

    val SampleManifest = MavenPomManifest(
      model = Model().apply {
        groupId = "com.example"
        artifactId = "my-app"
        version = "1"
        name = "My Example App"
        description = "An example Maven project"

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
          version = "5.12.0"
          scope = "test"
        }, Dependency().apply {
          groupId = "org.junit.jupiter"
          artifactId = "junit-jupiter-api"
          version = "5.12.0"
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
