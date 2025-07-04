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
package elide.runtime.intrinsics.js.node.fs

import elide.annotations.API
import elide.runtime.interop.ReadOnlyProxyObject
import elide.vm.annotations.Polyglot

/**
 * ## Node Filesystem: Directory Entry
 *
 * Corresponds to the `fs.Dirent` class structure provided by Node's built-in Filesystem API; such structures yield
 * during streaming of directory contents. Each instance describes the name of a streamed directory entry, plus some
 * boolean flags indicating the type of entry.
 *
 * [Node.js API](https://nodejs.org/docs/latest/api/fs.html#class-fsdirent)
 *
 * @see Dir Directory streaming
 */
@API public interface Dirent : ReadOnlyProxyObject {
  /**
   * Provides the name of this entry; this corresponds to the file-name if the entry represents a file, or the directory
   * name if the entry represents a directory.
   */
  @get:Polyglot public val name: String

  /**
   * Parent path which contains this directory entry.
   */
  @get:Polyglot public val parentPath: String

  /**
   * Indicates whether this directory entry represents a directory (a nested directory).
   */
  @get:Polyglot public val isDirectory: Boolean

  /**
   * Indicates whether this directory entry represents a file.
   */
  @get:Polyglot public val isFile: Boolean

  /**
   * Indicates whether this directory entry represents a symbolic link.
   */
  @get:Polyglot public val isSymbolicLink: Boolean

  /**
   * Indicates whether this directory entry represents a block device.
   *
   * Note: This is always `false` on Elide.
   */
  @get:Polyglot public val isBlockDevice: Boolean

  /**
   * Indicates whether this directory entry represents a character device.
   *
   * Note: This is always `false` on Elide.
   */
  @get:Polyglot public val isCharacterDevice: Boolean

  /**
   * Indicates whether this directory entry represents a first-in-first-out queue.
   *
   * Note: This is always `false` on Elide.
   */
  @get:Polyglot public val isFIFO: Boolean

  /**
   * Indicates whether this directory entry represents a socket.
   *
   * Note: This is always `false` on Elide.
   */
  @get:Polyglot public val isSocket: Boolean
}
