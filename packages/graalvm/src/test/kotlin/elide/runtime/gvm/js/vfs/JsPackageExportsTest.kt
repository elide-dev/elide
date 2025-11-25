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
@file:Suppress("JSFileReferences", "JSUnresolvedFunction", "NpmUsedModulesInstalled")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.js.vfs

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.vfs.HostVFSImpl
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.gvm.vfs.HostVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.util.UUID

/**
 * Tests for ESM-style import calls that resolve via package.json "exports" field.
 *
 * This tests the fix for nested conditional exports which GraalJS doesn't support natively.
 *
 * @see <a href="https://github.com/elide-dev/elide/issues/1793">Issue #1793</a>
 * @see <a href="https://nodejs.org/api/packages.html#conditional-exports">Node.js Conditional Exports</a>
 */
@TestCase internal class JsPackageExportsTest : AbstractJsTest() {
  private fun tempHostFs() = HostVFS.scopedTo(
    Files.createTempDirectory("elide-vfs-${UUID.random()}").toAbsolutePath().toString(),
    writable = true,
  ) as HostVFSImpl

  /**
   * Test: Import from a package with simple string exports.
   *
   * package.json: { "exports": "./dist/index.mjs" }
   */
  @Test fun testSimpleStringExports() {
    val fs = tempHostFs()
    fs.setCurrentWorkingDirectory(Path("/"))
    fs.createDirectory(fs.getPath("node_modules"))
    fs.createDirectory(fs.getPath("node_modules/simple-exports"))
    fs.createDirectory(fs.getPath("node_modules/simple-exports/dist"))

    // Create package.json with simple string exports
    val configPath = fs.getPath("node_modules/simple-exports/package.json")
    fs.writeStream(configPath).use { stream ->
      stream.write("""
        {
          "name": "simple-exports",
          "version": "1.0.0",
          "exports": "./dist/index.mjs"
        }
      """.trimIndent().toByteArray())
    }

    // Create the module file
    val modulePath = fs.getPath("node_modules/simple-exports/dist/index.mjs")
    fs.writeStream(modulePath).use { stream ->
      stream.write("export const value = 'simple-string-export';".toByteArray())
    }

    // Create root package.json
    val pkgPath = fs.getPath("package.json")
    fs.writeStream(pkgPath).use { stream ->
      stream.write("""{"name": "test", "type": "module"}""".toByteArray())
    }

    withHostFs(fs) {
      // language=javascript
      """
        import { value } from "simple-exports";
        test(value).isEqualTo("simple-string-export");
      """
    }.doesNotFail()
  }

  /**
   * Test: Import from a package with flat conditional exports.
   *
   * package.json: { "exports": { "import": "./dist/index.mjs", "require": "./dist/index.js" } }
   */
  @Test fun testFlatConditionalExports() {
    val fs = tempHostFs()
    fs.setCurrentWorkingDirectory(Path("/"))
    fs.createDirectory(fs.getPath("node_modules"))
    fs.createDirectory(fs.getPath("node_modules/flat-conditional"))
    fs.createDirectory(fs.getPath("node_modules/flat-conditional/dist"))

    // Create package.json with flat conditional exports
    val configPath = fs.getPath("node_modules/flat-conditional/package.json")
    fs.writeStream(configPath).use { stream ->
      stream.write("""
        {
          "name": "flat-conditional",
          "version": "1.0.0",
          "exports": {
            "import": "./dist/index.mjs",
            "require": "./dist/index.cjs",
            "default": "./dist/index.mjs"
          }
        }
      """.trimIndent().toByteArray())
    }

    // Create the ESM module file (should be selected for import)
    val modulePath = fs.getPath("node_modules/flat-conditional/dist/index.mjs")
    fs.writeStream(modulePath).use { stream ->
      stream.write("export const value = 'flat-conditional-esm';".toByteArray())
    }

    // Create root package.json
    val pkgPath = fs.getPath("package.json")
    fs.writeStream(pkgPath).use { stream ->
      stream.write("""{"name": "test", "type": "module"}""".toByteArray())
    }

    withHostFs(fs) {
      // language=javascript
      """
        import { value } from "flat-conditional";
        test(value).isEqualTo("flat-conditional-esm");
      """
    }.doesNotFail()
  }

