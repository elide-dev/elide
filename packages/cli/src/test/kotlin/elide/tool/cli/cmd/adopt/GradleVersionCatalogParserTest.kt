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
import elide.tooling.project.adopt.gradle.VersionCatalogParser
import elide.tooling.project.adopt.gradle.VersionCatalog

/** Tests for Gradle version catalog parser functionality. */
class VersionCatalogParserTest {
  private fun createTempCatalog(content: String): Path {
    val tempFile = Files.createTempFile("test-catalog", ".toml")
    tempFile.writeText(content)
    return tempFile
  }

  @Test
  fun testParseVersions() {
    val catalog = createTempCatalog("""
      [versions]
      kotlin = "1.9.21"
      ktor = "2.3.6"
      junit = "5.10.1"
    """.trimIndent())

    val parsed = VersionCatalogParser.parse(catalog)

    assertEquals(3, parsed.versions.size)
    assertEquals("1.9.21", parsed.versions["kotlin"])
    assertEquals("2.3.6", parsed.versions["ktor"])
    assertEquals("5.10.1", parsed.versions["junit"])
  }

  @Test
  fun testParseLibrariesWithVersionRef() {
    val catalog = createTempCatalog("""
      [versions]
      kotlin = "1.9.21"

      [libraries]
      kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
      kotlin-reflect = { module = "org.jetbrains.kotlin:kotlin-reflect", version.ref = "kotlin" }
    """.trimIndent())

    val parsed = VersionCatalogParser.parse(catalog)

    assertEquals(2, parsed.libraries.size)

    val stdlib = parsed.libraries["kotlin-stdlib"]
    assertNotNull(stdlib)
    assertEquals("org.jetbrains.kotlin:kotlin-stdlib", stdlib.module)
    assertEquals("kotlin", stdlib.versionRef)
    assertEquals("1.9.21", stdlib.resolveVersion(parsed.versions))
  }

  @Test
  fun testParseLibrariesWithDirectVersion() {
    val catalog = createTempCatalog("""
      [libraries]
      commons-lang3 = { module = "org.apache.commons:commons-lang3", version = "3.14.0" }
    """.trimIndent())

    val parsed = VersionCatalogParser.parse(catalog)

    assertEquals(1, parsed.libraries.size)

    val commonsLang = parsed.libraries["commons-lang3"]
    assertNotNull(commonsLang)
    assertEquals("org.apache.commons:commons-lang3", commonsLang.module)
    assertEquals("3.14.0", commonsLang.version)
    assertEquals("3.14.0", commonsLang.resolveVersion(parsed.versions))
  }

  @Test
  fun testParseLibrariesWithoutVersion() {
    val catalog = createTempCatalog("""
      [libraries]
      junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher" }
    """.trimIndent())

    val parsed = VersionCatalogParser.parse(catalog)

    assertEquals(1, parsed.libraries.size)

    val launcher = parsed.libraries["junit-platform-launcher"]
    assertNotNull(launcher)
    assertEquals("org.junit.platform:junit-platform-launcher", launcher.module)
    assertNull(launcher.version)
    assertNull(launcher.versionRef)
    assertNull(launcher.resolveVersion(parsed.versions))
  }

  @Test
  fun testParseBundles() {
    val catalog = createTempCatalog("""
      [libraries]
      ktor-server-core = { module = "io.ktor:ktor-server-core", version = "2.3.6" }
      ktor-server-netty = { module = "io.ktor:ktor-server-netty", version = "2.3.6" }
      ktor-serialization = { module = "io.ktor:ktor-serialization-kotlinx-json", version = "2.3.6" }

      [bundles]
      ktor = ["ktor-server-core", "ktor-server-netty", "ktor-serialization"]
    """.trimIndent())

    val parsed = VersionCatalogParser.parse(catalog)

    assertEquals(1, parsed.bundles.size)

    val ktorBundle = parsed.bundles["ktor"]
    assertNotNull(ktorBundle)
    assertEquals(3, ktorBundle.size)
    assertEquals(listOf("ktor-server-core", "ktor-server-netty", "ktor-serialization"), ktorBundle)
  }

