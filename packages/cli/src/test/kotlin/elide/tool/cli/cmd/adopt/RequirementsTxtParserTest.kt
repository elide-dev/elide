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
 * Tests for [RequirementsTxtParser].
 */
class RequirementsTxtParserTest {
  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `test parse basic requirements txt`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      fastapi>=0.104.0
      uvicorn[standard]>=0.24.0
      pydantic>=2.5.0
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals("test-app", descriptor.name)
    assertEquals(3, descriptor.dependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
    assertTrue(descriptor.dependencies.contains("uvicorn[standard]>=0.24.0"))
    assertTrue(descriptor.dependencies.contains("pydantic>=2.5.0"))
    assertEquals(PythonDescriptor.SourceType.REQUIREMENTS_TXT, descriptor.sourceType)
  }

  @Test
  fun `test parse requirements txt with comments`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      # Production dependencies
      fastapi>=0.104.0
      uvicorn>=0.24.0  # ASGI server

      # Database
      sqlalchemy>=2.0.0
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(3, descriptor.dependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
    assertTrue(descriptor.dependencies.contains("uvicorn>=0.24.0"))
    assertTrue(descriptor.dependencies.contains("sqlalchemy>=2.0.0"))
  }

  @Test
  fun `test parse requirements txt with dev dependencies`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      fastapi>=0.104.0
      pytest>=7.4.0  # dev
      black>=23.11.0  # dev
      ruff>=0.1.0  # test
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(1, descriptor.dependencies.size)
    assertEquals(3, descriptor.devDependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
    assertTrue(descriptor.devDependencies.contains("pytest>=7.4.0"))
    assertTrue(descriptor.devDependencies.contains("black>=23.11.0"))
    assertTrue(descriptor.devDependencies.contains("ruff>=0.1.0"))
  }

  @Test
  fun `test parse requirements txt with environment markers`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      fastapi>=0.104.0
      dataclasses>=0.6; python_version<'3.7'
      typing-extensions>=4.0; python_version<'3.10'
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(3, descriptor.dependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
    assertTrue(descriptor.dependencies.contains("dataclasses>=0.6"))
    assertTrue(descriptor.dependencies.contains("typing-extensions>=4.0"))
  }

