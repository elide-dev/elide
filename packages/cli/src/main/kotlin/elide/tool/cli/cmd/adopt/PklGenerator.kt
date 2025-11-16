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

/**
 * Generates elide.pkl content from a Maven POM descriptor.
 */
object PklGenerator {
  /**
   * Generate elide.pkl content from a POM descriptor.
   */
  fun generate(pom: PomDescriptor): String = buildString {
    // Header
    appendLine("amends \"elide:project.pkl\"")
    appendLine()

    // Project metadata
    appendLine("name = \"${pom.artifactId}\"")
    if (pom.description != null) {
      appendLine("description = \"${pom.description.escapeQuotes()}\"")
    }
    appendLine()

    // Dependencies section
    val compileDeps = pom.dependencies.filter { it.scope == "compile" || it.scope == "runtime" }
    val testDeps = pom.dependencies.filter { it.scope == "test" }

    if (compileDeps.isNotEmpty() || testDeps.isNotEmpty()) {
      appendLine("dependencies {")
      appendLine("  maven {")

      // Compile dependencies
      if (compileDeps.isNotEmpty()) {
        appendLine("    packages {")
        compileDeps.forEach { dep ->
          appendLine("      \"${dep.coordinate}\"")
        }
        appendLine("    }")
      }

      // Test dependencies
      if (testDeps.isNotEmpty()) {
        appendLine("    testPackages {")
        testDeps.forEach { dep ->
          appendLine("      \"${dep.coordinate}\"")
        }
        appendLine("    }")
      }

      appendLine("  }")
      appendLine("}")
      appendLine()
    }

    // Source mappings
    appendLine("sources {")
    appendLine("  [\"main\"] = \"src/main/java/**/*.java\"")
    appendLine("  [\"test\"] = \"src/test/java/**/*.java\"")
    appendLine("}")
  }

  private fun String.escapeQuotes(): String = replace("\"", "\\\"")
}
