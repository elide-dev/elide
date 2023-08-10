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

package elide.runtime.gvm.internals

import org.graalvm.polyglot.io.FileSystem
import java.io.Closeable
import java.net.URI
import elide.runtime.gvm.internals.AbstractVMEngine.RuntimeInfo
import elide.runtime.gvm.internals.AbstractVMEngine.RuntimeVFS

/**
 * # Guest: Virtual Filesystem
 *
 * Describes the API surface of a guest virtual file system (VFS) implementation. The VFS system is used to fully
 * virtualize and isolate I/O for guest languages running within an Elide application. By default, the guest VFS
 * implementation functions as a read-only, TAR-backed file system.
 *
 * If desired, the VFS implementation can be replaced by one which accesses host I/O facilities directly (via flags,
 * configuration, and so on).
 */
public interface GuestVFS : FileSystem, Closeable, AutoCloseable {
  /**
   * ## Guest VFS: Host file access.
   *
   * Indicates whether this VFS implementation allows access to host-side I/O for regular files on-disk. Unless I/O for
   * guest->host access is enabled, this is always `false`.
   *
   * @return Whether host file access is supported, or allowed, by this VFS implementation.
   */
  public fun allowsHostFileAccess(): Boolean

  /**
   * ## Guest VFS: Host socket access.
   *
   * Indicates whether this VFS implementation allows access to host-side I/O for native sockets. Unless I/O for
   * guest->host access is enabled, this is always `false`.
   *
   * @return Whether host socket access is supported, or allowed, by this VFS implementation.
   */
  public fun allowsHostSocketAccess(): Boolean

  /**
   * ## Guest VFS: Configurator
   *
   * Implementations of this interface are accessed via dependency injection at the time a VFS configuration is created,
   * so that they may participate in configuration of the VFS implementation. This entrypoint is typically only for
   * internal use.
   */
  public interface VFSConfigurator {
    /**
     * ## VFS: Image
     *
     * Early-load bundle URI which is specialized with native libraries for the active host architecture and OS.
     * This bundle is loaded before any other bundles. Only one image bundle may be specified; not all runtimes support
     * or require this feature.
     *
     * @return URI for the right image bundle to load, based on the active host architecture and OS.
     */
    public fun image(): RuntimeVFS? = null

    /**
     * ## VFS: Bundles
     *
     * Add bundles to the list of file-system bundles which are due to be loaded. In embedded circumstances, these
     * bundles must be loaded before any user-provided code or filesystem data. When operating in a host I/O context,
     * the bundles are loaded to a temporary space.
     *
     * @return Bundles to add to the visible set.
     */
    public fun bundles(): List<URI>
  }
}
