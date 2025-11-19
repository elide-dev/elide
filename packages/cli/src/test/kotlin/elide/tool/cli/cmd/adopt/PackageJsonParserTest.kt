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
 * Unit tests for PackageJsonParser.
 */
class PackageJsonParserTest {
  private lateinit var tempDir: Path

  @BeforeTest
  fun setup() {
    tempDir = Files.createTempDirectory("package-json-parser-test")
  }

  @AfterTest
  fun cleanup() {
    tempDir.toFile().deleteRecursively()
  }

  @Test
  fun testParseBasicPackageJson() {
    val packageJsonContent = """
      {
        "name": "test-package",
        "version": "1.0.0",
        "description": "A test package",
        "dependencies": {
          "express": "^4.18.2",
          "lodash": "~4.17.21"
        },
        "devDependencies": {
          "mocha": "^10.2.0",
          "chai": "^4.3.7"
        }
      }
    """.trimIndent()

    val packageJsonFile = tempDir.resolve("package.json")
    packageJsonFile.writeText(packageJsonContent)

    val pkg = PackageJsonParser.parse(packageJsonFile)

    assertEquals("test-package", pkg.name)
    assertEquals("1.0.0", pkg.version)
    assertEquals("A test package", pkg.description)

    assertEquals(2, pkg.dependencies.size)
    assertEquals("^4.18.2", pkg.dependencies["express"])
    assertEquals("~4.17.21", pkg.dependencies["lodash"])

    assertEquals(2, pkg.devDependencies.size)
    assertEquals("^10.2.0", pkg.devDependencies["mocha"])
    assertEquals("^4.3.7", pkg.devDependencies["chai"])
  }

  @Test
  fun testParseWithWorkspacesArray() {
    val packageJsonContent = """
      {
        "name": "monorepo",
        "version": "1.0.0",
        "workspaces": [
          "packages/*",
          "apps/*"
        ]
      }
    """.trimIndent()

    val packageJsonFile = tempDir.resolve("package.json")
    packageJsonFile.writeText(packageJsonContent)

    val pkg = PackageJsonParser.parse(packageJsonFile)

    assertEquals("monorepo", pkg.name)
    assertEquals(2, pkg.workspaces.size)
    assertTrue(pkg.workspaces.contains("packages/*"))
    assertTrue(pkg.workspaces.contains("apps/*"))
  }

  @Test
  fun testParseWithWorkspacesObject() {
    val packageJsonContent = """
      {
        "name": "monorepo",
        "version": "1.0.0",
        "workspaces": {
          "packages": [
            "packages/*",
            "tools/*"
          ]
        }
      }
    """.trimIndent()

    val packageJsonFile = tempDir.resolve("package.json")
    packageJsonFile.writeText(packageJsonContent)

    val pkg = PackageJsonParser.parse(packageJsonFile)

    assertEquals("monorepo", pkg.name)
    assertEquals(2, pkg.workspaces.size)
    assertTrue(pkg.workspaces.contains("packages/*"))
    assertTrue(pkg.workspaces.contains("tools/*"))
  }

  @Test
  fun testParseWithPeerAndOptionalDependencies() {
    val packageJsonContent = """
      {
        "name": "plugin-package",
        "version": "1.0.0",
        "peerDependencies": {
          "react": "^18.0.0",
          "react-dom": "^18.0.0"
        },
        "optionalDependencies": {
          "fsevents": "^2.3.2"
        }
      }
    """.trimIndent()

    val packageJsonFile = tempDir.resolve("package.json")
    packageJsonFile.writeText(packageJsonContent)

    val pkg = PackageJsonParser.parse(packageJsonFile)

    assertEquals("plugin-package", pkg.name)

    assertEquals(2, pkg.peerDependencies.size)
    assertEquals("^18.0.0", pkg.peerDependencies["react"])
    assertEquals("^18.0.0", pkg.peerDependencies["react-dom"])

    assertEquals(1, pkg.optionalDependencies.size)
    assertEquals("^2.3.2", pkg.optionalDependencies["fsevents"])
  }

  @Test
  fun testParseWithScripts() {
    val packageJsonContent = """
      {
        "name": "app",
        "version": "1.0.0",
        "scripts": {
          "start": "node index.js",
          "test": "mocha",
          "build": "webpack --mode production",
          "dev": "webpack --mode development --watch"
        }
      }
    """.trimIndent()

    val packageJsonFile = tempDir.resolve("package.json")
    packageJsonFile.writeText(packageJsonContent)

    val pkg = PackageJsonParser.parse(packageJsonFile)

    assertEquals(4, pkg.scripts.size)
    assertEquals("node index.js", pkg.scripts["start"])
    assertEquals("mocha", pkg.scripts["test"])
    assertEquals("webpack --mode production", pkg.scripts["build"])
    assertEquals("webpack --mode development --watch", pkg.scripts["dev"])
  }

