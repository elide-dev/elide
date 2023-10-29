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

package elide.runtime.plugins.vfs

import java.net.URI
import java.net.URL
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.HostPlatform
import elide.runtime.core.HostPlatform.OperatingSystem.*

/** Configuration DSL for the [Vfs] plugin. */
@DelicateElideApi public class VfsConfig internal constructor(platform: HostPlatform) {
  /** Private mutable list of registered bundles. */
  private val bundles: MutableList<URI> = mutableListOf()

  /** Internal list of bundles registered for use in the VFS. */
  internal val registeredBundles: List<URI> get() = bundles

  /** Whether the file system is writable. If false, write operations will throw an exception. */
  public var writable: Boolean = false

  /** A path to be used as root for the VFS; defaults to "`/`" on Unix, and "`C:/`" on Windows. */
  public var root: String = resolveDefaultVfsRoot(platform)

  /** A path to be used as current working directory (cwd); defaults to "`/`" on Unix, and "`C:/`" on Windows. */
  public var workingDirectory: String = resolveDefaultWorkingDirectory(platform)

  /**
   * Whether to use the host's file system instead of an embedded VFS. If true, bundles registered using [include] will
   * not be applied.
   */
  internal var useHost: Boolean = false

  /** Register a [bundle] to be added to the VFS on creation. */
  public fun include(bundle: URI) {
    bundles.add(bundle)
  }

  public companion object {
    /**
     * Path to be used as root for the virtual file system when running on Windows platforms. Since the in-memory file
     * system implementation does not support using "/" as root on Windows.
     */
    private const val VFS_ROOT_WINDOWS = "C:/"

    /** Path to be used as root for the virtual file system when running on Unix platforms. */
    private const val VFS_ROOT_UNIX = "/"

    /**
     * Resolve a [platform]-specific default value for the VFS root, to avoid issues with unsupported roots when
     * running on Windows.
     */
    public fun resolveDefaultVfsRoot(platform: HostPlatform): String = when (platform.os) {
      WINDOWS -> VFS_ROOT_WINDOWS
      LINUX -> VFS_ROOT_UNIX
      DARWIN -> VFS_ROOT_UNIX
    }

    /**
     * Resolve a [platform]-specific default value for the working directory, to avoid issues with unsupported roots
     * when running on Windows.
     */
    public fun resolveDefaultWorkingDirectory(platform: HostPlatform): String {
      return resolveDefaultVfsRoot(platform)
    }
  }
}

/** Include a bundle by its [url]. This is a shortcut for calling `include(url.toURI())`. */
@DelicateElideApi public fun VfsConfig.include(url: URL) {
  include(url.toURI())
}

/** Include a bundle using a URI string. The string must be a properly formatted URI. */
@DelicateElideApi public fun VfsConfig.include(uriString: String) {
  include(URI.create(uriString))
}
