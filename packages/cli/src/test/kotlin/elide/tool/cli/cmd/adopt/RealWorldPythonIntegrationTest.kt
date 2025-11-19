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
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * Real-world integration tests for Python projects.
 *
 * These tests validate the Python adopter against actual open-source projects.
 * Tests are skipped if the repositories are not cloned locally.
 *
 * To enable these tests, run the setup script:
 * ```bash
 * ./scripts/setup-real-world-tests.sh
 * ```
 *
 * Or manually clone:
 * ```bash
 * git clone --depth 1 https://github.com/tiangolo/fastapi.git /tmp/elide-real-world-tests/fastapi
 * git clone --depth 1 https://github.com/psf/requests.git /tmp/elide-real-world-tests/requests
 * git clone --depth 1 https://github.com/psf/black.git /tmp/elide-real-world-tests/black
 * git clone --depth 1 https://github.com/HypothesisWorks/hypothesis.git /tmp/elide-real-world-tests/hypothesis
 * ```
 */
class RealWorldPythonIntegrationTest {

  /**
   * Test FastAPI - modern Python web framework using pyproject.toml
   */
  @Test
  fun testFastAPIWithPyprojectToml() {
    val pyprojectPath = Paths.get("/tmp/elide-real-world-tests/fastapi/pyproject.toml")

    if (!pyprojectPath.exists()) {
      println("Skipping FastAPI test - repository not found")
      println("To enable: ./scripts/setup-real-world-tests.sh")
      return
    }

    // Parse the pyproject.toml
    val descriptor = PyProjectParser.parse(pyprojectPath)

    // Verify basic metadata
    assertEquals("fastapi", descriptor.name)
    assertNotNull(descriptor.version)
    assertNotNull(descriptor.description)

    // Verify Python version requirement
    assertNotNull(descriptor.pythonVersion)

    // Verify dependencies exist
    assertTrue(descriptor.dependencies.isNotEmpty(), "FastAPI should have dependencies")

    // FastAPI should have starlette and pydantic
    assertTrue(
      descriptor.dependencies.any { it.contains("starlette") || it.contains("Starlette") },
      "FastAPI should depend on Starlette"
    )
    assertTrue(
      descriptor.dependencies.any { it.contains("pydantic") || it.contains("Pydantic") },
      "FastAPI should depend on Pydantic"
    )

    // Generate PKL
    val pkl = PklGenerator.generateFromPython(descriptor)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("name = \"fastapi\""))
    assertTrue(pkl.contains("pypi {"))
    assertTrue(pkl.contains("packages {"))

    // Should reference pyproject.toml in header
    assertTrue(pkl.contains("pyproject.toml"))

