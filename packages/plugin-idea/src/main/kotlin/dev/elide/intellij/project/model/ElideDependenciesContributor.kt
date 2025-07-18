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

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import dev.elide.intellij.Constants
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import elide.tooling.ClasspathSpec
import elide.tooling.JvmMultiPathEntryType
import elide.tooling.jvm.JvmLibraries
import elide.tooling.jvm.resolver.MavenLockfileResolver
import elide.tooling.lockfile.ElideLockfile
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.SourceSet
import elide.tooling.project.SourceSetLanguage
import elide.tooling.project.SourceSetType

/**
 * Adds library dependencies to a project and its modules, using the resolved dependency data from the Elide lockfile.
 * Where applicable, built-in dependencies shipped with the runtime are also added.
 */
class ElideDependenciesContributor : ElideProjectModelContributor {
  override suspend fun enhanceModule(
    moduleNode: DataNode<ModuleData>,
    elideProject: ElideConfiguredProject,
    elideSourceSet: SourceSet,
    projectPath: Path
  ) {
    // add dependencies
    val libraries = collectMavenDependencies(elideProject, elideSourceSet, projectPath) +
            collectBuiltinDependencies(elideProject, elideSourceSet)

    libraries.forEach {
      moduleNode.createChild(
        ProjectKeys.LIBRARY_DEPENDENCY,
        LibraryDependencyData(moduleNode.data, it, LibraryLevel.MODULE),
      )
    }
  }

  /** Provides library data for dependencies that are bundled with Elide for JVM projects. */
  private fun collectBuiltinDependencies(
    elideProject: ElideConfiguredProject,
    sourceSet: SourceSet,
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

  /**
   * Provides library data from the resolved classpath stored in the project's lockfile, using a
   * [MavenLockfileResolver].
   */
  private suspend fun collectMavenDependencies(
    elideProject: ElideConfiguredProject,
    sourceSet: SourceSet,
    projectPath: Path
  ): Sequence<LibraryData> {
    val mavenLockfile = elideProject.activeLockfile
      ?.lockfile
      ?.stanzas
      ?.find { it is ElideLockfile.StanzaData && it.state is ElideLockfile.MavenLockfile }
      ?.state as? ElideLockfile.MavenLockfile
      ?: return emptySequence()

    val resolver = MavenLockfileResolver.of(mavenLockfile, projectPath)
    val spec = when (sourceSet.type) {
      SourceSetType.Sources -> ClasspathSpec.Compile
      SourceSetType.Tests -> ClasspathSpec.TestCompile
    }

    return resolver.classpathProvider(spec)
      ?.classpath()
      ?.asSequence()
      ?.filter { it.type == JvmMultiPathEntryType.JAR }
      ?.map {
        val library = LibraryData(Constants.SYSTEM_ID, it.path.pathString, false)
        val basePath = it.path.parent.resolve(it.path.nameWithoutExtension)

        library.addPath(LibraryPathType.BINARY, it.path.pathString)
        library.addPath(LibraryPathType.SOURCE, "${basePath.pathString}${SUFFIX_SOURCES}")
        library.addPath(LibraryPathType.DOC, "${basePath.pathString}${SUFFIX_JAVADOC}")

        library
      }.orEmpty()
  }

  private companion object {
    private const val SUFFIX_JAR = "jar"
    private const val SUFFIX_JAVADOC = "-javadoc.$SUFFIX_JAR"
    private const val SUFFIX_SOURCES = "-sources.$SUFFIX_JAR"
  }
}
