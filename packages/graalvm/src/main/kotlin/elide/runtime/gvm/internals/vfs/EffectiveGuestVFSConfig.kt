/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.vfs

import java.net.URI

/**
 * # VFS: Effective Configuration
 *
 * This class represents the rendered and effective configuration which is applied for a given guest VM I/O strategy.
 * It is used internally as the result of combining any active [policy] instances.
 *
 * @param readOnly Whether the guest file-system should be read-only.
 * @param caseSensitive Whether the guest file-system should be case-sensitive.
 * @param supportsSymbolicLinks Whether the guest file-system should support symbolic links.
 * @param policy Guest VFS policy to apply to guest-originated file system operations.
 * @param bundle Bundle file to load the VFS file-system from, if applicable. If not present, the file-system will be
 *   empty at initialization time.
 * @param root Root directory to apply for this virtual filesystem.
 * @param workingDirectory Current-working-directory to apply for this virtual filesystem.
 */
public data class EffectiveGuestVFSConfig internal constructor (
  val readOnly: Boolean = DEFAULT_READ_ONLY,
  val caseSensitive: Boolean = DEFAULT_CASE_SENSITIVE,
  val supportsSymbolicLinks: Boolean = DEFAULT_SUPPORT_SYMBOLIC_LINKS,
  val policy: GuestVFSPolicy = DEFAULT_POLICY,
  val bundle: List<URI> = emptyList(),
  val root: String = DEFAULT_ROOT,
  val workingDirectory: String = DEFAULT_CWD,
) {
  internal companion object {
    private const val DEFAULT_READ_ONLY = true
    private const val DEFAULT_CASE_SENSITIVE = true
    private const val DEFAULT_SUPPORT_SYMBOLIC_LINKS = false
    private const val DEFAULT_ROOT = "/"
    private const val DEFAULT_CWD = "/"
    private val DEFAULT_POLICY: GuestVFSPolicy = GuestVFSPolicy.DEFAULTS

    /** Default settings. */
    @JvmStatic internal val DEFAULTS: EffectiveGuestVFSConfig = EffectiveGuestVFSConfig()

    /** Settings constructor. */
    @JvmStatic fun empty(
      readOnly: Boolean = DEFAULT_READ_ONLY,
      caseSensitive: Boolean = DEFAULT_CASE_SENSITIVE,
      supportsSymbolicLinks: Boolean = DEFAULT_SUPPORT_SYMBOLIC_LINKS,
      root: String = DEFAULT_ROOT,
      workingDirectory: String = DEFAULT_CWD,
    ): EffectiveGuestVFSConfig = EffectiveGuestVFSConfig(
      readOnly = readOnly,
      caseSensitive = caseSensitive,
      supportsSymbolicLinks = supportsSymbolicLinks,
      root = root,
      workingDirectory = workingDirectory,
    )

    /** Construct (empty) from a policy. */
    @JvmStatic fun withPolicy(
      policy: GuestVFSPolicy,
      caseSensitive: Boolean = DEFAULT_CASE_SENSITIVE,
      supportsSymbolicLinks: Boolean = DEFAULT_SUPPORT_SYMBOLIC_LINKS,
      root: String = DEFAULT_ROOT,
      workingDirectory: String = DEFAULT_CWD,
    ): EffectiveGuestVFSConfig = EffectiveGuestVFSConfig(
      caseSensitive = caseSensitive,
      supportsSymbolicLinks = supportsSymbolicLinks,
      policy = policy,
      readOnly = policy.readOnly ?: GuestVFSPolicy.DEFAULT_READ_ONLY,
      root = root,
      workingDirectory = workingDirectory,
    )
  }
}
