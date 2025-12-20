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
import elide.tooling.project.adopt.bazel.BazelParser
import elide.tooling.project.adopt.bazel.BazelDescriptor
import elide.tooling.project.adopt.PklGenerator

/**
 * Unit tests for BazelParser.
 */
class BazelParserTest {
  private lateinit var tempDir: Path

  @BeforeTest
  fun setup() {
    tempDir = Files.createTempDirectory("bazel-parser-test")
  }

  @AfterTest
  fun cleanup() {
    tempDir.toFile().deleteRecursively()
  }

  @Test
  fun testParseBasicWorkspace() {
    val workspaceContent = """
      workspace(name = "test-project")

      maven_install(
        artifacts = [
          "com.google.guava:guava:32.1.3-jre",
          "org.junit.jupiter:junit-jupiter:5.10.0",
        ],
        repositories = [
          "https://repo1.maven.org/maven2",
        ]
      )
    """.trimIndent()

    val workspaceFile = tempDir.resolve("WORKSPACE")
    workspaceFile.writeText(workspaceContent)

    val bazel = BazelParser.parse(tempDir)

    assertEquals("test-project", bazel.name)
    assertEquals(2, bazel.dependencies.size)

    val guava = bazel.dependencies.find { it.artifactId() == "guava" }
    assertNotNull(guava)
    assertEquals("com.google.guava", guava.groupId())
    assertEquals("32.1.3-jre", guava.version())

    val junit = bazel.dependencies.find { it.artifactId() == "junit-jupiter" }
    assertNotNull(junit)
    assertEquals("org.junit.jupiter", junit.groupId())
    assertEquals("5.10.0", junit.version())
  }

  @Test
  fun testParseModuleBazel() {
    val moduleContent = """
      module(name = "my-module")

      maven.install(
        artifacts = [
          "com.squareup.okhttp3:okhttp:4.12.0",
        ]
      )
    """.trimIndent()

    val moduleFile = tempDir.resolve("MODULE.bazel")
    moduleFile.writeText(moduleContent)

    val bazel = BazelParser.parse(tempDir)

    assertEquals("my-module", bazel.name)
    assertEquals(1, bazel.dependencies.size)

    val okhttp = bazel.dependencies.first()
    assertEquals("com.squareup.okhttp3", okhttp.groupId())
    assertEquals("okhttp", okhttp.artifactId())
    assertEquals("4.12.0", okhttp.version())
  }

  @Test
  fun testParseBuildFile() {
    val workspaceContent = """
      workspace(name = "test-project")
    """.trimIndent()

    val buildContent = """
      java_library(
        name = "core",
        srcs = [
          "src/main/java/Main.java",
          "src/main/java/Utils.java",
        ],
        deps = [
          "@maven//:com_google_guava_guava",
        ],
      )

      java_test(
        name = "core-test",
        srcs = [
          "src/test/java/MainTest.java",
          "src/test/java/UtilsTest.java",
        ],
        deps = [
          ":core",
          "@maven//:org_junit_jupiter_junit_jupiter",
        ],
      )
    """.trimIndent()

    tempDir.resolve("WORKSPACE").writeText(workspaceContent)
    tempDir.resolve("BUILD").writeText(buildContent)

    val bazel = BazelParser.parse(tempDir)

    assertEquals("test-project", bazel.name)
    assertEquals(2, bazel.targets.size)

    val coreLib = bazel.targets.find { it.name == "core" }
    assertNotNull(coreLib)
    assertEquals("java_library", coreLib.rule)
    assertTrue(coreLib.srcs.isNotEmpty())
    assertFalse(coreLib.isTestTarget())

    val coreTest = bazel.targets.find { it.name == "core-test" }
    assertNotNull(coreTest)
    assertEquals("java_test", coreTest.rule)
    assertTrue(coreTest.srcs.isNotEmpty())
    assertTrue(coreTest.isTestTarget())
  }

