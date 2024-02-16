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

package elide.runtime.gvm.internals.vfs

import elide.testing.annotations.TestCase

/** Tests for the host-based VFS implementation. */
@TestCase internal class HostVFSTest : AbstractVFSTest<HostVFSImpl, HostVFSImpl.Builder, HostVFSImpl.HostVFSFactory>() {
  /** @return Host VFS factory. */
  override fun factory() = HostVFSImpl.HostVFSFactory

  /** @return New builder. */
  override fun newBuilder(): HostVFSImpl.Builder = HostVFSImpl.Builder.newBuilder()

  /** @return Indication that host changes are expected. */
  override fun shouldUseHost(): Boolean = true
}