  @Test
  fun testParsePlugins() {
    val catalog = createTempCatalog("""
      [versions]
      kotlin = "1.9.21"

      [plugins]
      kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
      ktor = { id = "io.ktor.plugin", version = "2.3.6" }
    """.trimIndent())

    val parsed = VersionCatalogParser.parse(catalog)

    assertEquals(2, parsed.plugins.size)

    val kotlinPlugin = parsed.plugins["kotlin-jvm"]
    assertNotNull(kotlinPlugin)
    assertEquals("org.jetbrains.kotlin.jvm", kotlinPlugin.id)
    assertEquals("kotlin", kotlinPlugin.versionRef)
    assertEquals("1.9.21", kotlinPlugin.resolveVersion(parsed.versions))

    val ktorPlugin = parsed.plugins["ktor"]
    assertNotNull(ktorPlugin)
    assertEquals("io.ktor.plugin", ktorPlugin.id)
    assertEquals("2.3.6", ktorPlugin.version)
  }

  @Test
  fun testParseComprehensiveCatalog() {
    val catalogResource = this::class.java.getResource("/version-catalogs/comprehensive.versions.toml")
    assertNotNull(catalogResource, "Test resource not found")
    val catalogPath = Path.of(catalogResource.toURI())
    val parsed = VersionCatalogParser.parse(catalogPath)

    // Check versions
    assertEquals("1.9.21", parsed.versions["kotlin"])
    assertEquals("2.3.6", parsed.versions["ktor"])
    assertEquals("5.10.1", parsed.versions["junit"])

    // Check libraries
    assertTrue(parsed.libraries.containsKey("kotlin-stdlib"))
    assertTrue(parsed.libraries.containsKey("ktor-server-core"))
    assertTrue(parsed.libraries.containsKey("junit-jupiter"))
    assertTrue(parsed.libraries.containsKey("commons-lang3"))

    // Check bundles
    assertEquals(3, parsed.bundles.size)
    assertEquals(
      listOf("ktor-server-core", "ktor-server-netty", "ktor-serialization"),
      parsed.bundles["ktor"]
    )
    assertEquals(
      listOf("junit-jupiter", "mockito-core", "mockito-kotlin"),
      parsed.bundles["testing"]
    )

    // Check plugins
    assertEquals(2, parsed.plugins.size)
    assertEquals("org.jetbrains.kotlin.jvm", parsed.plugins["kotlin-jvm"]?.id)
  }

  @Test
  fun testParseEmptyCatalog() {
    val catalog = createTempCatalog("")
    val parsed = VersionCatalogParser.parse(catalog)

    assertTrue(parsed.versions.isEmpty())
    assertTrue(parsed.libraries.isEmpty())
    assertTrue(parsed.bundles.isEmpty())
    assertTrue(parsed.plugins.isEmpty())
  }

  @Test
  fun testParseWithComments() {
    val catalog = createTempCatalog("""
      # Version definitions
      [versions]
      kotlin = "1.9.21"  # Latest stable

      # Library declarations
      [libraries]
      # Kotlin standard library
      kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
    """.trimIndent())

    val parsed = VersionCatalogParser.parse(catalog)

    assertEquals(1, parsed.versions.size)
    assertEquals("1.9.21", parsed.versions["kotlin"])
    assertEquals(1, parsed.libraries.size)
  }

  @Test
  fun testFindVersionCatalog() {
    val projectDir = Files.createTempDirectory("test-project")
    val gradleDir = projectDir.resolve("gradle")
    Files.createDirectory(gradleDir)

    val catalogFile = gradleDir.resolve("libs.versions.toml")
    catalogFile.writeText("[versions]\nkotlin = \"1.9.21\"")

    val found = VersionCatalogParser.findVersionCatalog(projectDir)
    assertNotNull(found)
    assertEquals(catalogFile, found)
  }

  @Test
  fun testFindVersionCatalogNotPresent() {
    val projectDir = Files.createTempDirectory("test-project")
    val found = VersionCatalogParser.findVersionCatalog(projectDir)
    assertNull(found)
  }
}
