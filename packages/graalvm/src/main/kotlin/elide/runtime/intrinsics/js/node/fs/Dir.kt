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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyIterable
import elide.annotations.API
import elide.runtime.interop.ReadOnlyProxyObject
import elide.vm.annotations.Polyglot

/**
 * ## Node Filesystem: Directory
 */
@API public interface Dir : AutoCloseable, ProxyIterable, ReadOnlyProxyObject {
  /**
   * Public access to the original path used to create this instance.
   */
  @get:Polyglot public val path: String

  /**
   * Close the underlying resource for this directory instance, and then invoke the provided [callback].
   *
   * @param callback Callback to invoke after closing
   */
  @Polyglot public fun close(callback: Value)

  /**
   * Close the underlying resource for this directory instance, and then invoke the provided [callback].
   *
   * @param callback Callback to invoke after closing
   */
  public fun close(callback: () -> Unit)

  /**
   * Synchronously close underlying file resources.
   */
  @Polyglot public fun closeSync()

  /**
   * Asynchronously read the next directory entry via `readdir(3)` as an instance of [Dirent].
   */
  @Polyglot public fun read(callback: Value)

  /**
   * Synchronously read the next directory entry as an instance of [Dirent].
   */
  @Polyglot public fun readSync(): Dirent?
}
