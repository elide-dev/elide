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

import dev.elide.secrets.Console
import dev.elide.secrets.DataHandler
import dev.elide.secrets.Utils
import dev.elide.secrets.dto.api.github.GithubRepositoryResponse
import dev.elide.secrets.dto.persisted.SecretCollection
import dev.elide.secrets.dto.persisted.StringSecret
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

/**
 * Initializer for connecting to GitHub.
 *
 * @author Lauri Heino <datafox>
 */
internal class GithubRemoteInitializer(
    private val dataHandler: DataHandler,
    private val console: Console,
) : RemoteInitializer {
    private lateinit var repository: String
    private lateinit var token: String

    override fun initialize(interactive: Boolean, local: SecretCollection): GithubRemote {
        repository =
            local[GithubRemote.REPOSITORY_NAME]
                ?: if (interactive) askRepository()
                else throw IllegalStateException("A GitHub repository has not been registered")
        token =
            local[GithubRemote.TOKEN_NAME]
                ?: if (interactive) askToken()
                else throw IllegalStateException("A GitHub token has not been registered")
        val writeAccess = runBlocking { validateConnection(token, repository) }
        return GithubRemote(writeAccess, repository, token, dataHandler)
    }

    override fun updateLocal(local: SecretCollection): SecretCollection =
        local
            .add(StringSecret(GithubRemote.REPOSITORY_NAME, repository))
            .add(StringSecret(GithubRemote.TOKEN_NAME, token))

    private fun askRepository(): String {
        console.println(
            "Elide Secrets on GitHub are stored as encrypted files committed to a private repository."
        )
        return Utils.readWithConfirm(
            console,
            "Please enter the repository identity (\"owner/repository\"): ",
        )
    }

    private fun askToken(): String {
        console.println(
            "To access GitHub, you need a personal access token. If you do not have one with the required " +
                "permissions, please head over to https://github.com/settings/personal-access-tokens and " +
                "generate a new token. The token must at least have read access to \"Contents\" of the " +
                "repository ($repository). Optional write access to \"Contents\" allows you to update the remote " +
                "secrets."
        )
        console.print("Please enter your token: ")
        return console.readPassword()
    }

    private suspend fun validateConnection(token: String, repository: String): Boolean {
        val (_, content) =
            GithubRemote.get<GithubRepositoryResponse>(
                "repos/$repository",
                token,
                HttpStatusCode.OK,
            )
        if (!content!!.private) throw IllegalArgumentException("Repository is not private")
        if (!content.permissions.read)
            throw IllegalArgumentException("No read access to repository contents")
        return content.permissions.write
    }
}
