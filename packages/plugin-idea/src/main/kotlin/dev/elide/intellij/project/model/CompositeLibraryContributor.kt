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
import java.nio.file.Path
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.SourceSet

/**
 * A contributor that collects library data using all registered contributors from
 * [LibraryDependencyContributor.Extensions].
 */
class CompositeLibraryContributor : LibraryDependencyContributor {
  override suspend fun collectDependencies(
    projectPath: Path,
    elideProject: ElideConfiguredProject,
    sourceSet: SourceSet
  ): Sequence<LibraryData> {
    return LibraryDependencyContributor.Extensions.extensionList.flatMap {
      it.collectDependencies(projectPath, elideProject, sourceSet)
    }.asSequence()
  }
}
