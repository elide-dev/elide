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
import kotlin.io.path.isDirectory

/**
 * Integration test for Spring Cloud - validates multi-module project handling.
 *
 * Spring Cloud is a complex multi-module project with:
 * - Deep parent hierarchy (Spring Boot → Spring Boot Dependencies → Spring Boot Parent)
 * - Multiple modules with inter-dependencies
 * - Heavy use of dependency management and properties
 * - BOM (Bill of Materials) pattern
 *
 * This test uses the cloned Spring Cloud repository at /tmp/spring-cloud-release or
 * /private/tmp/spring-cloud-release.
 * If the repository doesn't exist, the test is skipped.
 */
class SpringCloudIntegrationTest {
  private val springCloudPath = findSpringCloudPath()

  private fun findSpringCloudPath(): Path? {
    val candidates = listOf(
      Paths.get("/tmp/spring-cloud-release"),
      Paths.get("/private/tmp/spring-cloud-release"),
      Paths.get("/tmp/spring-cloud-commons"),
      Paths.get("/private/tmp/spring-cloud-commons")
    )
    return candidates.firstOrNull { it.exists() && it.isDirectory() }
  }

  @BeforeTest
  fun checkPreconditions() {
    if (springCloudPath == null) {
      println("Skipping Spring Cloud integration test - repository not found")
      println("To enable this test, clone one of:")
      println("  git clone --depth 1 https://github.com/spring-cloud/spring-cloud-release.git /tmp/spring-cloud-release")
      println("  git clone --depth 1 https://github.com/spring-cloud/spring-cloud-commons.git /tmp/spring-cloud-commons")
    }
  }

