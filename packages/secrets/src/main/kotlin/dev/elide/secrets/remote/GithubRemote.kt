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
import dev.elide.secrets.Values
import dev.elide.secrets.dto.api.github.*
import dev.elide.secrets.dto.persisted.SecretMetadata
import dev.elide.secrets.exception.RemoteNotInitializedException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encode
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * GitHub [Remote] connection handling for secrets.
 *
 * @property writeAccess `true` if writing to this remote is permitted.
 * @property repository connected repository name in format `organization/repository`.
 * @property token GitHub personal access token.
 * @author Lauri Heino <datafox>
 */
internal class GithubRemote(
    override val writeAccess: Boolean,
    private val repository: String,
    private val token: String,
    private val dataHandler: DataHandler,
) : Remote {
    override suspend fun getMetadata(): ByteString? = getFile(Values.METADATA_NAME)

    override suspend fun getValidator(): ByteString? = getFile(Values.VALIDATOR_NAME)

    override suspend fun getCollection(profile: String): Pair<ByteString, ByteString> {
        val key =
            getFile(Utils.keyName(profile))
                ?: throw IllegalStateException(
                    "Key for profile \"$profile\" was not found in remote \"$repository\""
                )
        val value =
            getFile(Utils.collectionName(profile))
                ?: throw IllegalStateException(
                    "Collection for profile \"$profile\" was not found in remote \"$repository\""
                )
        return key to value
    }

    override suspend fun removeCollection(profile: String) {
        val metadataBytes =
            getMetadata() ?: throw RemoteNotInitializedException("Remote is not initialized")
        val metadata = dataHandler.deserializeMetadata(metadataBytes)
        val sha =
            metadata.collections[profile]?.sha
                ?: throw IllegalArgumentException(
                    "Profile \"$profile\" was not found in remote \"$repository\""
                )
        val (key, collection) = getCollection(profile)
        if (sha != Utils.sha(collection))
            throw IllegalStateException(
                "Remote is corrupted (collection for profile \"$profile\" SHA-1 has does not match metadata"
            )
        val keySha = Utils.sha(key)
        val branch = createBranch()
        deleteFile(Utils.collectionName(profile), "remove profile $profile collection", sha, branch)
        deleteFile(Utils.keyName(profile), "remove profile $profile key", keySha, branch)
        val newMetadata =
            metadata.copy(collections = metadata.collections.filterKeys { it != profile })
        writeFile(
            Values.METADATA_NAME,
            dataHandler.serializeMetadata(newMetadata),
            "new metadata (profile \"$profile\" removed)",
            Utils.sha(metadataBytes),
            branch,
        )
        post<GithubMergeRequest, GithubMergeResponse>(
            "repos/$repository/merges",
            token,
            GithubMergeRequest("main", branch, "merge $branch"),
            HttpStatusCode.Created,
        )
    }

    override suspend fun init(metadata: SecretMetadata, validator: ByteString) {
        writeFile(
            Values.METADATA_NAME,
            dataHandler.serializeMetadata(metadata.copy(collections = emptyMap())),
            "initial metadata",
        )
        writeFile(Values.VALIDATOR_NAME, validator, "validator")
    }

    override suspend fun update(collections: Map<String, Pair<ByteString, ByteString>>) {
        val remoteMetadataBytes =
            getMetadata() ?: throw RemoteNotInitializedException("Remote is not initialized")
        val remoteMetadata = dataHandler.deserializeMetadata(remoteMetadataBytes)
        val localMetadata = dataHandler.readMetadata()
        val branch = createBranch()
        val (newRemoteMetadata, updated) =
            createMetadata(localMetadata, remoteMetadata, collections.keys)
        val oldMetadataSha = Utils.sha(remoteMetadataBytes)
        writeFile(
            Values.METADATA_NAME,
            dataHandler.serializeMetadata(newRemoteMetadata),
            "new metadata",
            oldMetadataSha,
            branch,
        )
        updated.forEach { (sha, profile) ->
            writeFile(
                Utils.collectionName(profile),
                collections[profile]!!.second,
                "new $profile collection",
                sha,
                branch,
            )
            if (sha.isEmpty()) {
                writeFile(
                    Utils.keyName(profile),
                    collections[profile]!!.first,
                    "new $profile key",
                    "",
                    branch,
                )
            }
        }
        post<GithubMergeRequest, GithubMergeResponse>(
            "repos/$repository/merges",
            token,
            GithubMergeRequest("main", branch, "merge $branch"),
            HttpStatusCode.Created,
        )
    }

    private fun createMetadata(
        localMetadata: SecretMetadata,
        remoteMetadata: SecretMetadata,
        profiles: Set<String>,
    ): Pair<SecretMetadata, Set<Pair<String, String>>> {
        val unvisited = profiles.toMutableSet()
        val updated = mutableSetOf<Pair<String, String>>()
        return Pair(
            remoteMetadata.copy(
                collections =
                    remoteMetadata.collections.mapValues { (key, collection) ->
                        if (key in profiles) {
                            unvisited.remove(key)
                            updated.add(Pair(collection.sha, key))
                            localMetadata.collections[key]!!
                        } else collection
                    } +
                        unvisited.map {
                            updated.add(Pair("", it))
                            it to localMetadata.collections[it]!!
                        }
            ),
            updated,
        )
    }

    private suspend fun createBranch(): String {
        val (_, commits) =
            get<GithubCommitsRequest, Array<GithubCommitsResponse>>(
                "repos/$repository/commits",
                token,
                GithubCommitsRequest(1),
                HttpStatusCode.OK,
            )
        val head = commits!!.first().sha
        val branch = "update/$head"
        post<GithubCreateRefRequest, GithubCreateRefResponse>(
            "repos/$repository/git/refs",
            token,
            GithubCreateRefRequest("refs/heads/$branch", head),
            HttpStatusCode.Created,
        )
        return branch
    }

    private suspend fun getFile(path: String): ByteString? {
        val (_, content) =
            get<GithubFileResponse>(
                "repos/$repository/contents/$path",
                token,
                HttpStatusCode.OK,
                HttpStatusCode.NotFound,
            )
        if (content == null) return null
        // Use download url because sometimes base64 for a binary file comes out wrong somehow
        return ByteString(client.get(content.downloadUrl).bodyAsBytes())
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun writeFile(
        path: String,
        data: ByteString,
        message: String,
        sha: String = "",
        branch: String = "",
    ) {
        put<GithubUploadFileRequest, String>(
            "repos/$repository/contents/$path",
            token,
            GithubUploadFileRequest(message, Base64.Default.encode(data), sha, branch),
            HttpStatusCode.OK,
            HttpStatusCode.Created,
        )
    }

    private suspend fun deleteFile(path: String, message: String, sha: String, branch: String) {
        delete<GithubDeleteFileRequest, String>(
            "repos/$repository/contents/$path",
            token,
            GithubDeleteFileRequest(message, sha, branch),
            HttpStatusCode.OK,
        )
    }

    companion object {
        const val REPOSITORY_NAME = "github:repository"
        const val TOKEN_NAME = "github:token"
        const val GITHUB_URL = "https://api.github.com/"
        val client =
            HttpClient(CIO) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(HttpTimeout) {
                    requestTimeoutMillis = 10000
                    connectTimeoutMillis = 10000
                }
            }

        suspend inline fun <reified B, reified T> put(
            path: String,
            token: String,
            body: B,
            vararg allowedCodes: HttpStatusCode,
            block: HttpRequestBuilder.() -> Unit = {},
        ): Pair<HttpResponse, T?> {
            val response: HttpResponse =
                client.put("$GITHUB_URL$path") {
                    headers(token)
                    setBody(body)
                    block()
                }
            return parseContent(response, allowedCodes.toSet())
        }

        suspend inline fun <reified B, reified T> delete(
            path: String,
            token: String,
            body: B,
            vararg allowedCodes: HttpStatusCode,
            block: HttpRequestBuilder.() -> Unit = {},
        ): Pair<HttpResponse, T?> {
            val response: HttpResponse =
                client.delete("$GITHUB_URL$path") {
                    headers(token)
                    setBody(body)
                    block()
                }
            return parseContent(response, allowedCodes.toSet())
        }

        suspend inline fun <reified T> get(
            path: String,
            token: String,
            vararg allowedCodes: HttpStatusCode,
            block: HttpRequestBuilder.() -> Unit = {},
        ): Pair<HttpResponse, T?> {
            val response: HttpResponse =
                client.get("$GITHUB_URL$path") {
                    headers(token)
                    block()
                }
            return parseContent(response, allowedCodes.toSet())
        }

        suspend inline fun <reified B, reified T> get(
            path: String,
            token: String,
            body: B,
            vararg allowedCodes: HttpStatusCode,
            block: HttpRequestBuilder.() -> Unit = {},
        ): Pair<HttpResponse, T?> =
            get(path, token, *allowedCodes) {
                setBody(body)
                block()
            }

        suspend inline fun <reified B, reified T> post(
            path: String,
            token: String,
            body: B,
            vararg allowedCodes: HttpStatusCode,
            block: HttpRequestBuilder.() -> Unit = {},
        ): Pair<HttpResponse, T?> {
            val response: HttpResponse =
                client.post("$GITHUB_URL$path") {
                    headers(token)
                    setBody(body)
                    block()
                }
            return parseContent(response, allowedCodes.toSet())
        }

        suspend inline fun <reified T> parseContent(
            response: HttpResponse,
            allowedCodes: Set<HttpStatusCode>,
        ): Pair<HttpResponse, T?> {
            val content: T? =
                try {
                    response.body<T>()
                } catch (_: JsonConvertException) {
                    null
                }
            if (response.status !in allowedCodes)
                throw IllegalArgumentException(
                    "Something went wrong: $response, Content: ${content ?: response.bodyAsText()}"
                )
            return Pair(response, content)
        }

        fun HttpMessageBuilder.headers(token: String) {
            header("Accept", "application/vnd.github+json")
            header("Content-Type", "application/json")
            header("Authorization", "Bearer $token")
            header("X-GitHub-Api-Version", "2022-11-28")
        }
    }
}
