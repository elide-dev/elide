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
import elide.tooling.project.adopt.node.NodeParser
import elide.tooling.project.adopt.PklGenerator

/**
 * Integration test for Express.js - validates our Node.js implementation works with a real-world project.
 *
 * This test uses the cloned Express repository at /tmp/express.
 * If the repository doesn't exist, the test is skipped.
 */
class ExpressNodeIntegrationTest {
  private val expressPath = Paths.get("/tmp/express/package.json")

  @BeforeTest
  fun checkPreconditions() {
    if (!expressPath.exists()) {
      println("Skipping Express Node.js integration test - repository not found at $expressPath")
      println("To enable this test, clone: git clone --depth 1 https://github.com/expressjs/express.git /tmp/express")
    }
  }

  @Test
  fun testParseExpressPackageJson() {
    // Skip if repo not cloned
    if (!expressPath.exists()) {
      return
    }

    val pkg = NodeParser.parse(expressPath)

    // Verify basic project info
    assertEquals("express", pkg.name)
    assertNotNull(pkg.version)
    assertNotNull(pkg.description)
    assertTrue(pkg.description?.contains("web", ignoreCase = true) ?: false)

    // Verify dependencies
    assertTrue(pkg.dependencies.isNotEmpty(), "Express should have dependencies")

    // Common Express dependencies
    val depNames = pkg.dependencies.keys

    // Should have body-parser or similar dependencies
    assertTrue(
      depNames.any { it.contains("body-parser") || it.contains("accepts") || it.contains("type-is") },
      "Should have common Express dependencies"
    )

    // Verify dev dependencies
    assertTrue(pkg.devDependencies.isNotEmpty(), "Express should have devDependencies")

    // Should have test framework
    assertTrue(
      pkg.devDependencies.keys.any { it.contains("mocha") || it.contains("jest") || it.contains("test") },
      "Should have test framework in devDependencies"
    )

    // Verify scripts
    assertTrue(pkg.scripts.isNotEmpty(), "Express should have NPM scripts")
    assertTrue(
      pkg.scripts.containsKey("test"),
      "Should have test script"
    )
  }

  @Test
  fun testGeneratePklForExpress() {
    // Skip if repo not cloned
    if (!expressPath.exists()) {
      return
    }

    val pkg = NodeParser.parse(expressPath)
    val pkl = PklGenerator.generate(pkg)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("name = \"express\""))
    assertTrue(pkl.contains("description = "))

    // Verify dependencies section
    assertTrue(pkl.contains("dependencies {"))
    assertTrue(pkl.contains("npm {"))
    assertTrue(pkl.contains("packages {"))

    // Production dependencies should be in packages
    val prodDeps = pkg.dependencies.keys.first()
    assertTrue(pkl.contains("\"$prodDeps@"), "Should have production dependency: $prodDeps")

    // Dev dependencies should be in testPackages
    if (pkg.devDependencies.isNotEmpty()) {
      assertTrue(pkl.contains("testPackages {"))
    }

    // Should have scripts documented as comments
    if (pkg.scripts.isNotEmpty()) {
      assertTrue(pkl.contains("// NPM scripts") || pkl.contains("scripts:"))
    }

    // Basic PKL syntax validation
    assertTrue(pkl.contains("{"))
    assertTrue(pkl.contains("}"))
  }

  @Test
  fun testVersionSpecNormalization() {
    // Skip if repo not cloned
    if (!expressPath.exists()) {
      return
    }

    val pkg = NodeParser.parse(expressPath)
    val pkl = PklGenerator.generate(pkg)

    // Verify version specs have ^ prefix
    pkg.dependencies.forEach { (name, version) ->
      if (version.matches(Regex("^\\d+\\.\\d+\\.\\d+$"))) {
        // Plain version should get ^ prefix in PKL
        assertTrue(
          pkl.contains("\"$name@^$version\"") || pkl.contains("\"$name@~$version\""),
          "Plain version should get prefix: $name@$version"
        )
      } else if (version.startsWith("^") || version.startsWith("~")) {
        // Already prefixed versions should be preserved
        assertTrue(
          pkl.contains("\"$name@$version\""),
          "Prefixed version should be preserved: $name@$version"
        )
      }
    }
  }

  @Test
  fun testPeerAndOptionalDependencies() {
    // Skip if repo not cloned
    if (!expressPath.exists()) {
      return
    }

    val pkg = NodeParser.parse(expressPath)

    // If Express has peer or optional dependencies, they should be parsed
    if (pkg.peerDependencies.isNotEmpty()) {
      val pkl = PklGenerator.generate(pkg)
      // Peer dependencies should be documented as comments, not in packages
      pkg.peerDependencies.keys.forEach { peerDep ->
        if (pkl.contains(peerDep)) {
          assertTrue(
            pkl.contains("// $peerDep") || pkl.contains("peerDependencies"),
            "Peer dependency should be in comments: $peerDep"
          )
        }
      }
    }

    if (pkg.optionalDependencies.isNotEmpty()) {
      val pkl = PklGenerator.generate(pkg)
      // Optional dependencies should be documented as comments
      pkg.optionalDependencies.keys.forEach { optDep ->
        if (pkl.contains(optDep)) {
          assertTrue(
            pkl.contains("// $optDep") || pkl.contains("optionalDependencies"),
            "Optional dependency should be in comments: $optDep"
          )
        }
      }
    }
  }

  @Test
  fun testScriptsDocumentation() {
    // Skip if repo not cloned
    if (!expressPath.exists()) {
      return
    }

    val pkg = NodeParser.parse(expressPath)
    val pkl = PklGenerator.generate(pkg)

    // Scripts should be documented if present
    if (pkg.scripts.isNotEmpty()) {
      // At least the test script should be mentioned
      if (pkg.scripts.containsKey("test")) {
        assertTrue(
          pkl.contains("test") || pkl.contains("NPM scripts"),
          "Test script should be documented"
        )
      }
    }
  }
}