    println("✓ FastAPI test passed - parsed ${descriptor.dependencies.size} dependencies")
  }

  /**
   * Test Requests - popular HTTP library (may use setup.py or pyproject.toml depending on version)
   */
  @Test
  fun testRequestsLibrary() {
    val pyprojectPath = Paths.get("/tmp/elide-real-world-tests/requests/pyproject.toml")
    val requirementsPath = Paths.get("/tmp/elide-real-world-tests/requests/requirements.txt")

    if (!pyprojectPath.exists() && !requirementsPath.exists()) {
      println("Skipping Requests test - repository not found")
      println("To enable: ./scripts/setup-real-world-tests.sh")
      return
    }

    // Try pyproject.toml first, fall back to requirements.txt
    val descriptor = when {
      pyprojectPath.exists() -> {
        println("Testing Requests with pyproject.toml")
        PyProjectParser.parse(pyprojectPath)
      }
      requirementsPath.exists() -> {
        println("Testing Requests with requirements.txt")
        RequirementsTxtParser.parse(requirementsPath)
      }
      else -> {
        println("No supported Python project files found")
        return
      }
    }

    // Verify basic metadata
    assertTrue(descriptor.name.contains("requests", ignoreCase = true) || descriptor.name.isEmpty())

    // Requests should have some dependencies (certifi, charset-normalizer, idna, urllib3)
    assertTrue(descriptor.dependencies.isNotEmpty() || descriptor.devDependencies.isNotEmpty(),
      "Requests should have dependencies")

    // Generate PKL
    val pkl = PklGenerator.generateFromPython(descriptor)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("pypi {"))

    println("✓ Requests test passed - parsed ${descriptor.dependencies.size} runtime + ${descriptor.devDependencies.size} dev dependencies")
  }

  /**
   * Test Black - Python code formatter with pyproject.toml
   */
  @Test
  fun testBlackCodeFormatter() {
    val pyprojectPath = Paths.get("/tmp/elide-real-world-tests/black/pyproject.toml")

    if (!pyprojectPath.exists()) {
      println("Skipping Black test - repository not found")
      println("To enable: ./scripts/setup-real-world-tests.sh")
      return
    }

    // Parse the pyproject.toml
    val descriptor = PyProjectParser.parse(pyprojectPath)

    // Verify basic metadata
    assertEquals("black", descriptor.name)
    assertNotNull(descriptor.version)

    // Black has optional dependencies for different features
    // Should have base dependencies
    assertTrue(descriptor.dependencies.isNotEmpty(), "Black should have dependencies")

    // Verify optional dependencies
    // Black typically has extras like: colorama, d, jupyter, uvloop
    assertTrue(descriptor.optionalDependencies.isNotEmpty(),
      "Black should have optional dependencies (extras)")

    // Generate PKL
    val pkl = PklGenerator.generateFromPython(descriptor)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("name = \"black\""))

    // Verify optional dependencies are documented in comments
    if (descriptor.optionalDependencies.isNotEmpty()) {
      assertTrue(pkl.contains("Optional dependencies") || pkl.contains("extras"))
    }

    println("✓ Black test passed - ${descriptor.dependencies.size} deps, ${descriptor.optionalDependencies.size} optional groups")
  }

  /**
   * Test Hypothesis - property-based testing library
   */
  @Test
  fun testHypothesisWithComplexDependencies() {
    val pyprojectPath = Paths.get("/tmp/elide-real-world-tests/hypothesis/hypothesis-python/pyproject.toml")

    if (!pyprojectPath.exists()) {
      println("Skipping Hypothesis test - repository not found")
      println("To enable: ./scripts/setup-real-world-tests.sh")
      return
    }

    // Parse the pyproject.toml
    val descriptor = PyProjectParser.parse(pyprojectPath)

    // Verify basic metadata
    assertTrue(descriptor.name.contains("hypothesis", ignoreCase = true))

    // Hypothesis has many optional dependencies for different integrations
    assertTrue(descriptor.dependencies.isNotEmpty() || descriptor.optionalDependencies.isNotEmpty())

    // Generate PKL
    val pkl = PklGenerator.generateFromPython(descriptor)

    // Verify PKL structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("pypi {"))

    println("✓ Hypothesis test passed")
  }

  /**
   * Test a Django project structure
   */
  @Test
  fun testDjangoProject() {
    val requirementsPath = Paths.get("/tmp/django-project/requirements.txt")

    if (!requirementsPath.exists()) {
      println("Skipping Django project test - no test project found")
      println("To enable: create a Django project at /tmp/django-project")
      return
    }

    // Parse requirements.txt
    val descriptor = RequirementsTxtParser.parse(requirementsPath)

    // Should have Django
    assertTrue(
      descriptor.dependencies.any { it.contains("Django", ignoreCase = true) } ||
      descriptor.devDependencies.any { it.contains("Django", ignoreCase = true) },
      "Should have Django dependency"
    )

    // Generate PKL
    val pkl = PklGenerator.generateFromPython(descriptor)

    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("pypi {"))

    println("✓ Django project test passed")
  }
}