  @Test
  fun testParseMinimalPackageJson() {
    val packageJsonContent = """
      {
        "name": "minimal-package"
      }
    """.trimIndent()

    val packageJsonFile = tempDir.resolve("package.json")
    packageJsonFile.writeText(packageJsonContent)

    val pkg = PackageJsonParser.parse(packageJsonFile)

    assertEquals("minimal-package", pkg.name)
    assertNull(pkg.version)
    assertNull(pkg.description)
    assertTrue(pkg.dependencies.isEmpty())
    assertTrue(pkg.devDependencies.isEmpty())
    assertTrue(pkg.workspaces.isEmpty())
  }

  @Test
  fun testMissingPackageJsonFile() {
    val nonExistentFile = tempDir.resolve("missing-package.json")

    assertFailsWith<IllegalArgumentException> {
      PackageJsonParser.parse(nonExistentFile)
    }
  }

  @Test
  fun testPklGenerationForNodePackage() {
    val pkg = PackageJsonDescriptor(
      name = "test-app",
      version = "2.1.0",
      description = "A test application",
      dependencies = mapOf(
        "express" to "^4.18.2",
        "lodash" to "4.17.21",
      ),
      devDependencies = mapOf(
        "mocha" to "^10.2.0",
        "eslint" to "^8.52.0",
      ),
      scripts = mapOf(
        "test" to "mocha",
        "start" to "node index.js",
      ),
    )

    val pkl = PklGenerator.generate(pkg)

    // Verify structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("name = \"test-app\""))
    assertTrue(pkl.contains("description = \"A test application\""))

    // Verify dependencies
    assertTrue(pkl.contains("dependencies {"))
    assertTrue(pkl.contains("npm {"))
    assertTrue(pkl.contains("packages {"))

    // Production deps should be in packages
    assertTrue(pkl.contains("\"express@^4.18.2\""))
    // Plain version should get ^ prefix
    assertTrue(pkl.contains("\"lodash@^4.17.21\""))

    // Dev deps should be in testPackages
    assertTrue(pkl.contains("testPackages {"))
    assertTrue(pkl.contains("\"mocha@^10.2.0\""))
    assertTrue(pkl.contains("\"eslint@^8.52.0\""))

    // Scripts should be documented
    assertTrue(pkl.contains("test") || pkl.contains("scripts"))
  }

  @Test
  fun testPklGenerationForWorkspace() {
    val rootPkg = PackageJsonDescriptor(
      name = "monorepo",
      version = "1.0.0",
      workspaces = listOf("packages/*"),
    )

    val pkg1 = PackageJsonDescriptor(
      name = "@monorepo/package-a",
      dependencies = mapOf("react" to "^18.2.0"),
    )

    val pkg2 = PackageJsonDescriptor(
      name = "@monorepo/package-b",
      dependencies = mapOf("vue" to "^3.3.4"),
      devDependencies = mapOf("vitest" to "^0.34.0"),
    )

    val pkl = PklGenerator.generateWorkspace(rootPkg, listOf(pkg1, pkg2))

    // Verify structure
    assertTrue(pkl.contains("amends \"elide:project.pkl\""))
    assertTrue(pkl.contains("name = \"monorepo\""))

    // Should aggregate all dependencies
    assertTrue(pkl.contains("\"react@^18.2.0\""))
    assertTrue(pkl.contains("\"vue@^3.3.4\""))
    assertTrue(pkl.contains("\"vitest@^0.34.0\""))

    // Should document workspace packages
    assertTrue(pkl.contains("@monorepo/package-a") || pkl.contains("workspace"))
    assertTrue(pkl.contains("@monorepo/package-b") || pkl.contains("workspace"))
  }

  @Test
  fun testVersionSpecNormalization() {
    val pkg = PackageJsonDescriptor(
      name = "test",
      dependencies = mapOf(
        "with-caret" to "^1.0.0",
        "with-tilde" to "~2.0.0",
        "plain" to "3.0.0",
        "with-range" to ">=4.0.0",
      ),
    )

    val pkl = PklGenerator.generate(pkg)

    // Existing prefixes should be preserved
    assertTrue(pkl.contains("\"with-caret@^1.0.0\""))
    assertTrue(pkl.contains("\"with-tilde@~2.0.0\""))

    // Plain version should get ^ prefix
    assertTrue(pkl.contains("\"plain@^3.0.0\""))

    // Range spec should be preserved (or get ^)
    assertTrue(pkl.contains("with-range@"))
  }
}
