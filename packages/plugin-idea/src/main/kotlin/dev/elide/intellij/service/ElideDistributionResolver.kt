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

package dev.elide.intellij.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants
import dev.elide.intellij.service.ElideDistributionResolver.Companion.defaultDistributionPath
import dev.elide.intellij.service.ElideDistributionResolver.Companion.getElideHome
import dev.elide.intellij.service.ElideDistributionResolver.Companion.resourcesPath
import dev.elide.intellij.service.ElideDistributionResolver.Companion.validateDistributionPath
import dev.elide.intellij.settings.ElideDistributionSetting
import dev.elide.intellij.settings.ElideSettings
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

/**
 * Service used to resolve an Elide distribution for a project; use the static [getElideHome] function to obtain a
 * distribution path that respects project configuration, with a fallback to [defaultDistributionPath].
 *
 * For simple validation cases, [validateDistributionPath] can be used to verify that a minimal distribution structure
 * is present in the selected Elide home directory.
 */
@Service(Service.Level.PROJECT)
class ElideDistributionResolver(private val project: Project) {
  /**
   * Resolve the path to the preferred Elide distribution for the linked project at [externalProjectPath]. If no linked
   * settings are found, [defaultDistributionPath] is returned instead.
   *
   * The returned path is *not* validated by [validateDistributionPath] or in any other way; it is the responsibility
   * of the caller to properly check that the path correspond to a valid Elide distribution before using it as such.
   */
  fun resolveDistributionPath(externalProjectPath: String): Path {
    val settings = ElideSettings.getSettings(project)
      .getLinkedProjectSettings(externalProjectPath)
      ?: return defaultDistributionPath()

    return when (settings.elideDistributionType) {
      ElideDistributionSetting.Custom -> Path(settings.elideDistributionPath).normalize()
      ElideDistributionSetting.AutoDetect -> defaultDistributionPath()
    }
  }

  companion object {
    /**
     * Returns the path to the preferred Elide distribution for a linked external project, or the
     * [defaultDistributionPath] if no project settings are found.
     *
     * The returned path is *not* validated by [validateDistributionPath] or in any other way; it is the responsibility
     * of the caller to properly check that the path correspond to a valid Elide distribution before using it as such.
     */
    @JvmStatic fun getElideHome(project: Project, externalProjectPath: String): Path {
      return project.getService(ElideDistributionResolver::class.java).resolveDistributionPath(externalProjectPath)
    }

    /**
     * Returns the default path to the Elide installation in the user's home directory. Note that the path is not
     * guaranteed to contain a valid distribution or even exist.
     */
    @JvmStatic fun defaultDistributionPath(): Path {
      return Path(System.getProperty("user.home")).resolve(Constants.ELIDE_HOME)
    }

    /**
     * Shorthand for resolving the [resourcesPath] in the [preferred Elide distribution][getElideHome] for a linked
     * external project.
     */
    @JvmStatic fun resourcesPath(project: Project, externalProjectPath: String): Path {
      val elideHome = getElideHome(project, externalProjectPath)
      return elideHome.resolve(Constants.ELIDE_RESOURCES_DIR)
    }

    /** Returns the path to the resources directory inside the given Elide distribution. */
    @JvmStatic fun resourcesPath(elideHome: Path): Path {
      return elideHome.resolve(Constants.ELIDE_RESOURCES_DIR)
    }

    /** Lightly validates an Elide distribution [path], verifying some basic directories and files are presents. */
    @JvmStatic fun validateDistributionPath(path: Path): Boolean {
      if (!path.resolve(Constants.ELIDE_RESOURCES_DIR).isDirectory()) return false
      if (!path.resolve(Constants.ELIDE_BINARY).isRegularFile()) return false

      return true
    }
  }
}
