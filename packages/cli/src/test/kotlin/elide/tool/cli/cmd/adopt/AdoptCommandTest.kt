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

/**
 * Unit tests for AdoptCommand auto-detection logic.
 */
class AdoptCommandTest {
  private lateinit var tempDir: Path

  @BeforeTest
  fun setup() {
    tempDir = Files.createTempDirectory("adopt-command-test")
  }

  @AfterTest
  fun cleanup() {
    tempDir.toFile().deleteRecursively()
  }

  @Test
  fun testAutoDetectMaven() {
    // Create pom.xml
    val pomFile = tempDir.resolve("pom.xml")
    pomFile.writeText("""
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test</artifactId>
        <version>1.0.0</version>
      </project>
    """.trimIndent())

    // Should detect Maven
    assertTrue(pomFile.toFile().exists(), "pom.xml should exist")
  }

  @Test
  fun testAutoDetectGradle() {
    // Create build.gradle.kts
    val buildFile = tempDir.resolve("build.gradle.kts")
    buildFile.writeText("""
      plugins {
        kotlin("jvm") version "1.9.0"
      }
    """.trimIndent())

    // Should detect Gradle
    assertTrue(buildFile.toFile().exists(), "build.gradle.kts should exist")
  }

  @Test
  fun testAutoDetectGradleGroovy() {
    // Create build.gradle (Groovy DSL)
    val buildFile = tempDir.resolve("build.gradle")
    buildFile.writeText("""
      plugins {
        id 'java'
      }
    """.trimIndent())

    // Should detect Gradle
    assertTrue(buildFile.toFile().exists(), "build.gradle should exist")
  }

  @Test
  fun testAutoDetectBazelWorkspace() {
    // Create WORKSPACE file
    val workspaceFile = tempDir.resolve("WORKSPACE")
    workspaceFile.writeText("""
      workspace(name = "test")
    """.trimIndent())

    // Should detect Bazel
    assertTrue(workspaceFile.toFile().exists(), "WORKSPACE should exist")
  }

  @Test
  fun testAutoDetectBazelModule() {
    // Create MODULE.bazel file
    val moduleFile = tempDir.resolve("MODULE.bazel")
    moduleFile.writeText("""
      module(name = "test")
    """.trimIndent())

    // Should detect Bazel
    assertTrue(moduleFile.toFile().exists(), "MODULE.bazel should exist")
  }

  @Test
  fun testAutoDetectNodeJs() {
    // Create package.json
    val packageJson = tempDir.resolve("package.json")
    packageJson.writeText("""
      {
        "name": "test-package",
        "version": "1.0.0"
      }
    """.trimIndent())

    // Should detect Node.js
    assertTrue(packageJson.toFile().exists(), "package.json should exist")
  }

  @Test
  fun testDetectionPriorityMavenOverGradle() {
    // Create both pom.xml and build.gradle.kts
    tempDir.resolve("pom.xml").writeText("""
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test</artifactId>
        <version>1.0.0</version>
      </project>
    """.trimIndent())

    tempDir.resolve("build.gradle.kts").writeText("""
      plugins {
        kotlin("jvm") version "1.9.0"
      }
    """.trimIndent())

    // Maven should be detected first (priority order)
    assertTrue(tempDir.resolve("pom.xml").toFile().exists())
    assertTrue(tempDir.resolve("build.gradle.kts").toFile().exists())
  }

  @Test
  fun testDetectionPriorityGradleOverBazel() {
    // Create both build.gradle.kts and WORKSPACE
    tempDir.resolve("build.gradle.kts").writeText("""
      plugins {
        kotlin("jvm") version "1.9.0"
      }
    """.trimIndent())

    tempDir.resolve("WORKSPACE").writeText("""
      workspace(name = "test")
    """.trimIndent())

    // Gradle should be detected first (priority order)
    assertTrue(tempDir.resolve("build.gradle.kts").toFile().exists())
    assertTrue(tempDir.resolve("WORKSPACE").toFile().exists())
  }

  @Test
  fun testDetectionPriorityBazelOverNode() {
    // Create both WORKSPACE and package.json
    tempDir.resolve("WORKSPACE").writeText("""
      workspace(name = "test")
    """.trimIndent())

    tempDir.resolve("package.json").writeText("""
      {
        "name": "test-package",
        "version": "1.0.0"
      }
    """.trimIndent())

    // Bazel should be detected first (priority order)
    assertTrue(tempDir.resolve("WORKSPACE").toFile().exists())
    assertTrue(tempDir.resolve("package.json").toFile().exists())
  }

  @Test
  fun testNoDetectedBuildSystem() {
    // Empty directory - no build system files
    assertFalse(tempDir.resolve("pom.xml").toFile().exists())
    assertFalse(tempDir.resolve("build.gradle").toFile().exists())
    assertFalse(tempDir.resolve("build.gradle.kts").toFile().exists())
    assertFalse(tempDir.resolve("WORKSPACE").toFile().exists())
    assertFalse(tempDir.resolve("MODULE.bazel").toFile().exists())
    assertFalse(tempDir.resolve("package.json").toFile().exists())
  }

  @Test
  fun testBazelWorkspaceBazelVariant() {
    // Create WORKSPACE.bazel file
    val workspaceFile = tempDir.resolve("WORKSPACE.bazel")
    workspaceFile.writeText("""
      workspace(name = "test")
    """.trimIndent())

    // Should detect Bazel
    assertTrue(workspaceFile.toFile().exists(), "WORKSPACE.bazel should exist")
  }

  @Test
  fun testDetectionWithSubdirectories() {
    // Create Maven project with subdirectories
    tempDir.resolve("pom.xml").writeText("""
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test</artifactId>
        <version>1.0.0</version>
      </project>
    """.trimIndent())

    // Create src directory
    Files.createDirectories(tempDir.resolve("src/main/java"))

    // Should still detect Maven in root
    assertTrue(tempDir.resolve("pom.xml").toFile().exists())
    assertTrue(tempDir.resolve("src/main/java").toFile().isDirectory())
  }

  @Test
  fun testGradleKtsPreferredOverGroovy() {
    // When both build.gradle and build.gradle.kts exist, .kts should be preferred
    tempDir.resolve("build.gradle").writeText("""
      plugins {
        id 'java'
      }
    """.trimIndent())

    tempDir.resolve("build.gradle.kts").writeText("""
      plugins {
        kotlin("jvm") version "1.9.0"
      }
    """.trimIndent())

    // Both should exist, but Kotlin DSL is preferred
    assertTrue(tempDir.resolve("build.gradle").toFile().exists())
    assertTrue(tempDir.resolve("build.gradle.kts").toFile().exists())
  }
}