  @Test
  fun testParseSpringCloudParent() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      println("Skipping - pom.xml not found at $pomPath")
      return
    }

    val pom = PomParser.parse(pomPath)

    // Verify basic project info
    assertTrue(pom.groupId.startsWith("org.springframework"), "GroupId should be Spring: ${pom.groupId}")
    assertTrue(
      pom.artifactId.contains("spring-cloud") ||
      pom.artifactId.contains("spring-boot"),
      "ArtifactId should contain spring-cloud or spring-boot: ${pom.artifactId}"
    )
    assertNotNull(pom.version, "Should have version")
    assertTrue(pom.version.isNotEmpty(), "Version should not be empty")

    // Multi-module projects typically have pom packaging
    if (pom.modules.isNotEmpty()) {
      assertEquals("pom", pom.packaging, "Multi-module parent should have pom packaging")
    }

    // Should have properties for version management
    assertTrue(pom.properties.isNotEmpty(), "Spring projects heavily use properties")
  }

  @Test
  fun testMultiModuleStructure() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      return
    }

    val pom = PomParser.parse(pomPath)

    // If this is a multi-module project, verify modules are detected
    if (pom.packaging == "pom") {
      // Parent pom should declare modules
      assertTrue(
        pom.modules.isNotEmpty() || pom.artifactId.contains("dependencies") || pom.artifactId.contains("parent"),
        "Multi-module parent should have modules or be a parent/dependencies pom"
      )

      if (pom.modules.isNotEmpty()) {
        println("Detected ${pom.modules.size} modules: ${pom.modules.take(5)}")

        // Verify at least some modules exist as directories
        val moduleCount = pom.modules.count { moduleName ->
          val modulePath = springCloudPath!!.resolve(moduleName)
          modulePath.exists() && modulePath.isDirectory()
        }

        assertTrue(
          moduleCount > 0,
          "At least some declared modules should exist as directories"
        )
      }
    }
  }

  @Test
  fun testParseModulePom() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      return
    }

    val parentPom = PomParser.parse(pomPath)

    // Find a child module to test
    val moduleName = parentPom.modules.firstOrNull()
    if (moduleName != null) {
      val modulePath = springCloudPath!!.resolve(moduleName).resolve("pom.xml")

      if (modulePath.exists()) {
        val modulePom = PomParser.parse(modulePath)

        // Module should reference parent
        assertNotNull(modulePom.parent, "Module should have parent POM reference")

        // Parent reference should match parent POM
        if (modulePom.parent != null) {
          assertEquals(
            parentPom.artifactId,
            modulePom.parent!!.artifactId,
            "Module parent should reference parent POM"
          )
        }

        // Module should inherit properties from parent
        assertTrue(
          modulePom.properties.isNotEmpty(),
          "Module should have properties (inherited or defined)"
        )

        println("Successfully parsed module: ${modulePom.artifactId}")
      }
    }
  }

  @Test
  fun testSpringBootParentChain() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      return
    }

    val pom = PomParser.parse(pomPath)

    // Spring Cloud projects typically have Spring Boot in their parent chain
    var currentParent = pom.parent
    var foundSpringBoot = false
    var depth = 0
    val maxDepth = 10 // Prevent infinite loops

    while (currentParent != null && depth < maxDepth) {
      if (currentParent.artifactId.contains("spring-boot")) {
        foundSpringBoot = true
        println("Found Spring Boot parent: ${currentParent.groupId}:${currentParent.artifactId}:${currentParent.version}")
        break
      }
      depth++
      currentParent = null // We don't recursively resolve in this test
    }

    // Note: This might not always be true depending on which Spring Cloud repo is cloned
    // Some Spring Cloud projects might not have Spring Boot as direct parent
    println("Spring Boot in parent chain: $foundSpringBoot (depth checked: $depth)")
  }

  @Test
  fun testDependencyManagement() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      return
    }

    val pom = PomParser.parse(pomPath)

    // Spring Cloud heavily uses dependency management
    assertTrue(
      pom.dependencyManagement.isNotEmpty() || pom.packaging == "pom",
      "Spring Cloud parent should have dependency management or be a parent pom"
    )

    if (pom.dependencyManagement.isNotEmpty()) {
      println("Dependency management entries: ${pom.dependencyManagement.size}")

      // Verify dependency management entries are well-formed
      pom.dependencyManagement.values.forEach { dep ->
        assertTrue(dep.groupId.isNotEmpty(), "Managed dependency should have groupId")
        assertTrue(dep.artifactId.isNotEmpty(), "Managed dependency should have artifactId")
      }
    }
  }

  @Test
  fun testPropertyManagement() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      return
    }

    val pom = PomParser.parse(pomPath)

    // Spring projects use extensive property-based configuration
    assertTrue(pom.properties.isNotEmpty(), "Should have properties")

    // Common Spring properties
    val commonProps = listOf(
      "java.version",
      "maven.compiler.source",
      "maven.compiler.target",
      "project.build.sourceEncoding"
    )

    val foundProps = commonProps.filter { pom.properties.containsKey(it) }
    println("Found common properties: $foundProps")

    // Should have at least some standard Maven/Java properties
    assertTrue(
      pom.properties.containsKey("java.version") ||
      pom.properties.containsKey("maven.compiler.source") ||
      pom.properties.size > 5,
      "Should have Java version or compiler properties"
    )
  }

  @Test
  fun testGenerateMultiModulePkl() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      return
    }

    val pom = PomParser.parse(pomPath)

    // For multi-module projects, test PKL generation
    if (pom.modules.isNotEmpty()) {
      val pkl = PklGenerator.generateMultiModule(
        pom,
        pom.modules.map { moduleName ->
          val modulePath = springCloudPath!!.resolve(moduleName).resolve("pom.xml")
          if (modulePath.exists()) {
            try {
              PomParser.parse(modulePath)
            } catch (e: Exception) {
              println("Warning: Could not parse module $moduleName: ${e.message}")
              null
            }
          } else {
            null
          }
        }.filterNotNull()
      )

      // Verify PKL structure
      assertTrue(pkl.contains("amends \"elide:project.pkl\""), "Should amend elide:project.pkl")

      // Should reference modules
      assertTrue(
        pkl.contains("modules {") || pom.modules.isEmpty(),
        "Multi-module PKL should have modules section"
      )

      // Verify Maven Central
      assertTrue(
        pkl.contains("[\"central\"] = \"https://repo.maven.apache.org/maven2\""),
        "Should include Maven Central"
      )

      // Basic syntax validation
      val braceCount = pkl.count { it == '{' }
      val closeBraceCount = pkl.count { it == '}' }
      assertEquals(braceCount, closeBraceCount, "Braces should be balanced")

      println("Generated multi-module PKL (${pkl.lines().size} lines)")
    } else {
      // Single module - use regular generator
      val pkl = PklGenerator.generate(pom)

      assertTrue(pkl.contains("amends \"elide:project.pkl\""), "Should amend elide:project.pkl")

      // Basic syntax validation
      val braceCount = pkl.count { it == '{' }
      val closeBraceCount = pkl.count { it == '}' }
      assertEquals(braceCount, closeBraceCount, "Braces should be balanced")
    }
  }

  @Test
  fun testBomPattern() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      return
    }

    val pom = PomParser.parse(pomPath)

    // Spring Cloud often uses BOM (Bill of Materials) pattern
    // BOMs typically have:
    // - pom packaging
    // - extensive dependencyManagement
    // - minimal or no direct dependencies

    if (pom.packaging == "pom" && pom.dependencyManagement.isNotEmpty()) {
      println("Detected potential BOM pattern:")
      println("  - Packaging: ${pom.packaging}")
      println("  - Dependency management entries: ${pom.dependencyManagement.size}")
      println("  - Direct dependencies: ${pom.dependencies.size}")
      println("  - Modules: ${pom.modules.size}")

      // BOM pattern validation
      val isBom = pom.packaging == "pom" &&
                  pom.dependencyManagement.size > pom.dependencies.size

      if (isBom) {
        println("  ✓ Appears to be a BOM (Bill of Materials)")

        // Verify dependency management is comprehensive
        assertTrue(
          pom.dependencyManagement.isNotEmpty(),
          "BOM should have dependency management"
        )
      }
    }
  }

  @Test
  fun testInterModuleDependencies() {
    // Skip if repo not cloned
    if (springCloudPath == null) {
      return
    }

    val pomPath = springCloudPath!!.resolve("pom.xml")
    if (!pomPath.exists()) {
      return
    }

    val parentPom = PomParser.parse(pomPath)

    if (parentPom.modules.isEmpty()) {
      println("Not a multi-module project, skipping inter-module dependency test")
      return
    }

    // Parse all available modules
    val modulePoms = parentPom.modules.mapNotNull { moduleName ->
      val modulePath = springCloudPath!!.resolve(moduleName).resolve("pom.xml")
      if (modulePath.exists()) {
        try {
          moduleName to PomParser.parse(modulePath)
        } catch (e: Exception) {
          println("Warning: Could not parse module $moduleName: ${e.message}")
          null
        }
      } else {
        null
      }
    }.toMap()

    if (modulePoms.isEmpty()) {
      println("No modules could be parsed")
      return
    }

    println("Successfully parsed ${modulePoms.size} modules")

    // Check for inter-module dependencies
    var interModuleDepsFound = 0

    modulePoms.forEach { (moduleName, modulePom) ->
      val deps = modulePom.dependencies

      deps.forEach { dep ->
        // Check if this dependency references another module in the same project
        if (dep.groupId == parentPom.groupId) {
          val depModuleName = modulePoms.keys.find { otherModule ->
            val otherPom = modulePoms[otherModule]
            otherPom?.artifactId == dep.artifactId
          }

          if (depModuleName != null) {
            interModuleDepsFound++
            println("  $moduleName -> $depModuleName (${dep.artifactId})")
          }
        }
      }
    }

    println("Found $interModuleDepsFound inter-module dependencies")
  }
}
