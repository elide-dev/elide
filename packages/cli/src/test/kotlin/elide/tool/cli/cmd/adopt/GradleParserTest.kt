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

package elide.tool.cli.cmd.adopt

import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

/** Tests for Gradle build file parser functionality. */
class GradleParserTest {
  private fun createTempBuildFile(content: String, isKotlinDsl: Boolean = true): Path {
    val tempFile = Files.createTempFile(
      "build",
      if (isKotlinDsl) ".gradle.kts" else ".gradle"
    )
    tempFile.writeText(content)
    return tempFile
  }

  private fun createProjectWithVersionCatalog(
    buildContent: String,
    catalogContent: String,
    isKotlinDsl: Boolean = true
  ): Path {
    val projectDir = Files.createTempDirectory("test-project")
    val buildFile = projectDir.resolve(if (isKotlinDsl) "build.gradle.kts" else "build.gradle")
    buildFile.writeText(buildContent)

    val gradleDir = projectDir.resolve("gradle")
    Files.createDirectory(gradleDir)
    val catalogFile = gradleDir.resolve("libs.versions.toml")
    catalogFile.writeText(catalogContent)

    return buildFile
  }

  @Test
  fun testParseBasicKotlinDsl() {
    val buildFile = createTempBuildFile("""
      group = "com.example"
      version = "1.0.0"
      description = "Test project"
    """.trimIndent())

    val descriptor = GradleParser.parse(buildFile)

    assertEquals("com.example", descriptor.group)
    assertEquals("1.0.0", descriptor.version)
    assertEquals("Test project", descriptor.description)
  }

  @Test
  fun testParseBasicGroovyDsl() {
    val buildFile = createTempBuildFile("""
      group = 'com.example'
      version = '1.0.0'
      description = 'Test project'
    """.trimIndent(), isKotlinDsl = false)

    val descriptor = GradleParser.parse(buildFile)

    assertEquals("com.example", descriptor.group)
    assertEquals("1.0.0", descriptor.version)
    assertEquals("Test project", descriptor.description)
  }

  @Test
  fun testParseDependenciesKotlinDsl() {
    val buildFile = createTempBuildFile("""
      dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        api("com.google.guava:guava:32.1.3-jre")
      }
    """.trimIndent())

    val descriptor = GradleParser.parse(buildFile)

    assertEquals(3, descriptor.dependencies.size)

    val kotlinStdlib = descriptor.dependencies.find { it.artifactId == "kotlin-stdlib" }
    assertNotNull(kotlinStdlib)
    assertEquals("implementation", kotlinStdlib.configuration)
    assertEquals("org.jetbrains.kotlin", kotlinStdlib.groupId)
    assertEquals("1.9.21", kotlinStdlib.version)

    val junit = descriptor.dependencies.find { it.artifactId == "junit-jupiter" }
    assertNotNull(junit)
    assertEquals("testImplementation", junit.configuration)
    assertTrue(junit.isTestScope())
  }

  @Test
  fun testParseDependenciesGroovyDsl() {
    val buildFile = createTempBuildFile("""
      dependencies {
        implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.21'
        testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
      }
    """.trimIndent(), isKotlinDsl = false)

    val descriptor = GradleParser.parse(buildFile)

    assertEquals(2, descriptor.dependencies.size)
  }

  @Test
  fun testParseDependenciesWithoutVersion() {
    val buildFile = createTempBuildFile("""
      dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        testImplementation("org.junit.jupiter:junit-jupiter")
      }
    """.trimIndent())

    val descriptor = GradleParser.parse(buildFile)

    assertEquals(2, descriptor.dependencies.size)

    val kotlinStdlib = descriptor.dependencies.find { it.artifactId == "kotlin-stdlib" }
    assertNotNull(kotlinStdlib)
    assertNull(kotlinStdlib.version)
  }

  @Test
  fun testParseRepositories() {
    val buildFile = createTempBuildFile("""
      repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
      }
    """.trimIndent())

    val descriptor = GradleParser.parse(buildFile)

    assertEquals(3, descriptor.repositories.size)

    val central = descriptor.repositories.find { it.name == "central" }
    assertNotNull(central)
    assertEquals("https://repo.maven.apache.org/maven2", central.url)

    val google = descriptor.repositories.find { it.name == "google" }
    assertNotNull(google)
    assertEquals("https://maven.google.com", google.url)

    val jitpack = descriptor.repositories.find { it.url == "https://jitpack.io" }
    assertNotNull(jitpack)
  }

  @Test
  fun testParsePlugins() {
    val buildFile = createTempBuildFile("""
      plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.21"
        id("io.ktor.plugin") version "2.3.6"
        id("application")
      }
    """.trimIndent())

    val descriptor = GradleParser.parse(buildFile)

    assertEquals(3, descriptor.plugins.size)

    val kotlinPlugin = descriptor.plugins.find { it.id == "org.jetbrains.kotlin.jvm" }
    assertNotNull(kotlinPlugin)
    assertEquals("1.9.21", kotlinPlugin.version)

    val appPlugin = descriptor.plugins.find { it.id == "application" }
    assertNotNull(appPlugin)
    assertNull(appPlugin.version)
  }