  /**
   * Test: Import from a package with nested conditional exports (the main fix).
   *
   * This is the pattern used by `@discordjs/*` packages that caused the original issue.
   *
   * package.json:
   * {
   *   "exports": {
   *     ".": {
   *       "import": {
   *         "types": "./dist/index.d.mts",
   *         "default": "./dist/index.mjs"
   *       },
   *       "require": {
   *         "types": "./dist/index.d.ts",
   *         "default": "./dist/index.cjs"
   *       }
   *     }
   *   }
   * }
   */
  @Test fun testNestedConditionalExports() {
    val fs = tempHostFs()
    fs.setCurrentWorkingDirectory(Path("/"))
    fs.createDirectory(fs.getPath("node_modules"))
    fs.createDirectory(fs.getPath("node_modules/nested-conditional"))
    fs.createDirectory(fs.getPath("node_modules/nested-conditional/dist"))

    // Create package.json with nested conditional exports (the problematic pattern)
    val configPath = fs.getPath("node_modules/nested-conditional/package.json")
    fs.writeStream(configPath).use { stream ->
      stream.write("""
        {
          "name": "nested-conditional",
          "version": "1.0.0",
          "exports": {
            ".": {
              "import": {
                "types": "./dist/index.d.mts",
                "default": "./dist/index.mjs"
              },
              "require": {
                "types": "./dist/index.d.ts",
                "default": "./dist/index.cjs"
              }
            }
          }
        }
      """.trimIndent().toByteArray())
    }

    // Create the ESM module file (should be selected via import -> default)
    val modulePath = fs.getPath("node_modules/nested-conditional/dist/index.mjs")
    fs.writeStream(modulePath).use { stream ->
      stream.write("export const value = 'nested-conditional-esm';".toByteArray())
    }

    // Create root package.json
    val pkgPath = fs.getPath("package.json")
    fs.writeStream(pkgPath).use { stream ->
      stream.write("""{"name": "test", "type": "module"}""".toByteArray())
    }

    withHostFs(fs) {
      // language=javascript
      """
        import { value } from "nested-conditional";
        test(value).isEqualTo("nested-conditional-esm");
      """
    }.doesNotFail()
  }

  /**
   * Test: Import from a scoped package with nested conditional exports.
   *
   * This mimics packages like @discordjs/collection.
   */
  @Test fun testScopedPackageNestedExports() {
    val fs = tempHostFs()
    fs.setCurrentWorkingDirectory(Path("/"))
    fs.createDirectory(fs.getPath("node_modules"))
    fs.createDirectory(fs.getPath("node_modules/@testscope"))
    fs.createDirectory(fs.getPath("node_modules/@testscope/collection"))
    fs.createDirectory(fs.getPath("node_modules/@testscope/collection/dist"))

    // Create package.json mimicking @discordjs/collection pattern
    val configPath = fs.getPath("node_modules/@testscope/collection/package.json")
    fs.writeStream(configPath).use { stream ->
      stream.write("""
        {
          "name": "@testscope/collection",
          "version": "2.1.1",
          "exports": {
            ".": {
              "require": {
                "types": "./dist/index.d.ts",
                "default": "./dist/index.js"
              },
              "import": {
                "types": "./dist/index.d.mts",
                "default": "./dist/index.mjs"
              }
            }
          },
          "main": "./dist/index.js",
          "module": "./dist/index.mjs"
        }
      """.trimIndent().toByteArray())
    }

    // Create the ESM module file
    val modulePath = fs.getPath("node_modules/@testscope/collection/dist/index.mjs")
    fs.writeStream(modulePath).use { stream ->
      stream.write("""
        export class Collection extends Map {
          constructor() {
            super();
            this.name = "TestCollection";
          }
        }
      """.trimIndent().toByteArray())
    }

    // Create root package.json
    val pkgPath = fs.getPath("package.json")
    fs.writeStream(pkgPath).use { stream ->
      stream.write("""{"name": "test", "type": "module"}""".toByteArray())
    }

    withHostFs(fs) {
      // language=javascript
      """
        import { Collection } from "@testscope/collection";
        const c = new Collection();
        test(c.name).isEqualTo("TestCollection");
      """
    }.doesNotFail()
  }

  /**
   * Test: Import with "default" fallback when specific condition not available.
   */
  @Test fun testDefaultFallback() {
    val fs = tempHostFs()
    fs.setCurrentWorkingDirectory(Path("/"))
    fs.createDirectory(fs.getPath("node_modules"))
    fs.createDirectory(fs.getPath("node_modules/default-fallback"))
    fs.createDirectory(fs.getPath("node_modules/default-fallback/dist"))

    // Create package.json with only "default" (no "import" condition)
    val configPath = fs.getPath("node_modules/default-fallback/package.json")
    fs.writeStream(configPath).use { stream ->
      stream.write("""
        {
          "name": "default-fallback",
          "version": "1.0.0",
          "exports": {
            ".": {
              "default": "./dist/index.mjs"
            }
          }
        }
      """.trimIndent().toByteArray())
    }

    // Create the module file
    val modulePath = fs.getPath("node_modules/default-fallback/dist/index.mjs")
    fs.writeStream(modulePath).use { stream ->
      stream.write("export const value = 'default-fallback-value';".toByteArray())
    }

    // Create root package.json
    val pkgPath = fs.getPath("package.json")
    fs.writeStream(pkgPath).use { stream ->
      stream.write("""{"name": "test", "type": "module"}""".toByteArray())
    }

    withHostFs(fs) {
      // language=javascript
      """
        import { value } from "default-fallback";
        test(value).isEqualTo("default-fallback-value");
      """
    }.doesNotFail()
  }
}
