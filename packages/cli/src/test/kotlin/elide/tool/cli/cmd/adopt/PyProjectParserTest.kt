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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [PyProjectParser].
 */
class PyProjectParserTest {
  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `test parse basic pyproject toml`() {
    val pyprojectFile = tempDir.resolve("pyproject.toml")
    pyprojectFile.writeText(
      """
      [project]
      name = "my-app"
      version = "1.0.0"
      description = "My Python application"
      requires-python = ">=3.11"

      dependencies = [
          "fastapi>=0.104.0",
          "uvicorn[standard]>=0.24.0",
      ]
      """.trimIndent()
    )

    val descriptor = PyProjectParser.parse(pyprojectFile)

    assertEquals("my-app", descriptor.name)
    assertEquals("1.0.0", descriptor.version)
    assertEquals("My Python application", descriptor.description)
    assertEquals(">=3.11", descriptor.pythonVersion)
    assertEquals(2, descriptor.dependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
    assertTrue(descriptor.dependencies.contains("uvicorn[standard]>=0.24.0"))
    assertEquals(PythonDescriptor.SourceType.PYPROJECT_TOML, descriptor.sourceType)
  }

  @Test
  fun `test parse pyproject toml with optional dependencies`() {
    val pyprojectFile = tempDir.resolve("pyproject.toml")
    pyprojectFile.writeText(
      """
      [project]
      name = "my-app"
      version = "1.0.0"

      dependencies = [
          "fastapi>=0.104.0",
      ]

      [project.optional-dependencies]
      dev = [
          "pytest>=7.4.0",
          "black>=23.11.0",
      ]
      test = [
          "pytest-cov>=4.1.0",
      ]
      """.trimIndent()
    )

    val descriptor = PyProjectParser.parse(pyprojectFile)

    assertEquals("my-app", descriptor.name)
    assertEquals(1, descriptor.dependencies.size)
    assertEquals(2, descriptor.optionalDependencies.size)
    assertTrue(descriptor.optionalDependencies.containsKey("dev"))
    assertTrue(descriptor.optionalDependencies.containsKey("test"))
    assertEquals(2, descriptor.optionalDependencies["dev"]?.size)
    assertEquals(1, descriptor.optionalDependencies["test"]?.size)
  }

  @Test
  fun `test parse pyproject toml with scripts`() {
    val pyprojectFile = tempDir.resolve("pyproject.toml")
    pyprojectFile.writeText(
      """
      [project]
      name = "my-app"
      version = "1.0.0"

      dependencies = []

      [project.scripts]
      my-app = "my_app.main:run"
      my-cli = "my_app.cli:main"
      """.trimIndent()
    )

    val descriptor = PyProjectParser.parse(pyprojectFile)

    assertEquals("my-app", descriptor.name)
    assertEquals(2, descriptor.scripts.size)
    assertEquals("my_app.main:run", descriptor.scripts["my-app"])
    assertEquals("my_app.cli:main", descriptor.scripts["my-cli"])
    assertTrue(descriptor.hasScripts())
  }

  @Test
  fun `test parse pyproject toml with build system`() {
    val pyprojectFile = tempDir.resolve("pyproject.toml")
    pyprojectFile.writeText(
      """
      [project]
      name = "my-app"
      version = "1.0.0"
      dependencies = []

      [build-system]
      requires = ["setuptools>=61.0", "wheel"]
      build-backend = "setuptools.build_meta"
      """.trimIndent()
    )

    val descriptor = PyProjectParser.parse(pyprojectFile)

    assertEquals("my-app", descriptor.name)
    assertEquals("setuptools.build_meta", descriptor.buildSystem)
  }

  @Test
  fun `test extract dev dependencies`() {
    val descriptor = PythonDescriptor(
      name = "my-app",
      version = "1.0.0",
      dependencies = listOf("fastapi>=0.104.0"),
      optionalDependencies = mapOf(
        "dev" to listOf("pytest>=7.4.0", "black>=23.11.0"),
        "docs" to listOf("sphinx>=7.0.0"),
        "test" to listOf("pytest-cov>=4.1.0"),
      ),
    )

    val updated = PyProjectParser.extractDevDependencies(descriptor)

    assertEquals(3, updated.devDependencies.size) // dev + test
    assertTrue(updated.devDependencies.contains("pytest>=7.4.0"))
    assertTrue(updated.devDependencies.contains("black>=23.11.0"))
    assertTrue(updated.devDependencies.contains("pytest-cov>=4.1.0"))
    assertEquals(1, updated.optionalDependencies.size) // Only docs remains
    assertTrue(updated.optionalDependencies.containsKey("docs"))
  }

  @Test
  fun `test isValidPyProjectToml with valid file`() {
    val pyprojectFile = tempDir.resolve("pyproject.toml")
    pyprojectFile.writeText(
      """
      [project]
      name = "my-app"
      version = "1.0.0"
      dependencies = []
      """.trimIndent()
    )

    assertTrue(PyProjectParser.isValidPyProjectToml(pyprojectFile))
  }

  @Test
  fun `test isValidPyProjectToml with invalid file`() {
    val invalidFile = tempDir.resolve("pyproject.toml")
    invalidFile.writeText(
      """
      [tool.poetry]
      name = "my-app"
      """.trimIndent()
    )

    assertFalse(PyProjectParser.isValidPyProjectToml(invalidFile))
  }

  @Test
  fun `test parse minimal pyproject toml`() {
    val pyprojectFile = tempDir.resolve("pyproject.toml")
    pyprojectFile.writeText(
      """
      [project]
      name = "minimal-app"
      dependencies = []
      """.trimIndent()
    )

    val descriptor = PyProjectParser.parse(pyprojectFile)

    assertEquals("minimal-app", descriptor.name)
    assertEquals(null, descriptor.version)
    assertEquals(null, descriptor.description)
    assertEquals(null, descriptor.pythonVersion)
    assertEquals(0, descriptor.dependencies.size)
    assertFalse(descriptor.hasDevDependencies())
    assertFalse(descriptor.hasOptionalDependencies())
    assertFalse(descriptor.hasScripts())
  }

  @Test
  fun `test parse pyproject toml with metadata`() {
    val pyprojectFile = tempDir.resolve("pyproject.toml")
    pyprojectFile.writeText(
      """
      [project]
      name = "my-app"
      version = "1.0.0"
      description = "A sample application"
      readme = "README.md"
      keywords = ["python", "fastapi"]
      classifiers = [
          "Development Status :: 4 - Beta",
          "Programming Language :: Python :: 3.11",
      ]

      dependencies = []

      [[project.authors]]
      name = "John Doe"
      email = "john@example.com"

      [project.urls]
      Homepage = "https://example.com"
      Repository = "https://github.com/example/my-app"
      """.trimIndent()
    )

    val descriptor = PyProjectParser.parse(pyprojectFile)

    assertEquals("my-app", descriptor.name)
    assertEquals("1.0.0", descriptor.version)
    assertEquals("A sample application", descriptor.description)
  }

  @Test
  fun `test allDependencies includes production and dev`() {
    val descriptor = PythonDescriptor(
      name = "my-app",
      dependencies = listOf("fastapi>=0.104.0", "pydantic>=2.5.0"),
      devDependencies = listOf("pytest>=7.4.0", "black>=23.11.0"),
    )

    val allDeps = descriptor.allDependencies()

    assertEquals(4, allDeps.size)
    assertTrue(allDeps.contains("fastapi>=0.104.0"))
    assertTrue(allDeps.contains("pytest>=7.4.0"))
  }

  @Test
  fun `test allOptionalDependencies flattens all groups`() {
    val descriptor = PythonDescriptor(
      name = "my-app",
      optionalDependencies = mapOf(
        "docs" to listOf("sphinx>=7.0.0", "sphinx-rtd-theme>=2.0.0"),
        "api" to listOf("httpx>=0.25.0"),
      ),
    )

    val allOptional = descriptor.allOptionalDependencies()

    assertEquals(3, allOptional.size)
    assertTrue(allOptional.contains("sphinx>=7.0.0"))
    assertTrue(allOptional.contains("httpx>=0.25.0"))
  }

  @Test
  fun `test sourceDisplayName returns correct format`() {
    val descriptor = PythonDescriptor(
      name = "my-app",
      sourceType = PythonDescriptor.SourceType.PYPROJECT_TOML,
    )

    assertEquals("pyproject.toml", descriptor.sourceDisplayName())
  }

  @Test
  fun `test parse pyproject toml with complex dependencies`() {
    val pyprojectFile = tempDir.resolve("pyproject.toml")
    pyprojectFile.writeText(
      """
      [project]
      name = "complex-app"
      version = "2.1.0"
      requires-python = ">=3.11,<4.0"

      dependencies = [
          "fastapi>=0.104.0,<1.0.0",
          "uvicorn[standard]>=0.24.0",
          "pydantic>=2.5.0",
          "sqlalchemy>=2.0.0",
          "alembic>=1.13.0",
      ]

      [project.optional-dependencies]
      dev = [
          "pytest>=7.4.0",
          "pytest-cov>=4.1.0",
          "black>=23.11.0",
          "ruff>=0.1.0",
      ]
      docs = [
          "sphinx>=7.0.0",
          "sphinx-rtd-theme>=2.0.0",
      ]
      redis = [
          "redis>=5.0.0",
      ]
      """.trimIndent()
    )

    val descriptor = PyProjectParser.parse(pyprojectFile)

    assertEquals("complex-app", descriptor.name)
    assertEquals("2.1.0", descriptor.version)
    assertEquals(">=3.11,<4.0", descriptor.pythonVersion)
    assertEquals(5, descriptor.dependencies.size)
    assertEquals(3, descriptor.optionalDependencies.size)
    assertNotNull(descriptor.optionalDependencies["dev"])
    assertNotNull(descriptor.optionalDependencies["docs"])
    assertNotNull(descriptor.optionalDependencies["redis"])
  }
}
