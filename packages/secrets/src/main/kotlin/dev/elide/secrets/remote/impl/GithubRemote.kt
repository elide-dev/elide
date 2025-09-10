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

import dev.elide.secrets.Encryption
import dev.elide.secrets.Utils
import dev.elide.secrets.Utils.deserialize
import dev.elide.secrets.Values
import dev.elide.secrets.dto.api.github.*
import dev.elide.secrets.dto.persisted.RemoteMetadata
import dev.elide.secrets.dto.persisted.SuperAccess
import dev.elide.secrets.remote.Remote
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.util.decodeBase64Bytes
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encode
import kotlinx.io.bytestring.toHexString
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
  private val encryption: Encryption,
  private val client: HttpClient,
  private val json: Json,
) : Remote {
  override suspend fun getMetadata(): ByteString? = getFile(Values.METADATA_FILE)

  override suspend fun getProfile(profile: String): ByteString? = getFile(Utils.profileName(profile))

  override suspend fun getAccess(access: String): ByteString? = getFile(Utils.accessName(access))

  override suspend fun getSuperAccess(): ByteString? = getFile(Values.SUPER_ACCESS_FILE)

  @OptIn(ExperimentalStdlibApi::class)
  override suspend fun update(
    metadata: ByteString,
    profiles: Map<String, ByteString>
  ) {
    val currentMetadataBytes = getMetadata() ?: throw IllegalStateException("Remote not initialized, use remote management instead")
    val currentMetadata: RemoteMetadata = currentMetadataBytes.deserialize(json)
    val branch = createBranch()
    profiles.forEach { (name, bytes) ->
      val sha = currentMetadata.profiles[name]?.hash ?: ""
      writeFile(Utils.profileName(name), bytes, "Changed profile $name", sha, branch)
    }
    writeFile(Values.METADATA_FILE, metadata, "Changed metadata", encryption.hashGitDataSHA1(currentMetadataBytes).toHexString(), branch)
    merge(branch)
  }

  @OptIn(ExperimentalStdlibApi::class)
  override suspend fun superUpdate(
    metadata: ByteString,
    profiles: Map<String, ByteString>,
    superAccess: ByteString,
    access: Map<String, ByteString>
  ) {
    val currentMetadataBytes = getMetadata()
    val currentMetadata: RemoteMetadata? = currentMetadataBytes?.deserialize(json)
    val currentSuperBytes = getSuperAccess()
    val branch = if (currentMetadataBytes == null) "" else createBranch()
    profiles.forEach { (name, bytes) ->
      val sha = currentMetadata?.profiles[name]?.hash ?: ""
      writeFile(Utils.profileName(name), bytes, "Changed profile $name", sha, branch)
    }
    access.forEach { (name, bytes) ->
      val sha = currentMetadata?.access[name]?.hash ?: ""
      writeFile(Utils.accessName(name), bytes, "Changed access $name", sha, branch)
    }
    val superSha = currentSuperBytes?.let { encryption.hashGitDataSHA1(it) }?.toHexString() ?: ""
    writeFile(Values.SUPER_ACCESS_FILE, superAccess, "Changed super access", superSha, branch)
    val metadataSha = currentMetadataBytes?.let { encryption.hashGitDataSHA1(it) }?.toHexString() ?: ""
    writeFile(Values.METADATA_FILE, metadata, "Changed metadata", metadataSha, branch)
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
        "raw",
        HttpStatusCode.OK,
        HttpStatusCode.NotFound,
      )
    if (content == null) return null
    return ByteString(content.content.decodeBase64Bytes())
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

  private suspend fun deleteFile(path: String, message: String, sha: String, branch: String) {
    client.delete<GithubDeleteFileRequest, String>(
      "repos/$repository/contents/$path",
      token,
      GithubDeleteFileRequest(message, sha, branch),
      HttpStatusCode.OK,
    )
  }

  private suspend fun merge(branch: String) {
    client.post<GithubMergeRequest, GithubMergeResponse>("repos/$repository/merges", token, GithubMergeRequest("main", branch, "Merge branch $branch"), HttpStatusCode.OK)
  }

  companion object {
    const val REPOSITORY_NAME = "github:repository"
    const val TOKEN_NAME = "github:token"
    const val GITHUB_URL = "https://api.github.com/"

    suspend inline fun <reified B, reified T> HttpClient.put(
      path: String,
      token: String,
      body: B,
      vararg allowedCodes: HttpStatusCode,
      block: HttpRequestBuilder.() -> Unit = {},
    ): Pair<HttpResponse, T?> {
      val response: HttpResponse =
        put("$GITHUB_URL$path") {
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
        delete("$GITHUB_URL$path") {
          headers(token)
          setBody(body)
          block()
        }
      return parseContent(response, allowedCodes.toSet())
    }

    suspend inline fun <reified T> HttpClient.get(
      path: String,
      token: String,
      classifier: String = "",
      vararg allowedCodes: HttpStatusCode,
      block: HttpRequestBuilder.() -> Unit = {},
    ): Pair<HttpResponse, T?> {
      val response: HttpResponse =
        get("$GITHUB_URL$path") {
          headers(token, classifier)
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
      get(path, token, "", *allowedCodes) {
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
        post("$GITHUB_URL$path") {
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

    private fun HttpMessageBuilder.headers(token: String, classifier: String = "") {
      val classifier = classifier.let { if (it.isBlank()) "" else ".$it" }
      header("Accept", "application/vnd.github$classifier+json")
      header("Content-Type", "application/json")
      header("Authorization", "Bearer $token")
      header("X-GitHub-Api-Version", "2022-11-28")
    }
  }
}
