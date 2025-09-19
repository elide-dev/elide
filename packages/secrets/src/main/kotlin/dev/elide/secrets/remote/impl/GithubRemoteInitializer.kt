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
import com.github.kinquirer.components.promptInputPassword
import dev.elide.secrets.Encryption
import dev.elide.secrets.SecretsState
import dev.elide.secrets.dto.api.github.GithubRepositoryResponse
import dev.elide.secrets.dto.persisted.Profile.Companion.get
import dev.elide.secrets.dto.persisted.StringSecret
import dev.elide.secrets.remote.RemoteInitializer
import dev.elide.secrets.remote.impl.GithubRemote.Companion.get
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import elide.annotations.Singleton

/**
 * Initializer for connecting to GitHub.
 *
 * @author Lauri Heino <datafox>
 */
@Singleton
internal class GithubRemoteInitializer(
  private val encryption: Encryption,
  private val client: HttpClient,
  private val json: Json,
) : RemoteInitializer {
  private lateinit var repository: String
  private lateinit var token: String
  override val name: String = "github"

  override suspend fun initialize(): GithubRemote {
    repository =
      SecretsState.local[GithubRemote.REPOSITORY_NAME]
        ?: if (SecretsState.interactive) askRepository()
        else throw IllegalStateException("A GitHub repository has not been registered")
    token =
      SecretsState.local[GithubRemote.TOKEN_NAME]
        ?: if (SecretsState.interactive) askToken()
        else throw IllegalStateException("A GitHub token has not been registered")
    val writeAccess = validateConnection(token, repository)
    SecretsState.local.addAll(
      StringSecret(GithubRemote.REPOSITORY_NAME, repository),
      StringSecret(GithubRemote.TOKEN_NAME, token),
    )
    return GithubRemote(writeAccess, repository, token, encryption, client, json)
  }

  private fun askRepository(): String {
    println("Elide Secrets on GitHub are stored as encrypted files committed to a private repository.")
    return KInquirer.promptInput("Please enter the repository identity (\"owner/repository\"): ")
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
    val (_, content) = client.get<GithubRepositoryResponse>("repos/$repository", token, "", HttpStatusCode.OK)
    if (!content!!.private) throw IllegalArgumentException("Repository is not private")
    if (!content.permissions.read) throw IllegalArgumentException("No read access to repository contents")
    return content.permissions.write
  }
}
