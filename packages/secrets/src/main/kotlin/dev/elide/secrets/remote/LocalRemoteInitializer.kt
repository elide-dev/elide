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
package dev.elide.secrets.remote

import dev.elide.secrets.DataHandler
import dev.elide.secrets.Utils
import dev.elide.secrets.dto.persisted.SecretCollection
import dev.elide.secrets.dto.persisted.StringSecret
import elide.annotations.Singleton

/**
 * Initializer for connecting to GitHub.
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class LocalRemoteInitializer(private val dataHandler: DataHandler) : RemoteInitializer {
  private lateinit var path: String
  override val id: String = "local"

  override fun initialize(interactive: Boolean, local: SecretCollection): LocalRemote {
    path =
      local[LocalRemote.PATH_NAME]
        ?: if (interactive) askPath() else throw IllegalStateException("A local remote has not been registered")
    return LocalRemote(path, dataHandler)
  }

  override fun updateLocal(local: SecretCollection): SecretCollection =
    local.add(StringSecret(LocalRemote.PATH_NAME, path))

  private fun askPath(): String {
    println(
      "Elide Secrets local remote stores secrets as encrypted files in a folder, which usually is committed to the project's source control system."
    )
    println(
      "Absolute paths start with \"/\" on Unix systems and with the drive letter and \":\\\" on Windows systems. Relative paths are resolved from the parent directory of the \".elide-secrets\" directory."
    )
    return Utils.readWithConfirm("Please enter a path to store remote secrets in:")
  }
}
