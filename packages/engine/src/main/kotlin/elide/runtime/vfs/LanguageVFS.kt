/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

@file:Suppress("FUNCTION_BOOLEAN_PREFIX")

package elide.runtime.vfs

import org.graalvm.polyglot.io.FileSystem
import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListMap
import elide.runtime.vfs.LanguageVFS.LanguageVFSInfo

// Private static language VFS registry.
private val languageVfsRegistry = ConcurrentSkipListMap<String, () -> LanguageVFSInfo>()

/**
 * Register a language-specific VFS implementation for the specified [languageId].
 *
 * @param languageId Identifier for the language to register the VFS for.
 * @param provider The VFS implementation provider to register.
 */
public fun registerLanguageVfs(languageId: String, provider: () -> LanguageVFSInfo) {
  languageVfsRegistry[languageId] = provider
}

/**
 * Retrieve the registry of virtual file-system layers, bound to the corresponding language ID that registered it.
 *
 * @return Map of file-system layers.
 */
public fun languageVfsRegistry(): Map<String, () -> LanguageVFSInfo> {
  return languageVfsRegistry
}

/**
 * # Guest: Language-virtual Filesystem
 *
 * Describes the API surface of a guest language-specific virtual file system (VFS) implementation. The VFS system is
 * aware of language VFS bindings, and provides these bindings in the event the corresponding language is requested for
 * use in Elide.
 *
 * Language-specific VFS implementations declare their [languageId].
 */
public interface LanguageVFS : GuestVFS {
  /**
   * ## Language VFS Info
   *
   * Describes a configured language-level virtual file system; this includes a [fsProvider] which can be used to obtain
   * the VFS implementation, and a [router] function which decides if a path is eligible to be handled by the VFS.
   */
  public interface LanguageVFSInfo {
    /**
     * ### Language VFS: Router
     *
     * Given a [Path], determine if this language VFS instance should handle I/O.
     */
    public val router: (Path) -> Boolean

    /**
     * ### Language VFS: Provider
     *
     * Obtain an instance of the language VFS which should handle an eligible path.
     */
    public val fsProvider: () -> FileSystem
  }

  /**
   * ## Language ID
   *
   * Well-known ID of the language which this VFS implementation is associated with.
   */
  public val languageId: String

  /**
   * ## Acceptance Criteria
   *
   * Returns `true` if this VFS implementation should claim access to the provided [path].
   *
   * @param path Path to check for ownership.
   * @return Whether this VFS should own the path; defaults to `false`.
   */
  public fun accepts(path: Path): Boolean
}
