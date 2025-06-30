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

package dev.elide.intellij

import com.intellij.DynamicBundle
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.IconManager
import com.intellij.ui.PlatformIcons
import dev.elide.intellij.Constants.Strings.get
import org.jetbrains.annotations.PropertyKey
import javax.swing.Icon

/** Useful constants used by the Elide plugin. */
object Constants {
  /** External System ID for Elide. */
  val SYSTEM_ID = ProjectSystemId("ELIDE")

  /** Elide plugin ID. */
  const val PLUGIN_ID = "dev.elide.intellij"

  /** ID used to reference settings panels. */
  const val CONFIGURABLE_ID = "reference.settingsdialog.project.elide"

  /** Name and extension of the Elide manifest file. */
  const val MANIFEST_NAME = "elide.pkl"

  /** Name of the project directory where the lockfile and other artifacts are placed. */
  const val OUTPUT_DIR = ".dev"

  /** Name and extension of the Elide lockfile. */
  const val LOCKFILE_NAME = "elide.lock.bin"

  /** Descriptor for a file chooser to be used when selecting an Elide project. */
  @JvmStatic fun projectFileChooser(): FileChooserDescriptor {
    return FileChooserDescriptor(
      /* chooseFiles = */ true,
      /* chooseFolders = */ false,
      /* chooseJars = */ false,
      /* chooseJarsAsFiles = */ false,
      /* chooseJarContents = */ false,
      /* chooseMultiple = */ false,
    ).withFileFilter { it.name == MANIFEST_NAME }
  }

  data object Icons {
    @JvmStatic private val LOG = Logger.getInstance(Icons::class.java)

    /** Icon for the project sync button. */
    @JvmStatic val RELOAD_PROJECT = load("/icons/elide.svg")

    /** Load an icon at the given [path] from the plugin resources. */
    private fun load(path: String): Icon {
      return try {
        IconLoader.getIcon(path, Icons::class.java)
      } catch (e: Exception) {
        LOG.warn("Unable to load icon from $path", e)

        // Fallback to IntelliJ's default icons if custom icons aren't available
        @Suppress("UnstableApiUsage")
        IconManager.getInstance().getPlatformIcon(PlatformIcons.Stub)
      }
    }
  }

  /**
   * Localized strings provided by a resource bundle, use the indexing operator or the static [get] function to obtain
   * formatted messages.
   */
  data object Strings : DynamicBundle("i18n.Strings") {
    @JvmStatic operator fun get(@PropertyKey(resourceBundle = "i18n.Strings") key: String, vararg params: Any): String {
      return getMessage(key, params = params)
    }
  }
}
