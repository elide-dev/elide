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

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInputPassword
import dev.elide.secrets.DataHandler
import dev.elide.secrets.Utils
import dev.elide.secrets.dto.api.github.GithubRepositoryResponse
import dev.elide.secrets.dto.persisted.SecretCollection
import dev.elide.secrets.dto.persisted.StringSecret
import dev.elide.secrets.remote.GithubRemote.Companion.get
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import elide.annotations.Singleton

/**
 * Initializer for connecting to GitHub.
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class GithubRemoteInitializer(private val dataHandler: DataHandler, private val client: HttpClient) :
  RemoteInitializer {
  private lateinit var repository: String
  private lateinit var token: String
  override val id: String = "github"

  override fun initialize(interactive: Boolean, local: SecretCollection): GithubRemote {
    repository =
      local[GithubRemote.REPOSITORY_NAME]
        ?: if (interactive) askRepository()
        else throw IllegalStateException("A GitHub repository has not been registered")
    token =
      local[GithubRemote.TOKEN_NAME]
        ?: if (interactive) askToken() else throw IllegalStateException("A GitHub token has not been registered")
    val writeAccess = runBlocking { validateConnection(token, repository) }
    return GithubRemote(writeAccess, repository, token, dataHandler, client)
  }

  override fun updateLocal(local: SecretCollection): SecretCollection =
    local.addAll(StringSecret(GithubRemote.REPOSITORY_NAME, repository), StringSecret(GithubRemote.TOKEN_NAME, token))

  private fun askRepository(): String {
    println("Elide Secrets on GitHub are stored as encrypted files committed to a private repository.")
    return Utils.readWithConfirm("Please enter the repository identity (\"owner/repository\"): ")
  }

  private fun askToken(): String {
    println(
      "To access GitHub, you need a personal access token. If you do not have one with the required " +
        "permissions, please head over to https://github.com/settings/personal-access-tokens and " +
        "generate a new token. The token must at least have read access to \"Contents\" of the " +
        "repository ($repository). Optional write access to \"Contents\" allows you to update the remote " +
        "secrets."
    )
    return KInquirer.promptInputPassword("Please enter your token: ")
  }

  private suspend fun validateConnection(token: String, repository: String): Boolean {
    val (_, content) = client.get<GithubRepositoryResponse>("repos/$repository", token, HttpStatusCode.OK)
    if (!content!!.private) throw IllegalArgumentException("Repository is not private")
    if (!content.permissions.read) throw IllegalArgumentException("No read access to repository contents")
    return content.permissions.write
  }
}
