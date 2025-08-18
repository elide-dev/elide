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
import dev.elide.secrets.SecretsState
import dev.elide.secrets.Utils
import dev.elide.secrets.Values
import dev.elide.secrets.dto.persisted.SecretMetadata
import dev.elide.secrets.exception.RemoteNotInitializedException
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemPathSeparator
import kotlinx.io.readByteString
import kotlinx.io.write

/**
 * Local [Remote] connection handling for secrets.
 *
 * @property writeAccess always `true`.
 * @property path path to the folder to store secrets into.
 * @author Lauri Heino <datafox>
 */
internal class LocalRemote(path: String, private val dataHandler: DataHandler) : Remote {
  private val path = parsePath(path)
  override val writeAccess = true

  override suspend fun getMetadata(): ByteString? = getFile(Values.METADATA_NAME)

  override suspend fun getValidator(): ByteString? = getFile(Values.VALIDATOR_NAME)

  override suspend fun getCollection(profile: String): Pair<ByteString, ByteString> {
    val key =
      getFile(Utils.keyName(profile)) ?: throw IllegalStateException("Key for profile \"$profile\" was not found in")
    val value =
      getFile(Utils.collectionName(profile))
        ?: throw IllegalStateException("Collection for profile \"$profile\" was not found")
    return key to value
  }

  override suspend fun removeCollection(profile: String) {
    val metadataBytes = getMetadata() ?: throw RemoteNotInitializedException("Remote is not initialized")
    val metadata = dataHandler.deserializeMetadata(metadataBytes)
    val sha =
      metadata.collections[profile]?.sha
        ?: throw IllegalArgumentException("Profile \"$profile\" was not found in remote")
    val (_, collection) = getCollection(profile)
    if (sha != Utils.sha(collection))
      throw IllegalStateException(
        "Remote is corrupted (collection for profile \"$profile\" SHA-1 hash does not match metadata)"
      )
    deleteFile(Utils.collectionName(profile))
    deleteFile(Utils.keyName(profile))
    val newMetadata = metadata.copy(collections = metadata.collections.filterKeys { it != profile })
    writeFile(Values.METADATA_NAME, dataHandler.serializeMetadata(newMetadata))
  }

  override suspend fun init(metadata: SecretMetadata, validator: ByteString) {
    writeFile(Values.METADATA_NAME, dataHandler.serializeMetadata(metadata.copy(collections = emptyMap())))
    writeFile(Values.VALIDATOR_NAME, validator)
  }

  override suspend fun update(collections: Map<String, Pair<ByteString, ByteString>>) {
    val remoteMetadataBytes = getMetadata() ?: throw RemoteNotInitializedException("Remote is not initialized")
    val remoteMetadata = dataHandler.deserializeMetadata(remoteMetadataBytes)
    val localMetadata = dataHandler.readMetadata()
    val (newRemoteMetadata, updated) = Remote.createMetadata(localMetadata, remoteMetadata, collections.keys)
    writeFile(Values.METADATA_NAME, dataHandler.serializeMetadata(newRemoteMetadata))
    updated.forEach { (sha, profile) ->
      writeFile(Utils.collectionName(profile), collections[profile]!!.second)
      if (sha.isEmpty()) {
        writeFile(Utils.keyName(profile), collections[profile]!!.first)
      }
    }
  }

  private fun getFile(path: String): ByteString? {
    val path = Path(this.path, path)
    if (!SystemFileSystem.exists(path)) return null
    return SystemFileSystem.source(path).buffered().use { it.readByteString() }
  }

  private fun writeFile(path: String, data: ByteString) {
    val path = Path(this.path, path)
    if (!SystemFileSystem.exists(path)) SystemFileSystem.createDirectories(path)
    SystemFileSystem.sink(path).buffered().use { it.write(data) }
  }

  private fun deleteFile(path: String) {
    val path = Path(this.path, path)
    if (SystemFileSystem.exists(path)) SystemFileSystem.delete(path)
  }

  companion object {
    const val PATH_NAME = "local:path"
    private const val WINDOWS_ABSOLUTE_PATH_PATTERN = "^[A-Z]:\\\\.*"

    fun parsePath(path: String): Path {
      return if (path.startsWith('/')) parseUnixAbsolutePath(path)
      else if (path.matches(Regex(WINDOWS_ABSOLUTE_PATH_PATTERN))) parseWindowsAbsolutePath(path)
      else parseRelativePath(path)
    }

    private fun parseUnixAbsolutePath(path: String): Path {
      val path = path.substring(1)
      val split = path.split('/')
      return if (split.size == 1) Path(split.first())
      else Path(split.first(), *split.subList(1, split.size).toTypedArray())
    }

    private fun parseWindowsAbsolutePath(path: String): Path {
      val split = path.split('\\')
      return if (split.size == 1) Path(split.first())
      else Path(split.first(), *split.subList(1, split.size).toTypedArray())
    }

    private fun parseRelativePath(path: String): Path {
      val base = runBlocking { SecretsState.get().path }.parent!!
      return Path(base, *path.split(SystemPathSeparator).toTypedArray())
    }
  }
}
