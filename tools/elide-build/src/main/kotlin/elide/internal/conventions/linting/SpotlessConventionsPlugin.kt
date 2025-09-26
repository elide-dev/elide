/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.internal.conventions.linting

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import elide.internal.conventions.Constants
import elide.internal.conventions.Constants.Versions
import elide.internal.conventions.ElideBuildExtension

// Branch to ratchet against.
private const val ratchetBase = "origin/main"

private fun SpotlessExtension.configureSpotlessForProject(conventions: ElideBuildExtension, project: Project) {
  ratchetFrom(ratchetBase)
  isEnforceCheck = conventions.checks.enforceCheck
  val licenseHeader = project.rootProject.layout.projectDirectory.file(".github/license-header.txt")
  val diktatConfig = "${project.rootDir}/config/diktat/diktat.yml"

  // ---- Linting: Protobuf
  //
  if (conventions.checks.experimental) protobuf {
    // Use Buf to lint Protobuf files.
    buf(Versions.BUF)
  }

  // ---- Linting: Java
  //
  if (conventions.java.requested) java {
    // Setup license header.
    licenseHeaderFile(licenseHeader)

    // Enforce import order.
    importOrder()

    // Remove unused imports.
    removeUnusedImports()

    // Use Google's Java formatter.
    if (conventions.checks.javaFormat) googleJavaFormat(Versions.GOOGLE_JAVA_FORMAT)

    // Preserve annotation formatting.
    if (conventions.checks.javaFormat) formatAnnotations()
  }

  // ---- Linting: Kotlin
  //
  if (conventions.kotlin.requested) kotlin {
    // Setup license header.
    licenseHeaderFile(licenseHeader)

    // enable ktlint.
    if (conventions.checks.ktlint) ktlint(Versions.KTLINT).editorConfigOverride(Constants.Linting.ktlintOverrides)

    // enable diktat.
    if (conventions.checks.diktat) diktat(Versions.DIKTAT).configFile(diktatConfig)
  }

  // ---- Linting: Python
  //
  if (conventions.python.requested) python {
    black()
  }

  // ---- Linting: JavaScript/TypeScript
  //
  if (conventions.javascript.requested || conventions.typescript.requested) {
    javascript {
      if (conventions.checks.prettier) prettier(Versions.PRETTIER)
      if (conventions.checks.eslint) eslint(Versions.ESLINT)
    }

    typescript {
      if (conventions.checks.prettier) prettier(Versions.PRETTIER)
      if (conventions.checks.eslint) eslint(Versions.ESLINT)
    }
  }

  // ---- Linting: C++/Headers
  //
  if (conventions.cpp.requested) cpp {
    clangFormat()
  }

  // ---- Linting: General
  //
  if (conventions.checks.prettier && conventions.checks.experimental) {
    json {
      prettier(Versions.PRETTIER)
    }

    yaml {
      prettier(Versions.PRETTIER)
    }
  }

  // ---- Linting: Kotlin/Gradle
  //
  kotlinGradle {
    target("*.gradle.kts")
    if (conventions.checks.ktlint) {
      ktlint(Versions.KTLINT).editorConfigOverride(Constants.Linting.ktlintOverridesKts)
    }
    if (conventions.checks.diktat) {
      diktat(Versions.DIKTAT).configFile("${project.rootDir}/config/diktat/diktat.yml")
    }
  }
}

public class SpotlessConventionsPlugin : Plugin<Project> {
  private companion object {
    const val SPOTLESS_PLUGIN = "com.diffplug.spotless"
  }

  override fun apply(target: Project) {
    target.pluginManager.withPlugin(SPOTLESS_PLUGIN) {
      val conventions = target.extensions.getByType(ElideBuildExtension::class.java)
      target.extensions.getByType(SpotlessExtension::class.java).run {
        configureSpotlessForProject(conventions, target)
      }
    }
  }
}
