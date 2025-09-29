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
package elide.secrets.remote.impl

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import elide.annotations.Singleton
import elide.secrets.SecretsState
import elide.secrets.Values
import elide.secrets.dto.persisted.Profile.Companion.get
import elide.secrets.dto.persisted.StringSecret
import elide.secrets.remote.RemoteInitializer
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Initializer for [ProjectRemote].
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class ProjectRemoteInitializer : RemoteInitializer {
  private lateinit var path: String
  override val name: String = ElidePackageManifest.SecretsRemote.PROJECT.symbol

  override suspend fun init(prompts: MutableList<String>): ProjectRemote {
    path =
      SecretsState.manifest?.secrets?.project?.path
        ?: SecretsState.local[Values.PROJECT_REMOTE_PATH_SECRET]
        ?: askPath(prompts)
    val realPath = validatePath()
    SecretsState.updateLocal { add(StringSecret(Values.PROJECT_REMOTE_PATH_SECRET, path)) }
    return ProjectRemote(realPath)
  }

  override suspend fun initNonInteractive(): ProjectRemote {
    path =
      SecretsState.manifest?.secrets?.project?.path
        ?: throw IllegalStateException(Values.PROJECT_REMOTE_PATH_NOT_SPECIFIED_EXCEPTION)
    val realPath = validatePath()
    SecretsState.updateLocal { add(StringSecret(Values.PROJECT_REMOTE_PATH_SECRET, path)) }
    return ProjectRemote(realPath)
  }

  private fun askPath(prompts: MutableList<String>): String {
    println(Values.PROJECT_REMOTE_PATH_MESSAGE)
    return prompts.removeFirstOrNull()
      ?: KInquirer.promptInput(
        Values.PROJECT_REMOTE_PATH_PROMPT,
        Values.PROJECT_REMOTE_DEFAULT_PATH,
      )
  }

  private fun validatePath(): Path {
    val root = SecretsState.path.parent!!
    val path = Path(root, path)
    SystemFileSystem.createDirectories(path)
    val metadata = SystemFileSystem.metadataOrNull(path)!!
    if (!metadata.isDirectory) throw IllegalStateException("The path is not a directory")
    return path
  }
}
