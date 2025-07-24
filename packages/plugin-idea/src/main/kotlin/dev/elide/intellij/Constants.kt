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
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.util.IconLoader
import dev.elide.intellij.Constants.Strings.get
import org.jetbrains.annotations.PropertyKey
import javax.swing.Icon

/** Useful constants used by the Elide plugin. */
object Constants {
  /** External System ID for Elide. */
  val SYSTEM_ID = ProjectSystemId("ELIDE")

  /** Elide plugin ID. */
  const val PLUGIN_ID = "dev.elide"

  /** ID used to reference settings panels. */
  const val CONFIGURABLE_ID = "reference.settingsdialog.project.elide"

  /** Name and extension of the Elide manifest file. */
  const val MANIFEST_NAME = "elide.pkl"

  /** Name of the project directory where the lockfile and other artifacts are placed. */
  const val OUTPUT_DIR = ".dev"

  /** Name and extension of the Elide lockfile. */
  const val LOCKFILE_NAME = "elide.lock.bin"

  /** Default installation directory for Elide under the user home path. */
  const val ELIDE_HOME = "elide"

  /** Resources path relative to the root of the Elide distribution. */
  const val ELIDE_RESOURCES_DIR = "resources"

  /** Relative path to the CLI binary in an Elide distribution. */
  const val ELIDE_BINARY = "elide"

  /** Browser URL for the installation section of the documentation. */
  const val INSTALL_URL = "https://docs.elide.dev/installation"

  // command names
  const val COMMAND_RUN = "run"
  const val COMMAND_BUILD = "build"
  const val COMMAND_INSTALL = "install"
  const val COMMAND_SERVE = "serve"

  /** Commands available to all projects by default. */
  val DEFAULT_COMMANDS = arrayOf(COMMAND_BUILD, COMMAND_INSTALL, COMMAND_RUN, COMMAND_SERVE)

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

  /** Descriptor for a file chooser to be used when selecting an Elide distribution. */
  @JvmStatic fun sdkFileChooser(): FileChooserDescriptor {
    return FileChooserDescriptor(
      /* chooseFiles = */ false,
      /* chooseFolders = */ true,
      /* chooseJars = */ false,
      /* chooseJarsAsFiles = */ false,
      /* chooseJarContents = */ false,
      /* chooseMultiple = */ false,
    )
  }

  data object Icons {
    /** Generic Icon for Elide. */
    @JvmStatic val ELIDE = load("/icons/elide.svg")

    /** Icon for the project sync button. */
    @JvmStatic val RELOAD_PROJECT = ELIDE

    /** Load an icon at the given [path] from the plugin resources. */
    private fun load(path: String): Icon {
      return IconLoader.getIcon(path, Icons::class.java)
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
