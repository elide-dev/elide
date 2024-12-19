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
package elide.runtime.intrinsics.js.node.childProcess

import elide.annotations.API

/**
 * ## Process Options (Identity-Enabled)
 *
 * Describes [ProcOptions], but enabled with POSIX identity attributes, such as [uid] (the user ID of an operation) and
 * [gid] (the group ID of an operation).
 *
 * @property uid User ID to run the process as.
 * @property gid Group ID to run the process as.
 */
@API public sealed interface IdentityProcOptions : ProcOptions {
  /** User ID to run a process as. */
  public val uid: Int?

  /** Group ID to run a process as. */
  public val gid: Int?
}