  @Test
  fun testParseKotlinTargets() {
    val workspaceContent = """
      workspace(name = "kotlin-project")
    """.trimIndent()

    val buildContent = """
      kt_jvm_library(
        name = "lib",
        srcs = ["Main.kt", "Utils.kt"],
      )

      kt_jvm_test(
        name = "lib-test",
        srcs = ["MainTest.kt"],
        deps = [":lib"],
      )
    """.trimIndent()

    tempDir.resolve("WORKSPACE").writeText(workspaceContent)
    tempDir.resolve("BUILD.bazel").writeText(buildContent)

    val bazel = BazelParser.parse(tempDir)

    assertEquals(2, bazel.targets.size)

    val lib = bazel.targets.find { it.name == "lib" }
    assertNotNull(lib)
    assertEquals("kt_jvm_library", lib.rule)
    assertEquals(2, lib.srcs.size)
    assertTrue(lib.srcs.contains("Main.kt"))
    assertTrue(lib.srcs.contains("Utils.kt"))

    val test = bazel.targets.find { it.name == "lib-test" }
    assertNotNull(test)
    assertEquals("kt_jvm_test", test.rule)
    assertTrue(test.isTestTarget())
  }

  @Test
  fun testParseMavenArtifactFormat() {
    val moduleContent = """
      module(name = "test")

      maven.artifact("com.google.code.gson:gson:2.10.1")
    """.trimIndent()

    val moduleFile = tempDir.resolve("MODULE.bazel")
    moduleFile.writeText(moduleContent)

    val bazel = BazelParser.parse(tempDir)

    assertEquals(1, bazel.dependencies.size)

    val gson = bazel.dependencies.first()
    assertEquals("com.google.code.gson", gson.groupId())
    assertEquals("gson", gson.artifactId())
    assertEquals("2.10.1", gson.version())
  }

  @Test
  fun testDependencyCoordinateParsing() {
    val dep = BazelDescriptor.Dependency("com.example:artifact:1.2.3")

    assertEquals("com.example", dep.groupId())
    assertEquals("artifact", dep.artifactId())
    assertEquals("1.2.3", dep.version())
  }

  @Test
  fun testDependencyWithoutVersion() {
    val dep = BazelDescriptor.Dependency("com.example:artifact")

    assertEquals("com.example", dep.groupId())
    assertEquals("artifact", dep.artifactId())
    assertNull(dep.version())
  }

  @Test
  fun testTargetIsTestTarget() {
    val javaTest = BazelDescriptor.Target("test1", "java_test")
    assertTrue(javaTest.isTestTarget())

    val ktTest = BazelDescriptor.Target("test2", "kt_jvm_test")
    assertTrue(ktTest.isTestTarget())

    val javaLib = BazelDescriptor.Target("lib1", "java_library")
    assertFalse(javaLib.isTestTarget())

    val javaBin = BazelDescriptor.Target("bin1", "java_binary")
    assertFalse(javaBin.isTestTarget())
  }

  @Test
  fun testFallbackToDirectoryName() {
    val workspaceContent = """
      # No workspace name defined
    """.trimIndent()

    tempDir.resolve("WORKSPACE").writeText(workspaceContent)

    val bazel = BazelParser.parse(tempDir)

    // Should fall back to directory name
    assertEquals(tempDir.fileName.toString(), bazel.name)
  }

  @Test
  fun testMissingWorkspaceFile() {
    assertFailsWith<IllegalArgumentException> {
      BazelParser.parse(tempDir)
    }
  }

  @Test
  fun testPklGenerationForBazel() {
    val bazel = BazelDescriptor(
      name = "test-project",
      dependencies = listOf(
        BazelDescriptor.Dependency("com.google.guava:guava:32.1.3-jre"),
        BazelDescriptor.Dependency("org.junit.jupiter:junit-jupiter:5.10.0"),
      ),
      targets = listOf(
        BazelDescriptor.Target(
          name = "core",
          rule = "java_library",
          srcs = listOf("src/main/java/Main.java"),
        ),
        BazelDescriptor.Target(
          name = "core-test",
          rule = "java_test",
          srcs = listOf("src/test/java/MainTest.java"),
        ),
      ),
    )

    val pkl = PklGenerator.generate(bazel)

    // Verify structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("name = \"test-project\""))

    // Verify dependencies
    assertTrue(pkl.contains("dependencies {"))
    assertTrue(pkl.contains("maven {"))
    assertTrue(pkl.contains("com.google.guava:guava:32.1.3-jre"))
    assertTrue(pkl.contains("org.junit.jupiter:junit-jupiter:5.10.0"))

    // Verify sources
    assertTrue(pkl.contains("sources {"))

    // Verify targets are documented
    assertTrue(pkl.contains("core") || pkl.contains("target"))
  }
}
