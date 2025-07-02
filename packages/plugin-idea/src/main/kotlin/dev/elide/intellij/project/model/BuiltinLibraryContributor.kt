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

package dev.elide.intellij.project.model

import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.LibraryPathType
import dev.elide.intellij.Constants
import java.nio.file.Path
import kotlin.io.path.pathString
import elide.tooling.jvm.JvmLibraries
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.SourceSet
import elide.tooling.project.SourceSetLanguage
import elide.tooling.project.SourceSetType

/** Provides library data for dependencies that are bundled with Elide for JVM projects. */
class BuiltinLibraryContributor : LibraryDependencyContributor {
  override suspend fun collectDependencies(
    projectPath: Path,
    elideProject: ElideConfiguredProject,
    sourceSet: SourceSet
  ): Sequence<LibraryData> {
    if (sourceSet.languages?.contains(SourceSetLanguage.Kotlin) != true) return emptySequence()

    return JvmLibraries.builtinClasspath(
      path = elideProject.resourcesPath,
      tests = sourceSet.type == SourceSetType.Tests,
      kotlin = true,
    ).asSequence().map {
      val library = LibraryData(Constants.SYSTEM_ID, it.path.pathString, false)
      library.addPath(LibraryPathType.BINARY, it.path.pathString)

      library
    }
  }
}
