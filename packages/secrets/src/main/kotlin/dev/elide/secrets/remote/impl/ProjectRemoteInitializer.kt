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
package dev.elide.secrets.remote.impl

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import dev.elide.secrets.Encryption
import dev.elide.secrets.SecretsState
import dev.elide.secrets.Values
import dev.elide.secrets.dto.persisted.Profile.Companion.get
import dev.elide.secrets.dto.persisted.StringSecret
import dev.elide.secrets.remote.RemoteInitializer
import io.ktor.client.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemPathSeparator
import kotlinx.serialization.json.Json
import elide.annotations.Singleton

/**
 * Initializer for storing remote secrets alongside project files.
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class ProjectRemoteInitializer(
  private val encryption: Encryption,
  private val client: HttpClient,
  private val json: Json,
) :
  RemoteInitializer {
  private lateinit var path: String
  override val id: String = "project"

  override suspend fun initialize(): ProjectRemote {
    path =
      SecretsState.local["project:path"]
        ?: if (SecretsState.interactive) askPath()
        else throw IllegalStateException("A GitHub repository has not been registered")
    val realPath = validatePath()
    SecretsState.local.add(StringSecret(GithubRemote.REPOSITORY_NAME, path))
    return ProjectRemote(realPath)
  }

  private fun askPath(): String {
    println("Elide Secrets in project mode are stored encrypted alongside project files.")
    return KInquirer.promptInput("Please enter a path relative to the project directory (parent directory of ${Values.DEFAULT_PATH}) using your system's path separator (\"$SystemPathSeparator\"):")
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
