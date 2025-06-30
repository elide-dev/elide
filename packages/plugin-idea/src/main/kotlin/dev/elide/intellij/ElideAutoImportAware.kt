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

import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.project.Project
import java.io.File

class ElideAutoImportAware : ExternalSystemAutoImportAware {
  override fun getAffectedExternalProjectPath(changedFileOrDirPath: String, project: Project): String? {
    val file = File(changedFileOrDirPath)

    return when {
      // If the changed file is the manifest, return its parent directory
      file.name == Constants.MANIFEST_NAME && file.isFile -> file.parent

      // If it's a directory, check if it contains elide.pkl
      file.isDirectory && File(file, Constants.MANIFEST_NAME).exists() -> changedFileOrDirPath

      // Check parent directories up to a reasonable limit
      else -> null
    }
  }

  override fun getAffectedExternalProjectFiles(projectPath: String?, project: Project): List<File?> {
    return File(projectPath, Constants.MANIFEST_NAME)
      .takeIf { it.exists() }
      ?.let { listOf(it) }
      .orEmpty()
  }
}
