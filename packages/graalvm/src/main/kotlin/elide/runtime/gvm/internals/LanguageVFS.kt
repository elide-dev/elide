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
package elide.runtime.gvm.internals

import java.nio.file.Path

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
