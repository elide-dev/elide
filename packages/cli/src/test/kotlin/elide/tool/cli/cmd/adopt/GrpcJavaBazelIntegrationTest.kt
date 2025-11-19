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
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * Integration test for gRPC-Java - validates our Bazel implementation works with a real-world project.
 *
 * This test uses the cloned gRPC-Java repository at /tmp/grpc-java.
 * If the repository doesn't exist, the test is skipped.
 */
class GrpcJavaBazelIntegrationTest {
  private val grpcJavaPath = Paths.get("/tmp/grpc-java")

  @BeforeTest
  fun checkPreconditions() {
    if (!grpcJavaPath.exists()) {
      println("Skipping gRPC-Java Bazel integration test - repository not found at $grpcJavaPath")
      println("To enable this test, clone: git clone --depth 1 https://github.com/grpc/grpc-java.git /tmp/grpc-java")
    }
  }

  @Test
  fun testParseGrpcJavaBazelProject() {
    // Skip if repo not cloned
    if (!grpcJavaPath.exists()) {
      return
    }

    val bazel = BazelParser.parse(grpcJavaPath)

    // Verify basic project info
    assertEquals("grpc-java", bazel.name)
    assertNotNull(bazel.workspaceFile)
    assertNotNull(bazel.buildFile)

    // Verify dependencies detected
    assertTrue(bazel.dependencies.isNotEmpty(), "Should have dependencies from maven_install")

    // Common gRPC dependencies
    val depCoordinates = bazel.dependencies.map { it.coordinate }

    // Should have Guava
    assertTrue(
      depCoordinates.any { it.startsWith("com.google.guava:guava:") },
      "Should have Guava dependency"
    )

    // Should have Protobuf
    assertTrue(
      depCoordinates.any { it.contains("protobuf") },
      "Should have Protobuf dependency"
    )

    // Verify targets detected
    if (bazel.targets.isNotEmpty()) {
      // Should have java targets
      assertTrue(
        bazel.targets.any { it.rule.contains("java") },
        "Should have Java targets"
      )
    }
  }

  @Test
  fun testGeneratePklForGrpcJava() {
    // Skip if repo not cloned
    if (!grpcJavaPath.exists()) {
      return
    }

    val bazel = BazelParser.parse(grpcJavaPath)
    val pkl = PklGenerator.generate(bazel)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("name = \"grpc-java\""))

    // Verify dependencies section
    assertTrue(pkl.contains("dependencies {"))
    assertTrue(pkl.contains("maven {"))
    assertTrue(pkl.contains("packages {"))

    // Should have Guava in packages
    assertTrue(pkl.contains("com.google.guava:guava:"))

    // Verify source mappings
    assertTrue(pkl.contains("sources {"))

    // Should have Bazel targets listed as comments if any were parsed
    if (bazel.targets.isNotEmpty()) {
      assertTrue(pkl.contains("Bazel targets") || pkl.contains("targets:"))
    }

    // Basic PKL syntax validation
    assertTrue(pkl.contains("{"))
    assertTrue(pkl.contains("}"))
  }

  @Test
  fun testBazelDependencyParsing() {
    // Skip if repo not cloned
    if (!grpcJavaPath.exists()) {
      return
    }

    val bazel = BazelParser.parse(grpcJavaPath)

    // Verify dependencies have proper structure
    bazel.dependencies.forEach { dep ->
      assertFalse(dep.groupId().isEmpty(), "Dependency should have groupId: ${dep.coordinate}")
      assertFalse(dep.artifactId().isEmpty(), "Dependency should have artifactId: ${dep.coordinate}")
      // Version can be null for some dependencies
    }

    // At least some dependencies should have versions
    assertTrue(
      bazel.dependencies.any { it.version() != null },
      "Some dependencies should have versions"
    )
  }

  @Test
  fun testBazelTargetParsing() {
    // Skip if repo not cloned
    if (!grpcJavaPath.exists()) {
      return
    }

    val bazel = BazelParser.parse(grpcJavaPath)

    // If targets were parsed, validate their structure
    if (bazel.targets.isNotEmpty()) {
      bazel.targets.forEach { target ->
        assertFalse(target.name.isEmpty(), "Target should have name")
        assertFalse(target.rule.isEmpty(), "Target should have rule")
      }

      // Should identify test targets correctly
      val testTargets = bazel.targets.filter { it.isTestTarget() }
      if (testTargets.isNotEmpty()) {
        testTargets.forEach { target ->
          assertTrue(
            target.rule.contains("test", ignoreCase = true),
            "Test target should have 'test' in rule: ${target.rule}"
          )
        }
      }
    }
  }
}