  @Test
  fun testParseVersionCatalogReferences() {
    val buildFile = createProjectWithVersionCatalog(
      buildContent = """
        dependencies {
          implementation(libs.kotlin.stdlib)
          implementation(libs.ktor.server.core)
          testImplementation(libs.junit.jupiter)
        }
      """.trimIndent(),
      catalogContent = """
        [versions]
        kotlin = "1.9.21"
        ktor = "2.3.6"
        junit = "5.10.1"

        [libraries]
        kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
        ktor-server-core = { module = "io.ktor:ktor-server-core", version.ref = "ktor" }
        junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
      """.trimIndent()
    )

    val descriptor = GradleParser.parse(buildFile)

    assertEquals(3, descriptor.dependencies.size)

    val kotlinStdlib = descriptor.dependencies.find { it.artifactId == "kotlin-stdlib" }
    assertNotNull(kotlinStdlib)
    assertEquals("org.jetbrains.kotlin", kotlinStdlib.groupId)
    assertEquals("1.9.21", kotlinStdlib.version)
    assertEquals("implementation", kotlinStdlib.configuration)

    val ktorCore = descriptor.dependencies.find { it.artifactId == "ktor-server-core" }
    assertNotNull(ktorCore)
    assertEquals("io.ktor", ktorCore.groupId)
    assertEquals("2.3.6", ktorCore.version)

    val junit = descriptor.dependencies.find { it.artifactId == "junit-jupiter" }
    assertNotNull(junit)
    assertEquals("5.10.1", junit.version)
    assertTrue(junit.isTestScope())
  }

  @Test
  fun testParseVersionCatalogBundles() {
    val buildFile = createProjectWithVersionCatalog(
      buildContent = """
        dependencies {
          implementation(libs.bundles.ktor)
        }
      """.trimIndent(),
      catalogContent = """
        [versions]
        ktor = "2.3.6"

        [libraries]
        ktor-server-core = { module = "io.ktor:ktor-server-core", version.ref = "ktor" }
        ktor-server-netty = { module = "io.ktor:ktor-server-netty", version.ref = "ktor" }
        ktor-serialization = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

        [bundles]
        ktor = ["ktor-server-core", "ktor-server-netty", "ktor-serialization"]
      """.trimIndent()
    )

    val descriptor = GradleParser.parse(buildFile)

    // Bundle should be expanded into 3 dependencies
    assertEquals(3, descriptor.dependencies.size)

    val core = descriptor.dependencies.find { it.artifactId == "ktor-server-core" }
    assertNotNull(core)
    assertEquals("io.ktor", core.groupId)
    assertEquals("2.3.6", core.version)

    val netty = descriptor.dependencies.find { it.artifactId == "ktor-server-netty" }
    assertNotNull(netty)

    val serialization = descriptor.dependencies.find { it.artifactId == "ktor-serialization-kotlinx-json" }
    assertNotNull(serialization)
  }

  @Test
  fun testParseMixedDependencies() {
    val buildFile = createProjectWithVersionCatalog(
      buildContent = """
        dependencies {
          implementation(libs.kotlin.stdlib)
          implementation("com.google.guava:guava:32.1.3-jre")
          testImplementation(libs.bundles.testing)
        }
      """.trimIndent(),
      catalogContent = """
        [versions]
        kotlin = "1.9.21"
        junit = "5.10.1"
        mockito = "5.7.0"

        [libraries]
        kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
        junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }

        [bundles]
        testing = ["junit-jupiter", "mockito-core"]
      """.trimIndent()
    )

    val descriptor = GradleParser.parse(buildFile)

    // 1 from catalog + 1 direct + 2 from bundle = 4 total
    assertEquals(4, descriptor.dependencies.size)

    val guava = descriptor.dependencies.find { it.artifactId == "guava" }
    assertNotNull(guava)
    assertEquals("32.1.3-jre", guava.version)

    val kotlin = descriptor.dependencies.find { it.artifactId == "kotlin-stdlib" }
    assertNotNull(kotlin)
    assertEquals("1.9.21", kotlin.version)

    val junit = descriptor.dependencies.find { it.artifactId == "junit-jupiter" }
    assertNotNull(junit)
    assertTrue(junit.isTestScope())
  }

  @Test
  fun testParseSettingsFile() {
    val projectDir = Files.createTempDirectory("test-project")
    val settingsFile = projectDir.resolve("settings.gradle.kts")
    settingsFile.writeText("""
      rootProject.name = "my-project"
      include("module-a")
      include("module-b")
    """.trimIndent())

    val buildFile = projectDir.resolve("build.gradle.kts")
    buildFile.writeText("""
      group = "com.example"
      version = "1.0.0"
    """.trimIndent())

    val descriptor = GradleParser.parse(buildFile)

    assertEquals("my-project", descriptor.name)
    assertEquals(2, descriptor.modules.size)
    assertTrue(descriptor.modules.contains("module-a"))
    assertTrue(descriptor.modules.contains("module-b"))
  }

  @Test
  fun testDependencyCoordinateFormat() {
    val buildFile = createTempBuildFile("""
      dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
        testImplementation("org.junit.jupiter:junit-jupiter")
      }
    """.trimIndent())

    val descriptor = GradleParser.parse(buildFile)

    val kotlinDep = descriptor.dependencies.find { it.artifactId == "kotlin-stdlib" }
    assertNotNull(kotlinDep)
    assertEquals("org.jetbrains.kotlin:kotlin-stdlib:1.9.21", kotlinDep.coordinate())

    val junitDep = descriptor.dependencies.find { it.artifactId == "junit-jupiter" }
    assertNotNull(junitDep)
    assertEquals("org.junit.jupiter:junit-jupiter", junitDep.coordinate())
  }
}