  @Test
  fun `test parse requirements txt with includes`() {
    val baseFile = tempDir.resolve("requirements.txt")
    val prodFile = tempDir.resolve("requirements-prod.txt")

    prodFile.writeText(
      """
      fastapi>=0.104.0
      uvicorn>=0.24.0
      """.trimIndent()
    )

    baseFile.writeText(
      """
      -r requirements-prod.txt
      pytest>=7.4.0  # dev
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(baseFile, "test-app")

    assertEquals(2, descriptor.dependencies.size)
    assertEquals(1, descriptor.devDependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
    assertTrue(descriptor.dependencies.contains("uvicorn>=0.24.0"))
    assertTrue(descriptor.devDependencies.contains("pytest>=7.4.0"))
  }

  @Test
  fun `test parse requirements txt skips URLs`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      fastapi>=0.104.0
      https://github.com/user/repo/archive/master.zip
      git+https://github.com/user/repo.git@main
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(1, descriptor.dependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
  }

  @Test
  fun `test parse requirements txt skips editable installs`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      fastapi>=0.104.0
      -e ./local-package
      --editable ../another-package
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(1, descriptor.dependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
  }

  @Test
  fun `test isRequirementsFile detects valid files`() {
    assertTrue(RequirementsTxtParser.isRequirementsFile(Path.of("requirements.txt")))
    assertTrue(RequirementsTxtParser.isRequirementsFile(Path.of("requirements-dev.txt")))
    assertTrue(RequirementsTxtParser.isRequirementsFile(Path.of("requirements-test.txt")))
    assertTrue(RequirementsTxtParser.isRequirementsFile(Path.of("REQUIREMENTS.TXT")))
    assertFalse(RequirementsTxtParser.isRequirementsFile(Path.of("setup.py")))
    assertFalse(RequirementsTxtParser.isRequirementsFile(Path.of("pyproject.toml")))
  }

  @Test
  fun `test findRequirementsFiles finds common files`() {
    tempDir.resolve("requirements.txt").writeText("fastapi>=0.104.0")
    tempDir.resolve("requirements-dev.txt").writeText("pytest>=7.4.0")
    tempDir.resolve("other.txt").writeText("not a requirement")

    val found = RequirementsTxtParser.findRequirementsFiles(tempDir)

    assertTrue(found.size >= 2)
    assertTrue(found.any { it.fileName.toString() == "requirements.txt" })
    assertTrue(found.any { it.fileName.toString() == "requirements-dev.txt" })
  }

  @Test
  fun `test parseMultiple combines prod and dev files`() {
    val prodFile = tempDir.resolve("requirements.txt")
    val devFile = tempDir.resolve("requirements-dev.txt")

    prodFile.writeText(
      """
      fastapi>=0.104.0
      uvicorn>=0.24.0
      """.trimIndent()
    )

    devFile.writeText(
      """
      pytest>=7.4.0
      black>=23.11.0
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parseMultiple(prodFile, devFile, "test-app")

    assertEquals(2, descriptor.dependencies.size)
    assertEquals(2, descriptor.devDependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi>=0.104.0"))
    assertTrue(descriptor.dependencies.contains("uvicorn>=0.24.0"))
    assertTrue(descriptor.devDependencies.contains("pytest>=7.4.0"))
    assertTrue(descriptor.devDependencies.contains("black>=23.11.0"))
  }

  @Test
  fun `test detectAndParse finds requirements automatically`() {
    tempDir.resolve("requirements.txt").writeText("fastapi>=0.104.0")

    val descriptor = RequirementsTxtParser.detectAndParse(tempDir, "auto-app")

    assertNotNull(descriptor)
    assertEquals("auto-app", descriptor.name)
    assertEquals(1, descriptor.dependencies.size)
  }

  @Test
  fun `test detectAndParse finds dev requirements`() {
    tempDir.resolve("requirements.txt").writeText("fastapi>=0.104.0")
    tempDir.resolve("requirements-dev.txt").writeText("pytest>=7.4.0")

    val descriptor = RequirementsTxtParser.detectAndParse(tempDir, "auto-app")

    assertNotNull(descriptor)
    assertEquals(1, descriptor.dependencies.size)
    assertEquals(1, descriptor.devDependencies.size)
  }

  @Test
  fun `test parse with version specifiers`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      fastapi==0.104.0
      uvicorn>=0.24.0
      pydantic~=2.5.0
      requests!=2.28.0
      httpx>0.25.0
      attrs<24.0.0
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(6, descriptor.dependencies.size)
    assertTrue(descriptor.dependencies.contains("fastapi==0.104.0"))
    assertTrue(descriptor.dependencies.contains("uvicorn>=0.24.0"))
    assertTrue(descriptor.dependencies.contains("pydantic~=2.5.0"))
    assertTrue(descriptor.dependencies.contains("requests!=2.28.0"))
    assertTrue(descriptor.dependencies.contains("httpx>0.25.0"))
    assertTrue(descriptor.dependencies.contains("attrs<24.0.0"))
  }

  @Test
  fun `test parse with extras`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      uvicorn[standard]>=0.24.0
      requests[security,socks]>=2.31.0
      sqlalchemy[asyncio]>=2.0.0
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(3, descriptor.dependencies.size)
    assertTrue(descriptor.dependencies.contains("uvicorn[standard]>=0.24.0"))
    assertTrue(descriptor.dependencies.contains("requests[security,socks]>=2.31.0"))
    assertTrue(descriptor.dependencies.contains("sqlalchemy[asyncio]>=2.0.0"))
  }

  @Test
  fun `test parse removes duplicates`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      fastapi>=0.104.0
      fastapi>=0.104.0
      uvicorn>=0.24.0
      uvicorn>=0.24.0
      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(2, descriptor.dependencies.size)
  }

  @Test
  fun `test parse handles blank lines`() {
    val reqFile = tempDir.resolve("requirements.txt")
    reqFile.writeText(
      """
      fastapi>=0.104.0


      uvicorn>=0.24.0

      """.trimIndent()
    )

    val descriptor = RequirementsTxtParser.parse(reqFile, "test-app")

    assertEquals(2, descriptor.dependencies.size)
  }

  @Test
  fun `test parse defaults project name to directory`() {
    val projectDir = tempDir.resolve("my-awesome-project")
    projectDir.toFile().mkdirs()
    val reqFile = projectDir.resolve("requirements.txt")
    reqFile.writeText("fastapi>=0.104.0")

    val descriptor = RequirementsTxtParser.parse(reqFile)

    assertEquals("my-awesome-project", descriptor.name)
  }
}
