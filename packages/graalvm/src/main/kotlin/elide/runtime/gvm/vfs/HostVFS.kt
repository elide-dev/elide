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

package elide.runtime.gvm.vfs

import java.nio.file.Path
import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.vfs.HostVFSImpl

/** Public access to the factory for bridged host I/O access. */
public object HostVFS {
  /** @return Factory for producing new instances of [HostVFSImpl]. */
  public fun acquire(): GuestVFS = HostVFSImpl.Builder.newBuilder().build()

  /** @return Factory for producing new writable instances of [HostVFSImpl]. */
  public fun acquireWritable(): GuestVFS = HostVFSImpl.Builder.newBuilder()
    .setReadOnly(false)
    .build()

  /** @return Factory for producing a scoped Host I/O provider. */
  public fun scopedTo(path: Path, writable: Boolean = false): GuestVFS = HostVFSImpl.Builder.newBuilder()
    .setScope(path)
    .setReadOnly(!writable)
    .build()

  /** @return Factory for producing a scoped Host I/O provider. */
  public fun scopedTo(path: String, writable: Boolean = false): GuestVFS = HostVFSImpl.Builder.newBuilder()
    .setScope(path)
    .setWorkingDirectory("/")  // @TODO: OS-specific? configurable?
    .setReadOnly(!writable)
    .build()
}
