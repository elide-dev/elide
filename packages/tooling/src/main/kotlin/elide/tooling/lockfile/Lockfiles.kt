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
package elide.tooling.lockfile

import java.io.IOException
import java.nio.file.Path
import java.util.LinkedList
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable
import kotlin.io.path.outputStream
import elide.tooling.deps.DependencyResolver
import elide.tooling.project.ElideProject
import elide.tooling.lockfile.ElideLockfile.*
import elide.tooling.lockfile.ElideLockfileV1.LockfileV1

/**
 * # Lockfile Utilities
 *
 * Static utilities for loading and/or creating [ElideLockfile] instances.
 */
public object Lockfiles {
  /**
   * Create a new Elide project lockfile structure from scratch.
   *
   * Fingerprints are calculated through the standard [Fingerprints] methods.
   *
   * @param root Root path to the project which will use this lockfile.
   * @param project Project information, if any.
   * @param extraContributor Extra contributor to include in the lockfile.
   * @return A new lockfile instance.
   */
  @JvmStatic public suspend fun create(
    root: Path,
    project: ElideProject? = null,
    resolvers: List<DependencyResolver> = emptyList(),
    extraContributor: LockfileContributor? = null,
  ): ElideLockfile = LockfileContributors.collect().let { contributors ->
    val stanzas = LinkedList<Stanza>()
    for (contributor in contributors) {
      contributor.contribute(root, project)?.let {
        stanzas.add(it)
      }
    }
    for (resolver in resolvers) {
      resolver.contribute(root, project)?.let {
        stanzas.add(it)
      }
    }
    extraContributor?.contribute(root, project)?.let {
      stanzas.add(it)
    }
    return build(
      stanzas = stanzas.toTypedArray(),
    )
  }

  /**
   * Create a new Elide project lockfile structure from scratch.
   *
   * Fingerprints are calculated through the standard [Fingerprints] methods.
   *
   * @param stanzas Stanzas to include in the lockfile.
   * @return A new lockfile instance.
   */
  @JvmStatic public fun build(vararg stanzas: Stanza): ElideLockfile = LockfileV1(
    version = ElideLockfileV1.LockfileVersionV1,
    fingerprint = Fingerprints.buildFrom(stanzas.toList()),
    stanzas = stanzas.toSortedSet(),
  )

  /**
   * Read a lockfile from the given path.
   *
   * If the file does not exist, or cannot be read, [NoSuchFileException] and [IOException] are thrown, respectively;
   * otherwise, the format is detected, and the file is read using the latest version of the lockfile format. If the
   * latest version fails to read, successive versions are attempted until one succeeds; if none succeed, an error is
   * thrown.
   *
   * @param path Path to the lockfile to read.
   * @param version Definition version to read with.
   * @return A new lockfile instance.
   */
  @JvmStatic public suspend fun read(
    path: Path,
    version: LockfileDefinition<*> = ElideLockfile.latest(),
  ): Pair<Format, ElideLockfile> = withContext(IO) {
    if (!path.exists()) throw NoSuchFileException(path.toFile())
    if (!path.isReadable()) throw IOException("Cannot read lockfile at path '$path'")
    val format = when (path.extension) {
      // extension for json decoding
      "json" -> Format.JSON

      // extension for binary decoding
      "bin" -> Format.BINARY

      else -> error("Unrecognized lockfile format: ${path.extension}")
    }
    path.inputStream().buffered().use {
      format to version.readFrom(format, it)
    }
  }

  /**
   * Write a lockfile to the given path.
   *
   * If the file already exists, it will be overwritten. If the file cannot be written, an [IOException] is thrown.
   *
   * @param lockfile Lockfile to write.
   * @param path Path to the lockfile to write.
   * @param format Format to use for writing the lockfile.
   */
  @JvmStatic public suspend fun write(
    lockfile: ElideLockfile,
    path: Path,
    format: Format,
    version: LockfileDefinition<*>,
  ): Unit = withContext(IO) {
    if (!path.isWritable()) throw IOException("Cannot write lockfile at path '$path' (not writable)")
    path.parent?.let { parent ->
      if (!parent.exists()) {
        parent.toFile().mkdirs()
      }
    }
    path.outputStream().buffered().use { output ->
      version.writeTo(format, lockfile, output)
    }
  }
}
