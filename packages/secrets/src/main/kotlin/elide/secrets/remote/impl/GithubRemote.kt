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

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encode
import kotlinx.io.bytestring.toHexString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import elide.secrets.Encryption
import elide.secrets.SecretUtils
import elide.secrets.SecretUtils.deserialize
import elide.secrets.SecretUtils.hash
import elide.secrets.SecretValues
import elide.secrets.dto.api.github.*
import elide.secrets.dto.persisted.RemoteMetadata
import elide.secrets.remote.Remote

/**
 * GitHub [Remote] connection handling for secrets.
 *
 * @property writeAccess `true` if writing to this remote is permitted.
 * @property repository connected repository name in format `organization/repository`.
 * @property token GitHub personal access token.
 * @property encryption [Encryption] instance.
 * @property client [HttpClient] instance.
 * @property json [Json] instance.
 * @author Lauri Heino <datafox>
 */
internal class GithubRemote(
  override val writeAccess: Boolean,
  private val repository: String,
  private val token: String,
  private val encryption: Encryption,
  private val client: HttpClient,
  private val json: Json,
) : Remote {
  override suspend fun getMetadata(): ByteString? = getFile(SecretValues.METADATA_FILE)

  override suspend fun getProfile(profile: String): ByteString? = getFile(SecretUtils.profileName(profile))

  override suspend fun getAccess(access: String): ByteString? = getFile(SecretUtils.accessName(access))

  override suspend fun getSuperAccess(): ByteString? = getFile(SecretValues.SUPER_ACCESS_FILE)

  @OptIn(ExperimentalStdlibApi::class)
  override suspend fun update(metadata: ByteString, profiles: Map<String, ByteString>) {
    val currentMetadataBytes =
      getMetadata() ?: throw IllegalStateException(SecretValues.REMOTE_NOT_INITIALIZED_EXCEPTION)
    val currentMetadata: RemoteMetadata = currentMetadataBytes.deserialize(json)
    val branch = createBranch()
    profiles.forEach { (name, bytes) ->
      val sha = currentMetadata.profiles[name]?.hash ?: ""
      writeFile(SecretUtils.profileName(name), bytes, SecretValues.changedProfileCommit(name), sha, branch)
    }
    writeFile(
      SecretValues.METADATA_FILE,
      metadata,
      SecretValues.CHANGED_METADATA_COMMIT,
      encryption.hashGitDataSHA1(currentMetadataBytes).toHexString(),
      branch,
    )
    merge(branch)
  }

  @OptIn(ExperimentalStdlibApi::class)
  override suspend fun superUpdate(
    metadata: ByteString,
    profiles: Map<String, ByteString>,
    superAccess: ByteString,
    access: Map<String, ByteString>,
    deletedProfiles: Set<String>,
    deletedAccesses: Set<String>,
  ) {
    val currentMetadataBytes = getMetadata()
    val currentMetadata: RemoteMetadata? = currentMetadataBytes?.deserialize(json)
    val currentSuperBytes = getSuperAccess()
    val branch = if (currentMetadataBytes == null) "" else createBranch()
    profiles.forEach { (name, bytes) ->
      val sha = currentMetadata?.profiles[name]?.hash ?: ""
      writeFile(SecretUtils.profileName(name), bytes, SecretValues.changedProfileCommit(name), sha, branch)
    }
    access.forEach { (name, bytes) ->
      val sha = currentMetadata?.access[name]?.hash ?: ""
      writeFile(SecretUtils.accessName(name), bytes, SecretValues.changedAccessCommit(name), sha, branch)
    }
    currentMetadata?.run {
      deletedProfiles.forEach {
        deleteFile(SecretUtils.profileName(it), SecretValues.deletedProfileCommit(it), this.profiles[it]!!.hash, branch)
      }
      deletedAccesses.forEach {
        deleteFile(SecretUtils.accessName(it), SecretValues.deletedAccessCommit(it), this.access[it]!!.hash, branch)
      }
    }
    val superSha = currentSuperBytes?.hash(encryption) ?: ""
    writeFile(SecretValues.SUPER_ACCESS_FILE, superAccess, SecretValues.CHANGED_SUPER_ACCESS_COMMIT, superSha, branch)
    val metadataSha = currentMetadataBytes?.hash(encryption) ?: ""
    writeFile(SecretValues.METADATA_FILE, metadata, SecretValues.CHANGED_METADATA_COMMIT, metadataSha, branch)
    if (branch.isNotBlank()) merge(branch)
  }

  private suspend fun createBranch(): String {
    val (_, commits) =
      client.get<GithubCommitsRequest, Array<GithubCommitsResponse>>(
        "repos/$repository/commits",
        token,
        GithubCommitsRequest(1),
        HttpStatusCode.OK,
      )
    val head = commits!!.first().sha
    val branch = "update/$head"
    client.post<GithubCreateRefRequest, GithubCreateRefResponse>(
      "repos/$repository/git/refs",
      token,
      GithubCreateRefRequest("refs/heads/$branch", head),
      HttpStatusCode.Created,
    )
    return branch
  }

  private suspend fun getFile(path: String): ByteString? {
    val (_, content) =
      client.get<GithubFileResponse>(
        "repos/$repository/contents/$path",
        token,
        HttpStatusCode.OK,
        HttpStatusCode.NotFound,
      )
    if (content == null) return null
    return ByteString(client.get(content.url).bodyAsBytes())
  }

  @OptIn(ExperimentalEncodingApi::class)
  private suspend fun writeFile(
    path: String,
    data: ByteString,
    message: String,
    sha: String = "",
    branch: String = "",
  ) {
    client.put<GithubUploadFileRequest, String>(
      "repos/$repository/contents/$path",
      token,
      GithubUploadFileRequest(message, Base64.Default.encode(data), sha, branch),
      HttpStatusCode.OK,
      HttpStatusCode.Created,
    )
  }

  @Suppress("unused")
  private suspend fun deleteFile(path: String, message: String, sha: String, branch: String) {
    client.delete<GithubDeleteFileRequest, String>(
      "repos/$repository/contents/$path",
      token,
      GithubDeleteFileRequest(message, sha, branch),
      HttpStatusCode.OK,
    )
  }

  private suspend fun merge(branch: String) {
    client.post<GithubMergeRequest, GithubMergeResponse>(
      "repos/$repository/merges",
      token,
      GithubMergeRequest("main", branch, SecretValues.mergeBranchCommit(branch)),
      HttpStatusCode.Created,
    )
  }

  companion object {

    suspend inline fun <reified B, reified T> HttpClient.put(
      path: String,
      token: String,
      body: B,
      vararg allowedCodes: HttpStatusCode,
      block: HttpRequestBuilder.() -> Unit = {},
    ): Pair<HttpResponse, T?> {
      val response: HttpResponse =
        put("${SecretValues.GITHUB_API_URL}$path") {
          headers(token)
          setBody(body)
          block()
        }
      return parseContent(response, allowedCodes.toSet())
    }

    suspend inline fun <reified B, reified T> HttpClient.delete(
      path: String,
      token: String,
      body: B,
      vararg allowedCodes: HttpStatusCode,
      block: HttpRequestBuilder.() -> Unit = {},
    ): Pair<HttpResponse, T?> {
      val response: HttpResponse =
        delete("${SecretValues.GITHUB_API_URL}$path") {
          headers(token)
          setBody(body)
          block()
        }
      return parseContent(response, allowedCodes.toSet())
    }

    suspend inline fun <reified T> HttpClient.get(
      path: String,
      token: String,
      vararg allowedCodes: HttpStatusCode,
      block: HttpRequestBuilder.() -> Unit = {},
    ): Pair<HttpResponse, T?> {
      val response: HttpResponse =
        get("${SecretValues.GITHUB_API_URL}$path") {
          headers(token)
          block()
        }
      return parseContent(response, allowedCodes.toSet())
    }

    suspend inline fun <reified B, reified T> HttpClient.get(
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

    suspend inline fun <reified B, reified T> HttpClient.post(
      path: String,
      token: String,
      body: B,
      vararg allowedCodes: HttpStatusCode,
      block: HttpRequestBuilder.() -> Unit = {},
    ): Pair<HttpResponse, T?> {
      val response: HttpResponse =
        post("${SecretValues.GITHUB_API_URL}$path") {
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
        throw IllegalArgumentException("Something went wrong: $response, Content: ${content ?: response.bodyAsText()}")
      return Pair(response, content)
    }

    private fun HttpMessageBuilder.headers(token: String) {
      header("Accept", "application/vnd.github+json")
      header("Content-Type", "application/json")
      header("Authorization", "Bearer $token")
      header("X-GitHub-Api-Version", "2022-11-28")
    }
  }
}
