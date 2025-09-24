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
import com.github.kinquirer.components.promptInputPassword
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import elide.annotations.Singleton
import elide.secrets.Encryption
import elide.secrets.SecretsState
import elide.secrets.Values
import elide.secrets.dto.api.github.GithubRepositoryResponse
import elide.secrets.dto.persisted.Profile.Companion.get
import elide.secrets.dto.persisted.StringSecret
import elide.secrets.remote.RemoteInitializer
import elide.secrets.remote.impl.GithubRemote.Companion.get
import elide.tooling.project.manifest.ElidePackageManifest

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
  override val name: String = ElidePackageManifest.SecretsRemote.GITHUB.symbol

  override suspend fun initialize(): GithubRemote {
    repository =
      SecretsState.manifest?.secrets?.github?.repository
        ?: SecretsState.local[Values.GITHUB_REPOSITORY_SECRET]
        ?: askRepository()
    token = SecretsState.local[Values.GITHUB_TOKEN_SECRET] ?: askToken()
    val writeAccess = validateConnection(token, repository)
    SecretsState.updateLocal { addAll(
      StringSecret(Values.GITHUB_REPOSITORY_SECRET, repository),
      StringSecret(Values.GITHUB_TOKEN_SECRET, token),
    )}
    return GithubRemote(writeAccess, repository, token, encryption, client, json)
  }

  private fun askRepository(): String {
    println(Values.GITHUB_REMOTE_REPOSITORY_MESSAGE)
    return KInquirer.promptInput(Values.GITHUB_REMOTE_REPOSITORY_PROMPT)
  }

  private fun askToken(): String {
    println(Values.GITHUB_REMOTE_TOKEN_MESSAGE)
    return KInquirer.promptInputPassword(Values.GITHUB_REMOTE_TOKEN_PROMPT)
  }

  private suspend fun validateConnection(token: String, repository: String): Boolean {
    val (_, content) = client.get<GithubRepositoryResponse>("repos/$repository", token, HttpStatusCode.OK)
    if (!content!!.private) throw IllegalArgumentException(Values.GITHUB_REMOTE_REPOSITORY_NOT_PRIVATE_EXCEPTION)
    if (!content.permissions.read)
      throw IllegalArgumentException(Values.GITHUB_REMOTE_REPOSITORY_NO_READ_ACCESS_EXCEPTION)
    return content.permissions.write
  }
}
