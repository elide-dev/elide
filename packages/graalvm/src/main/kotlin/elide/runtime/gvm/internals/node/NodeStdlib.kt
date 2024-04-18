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

package elide.runtime.gvm.internals.node

import elide.runtime.gvm.internals.node.path.NodePaths
import elide.runtime.gvm.internals.node.process.NodeProcess
import elide.runtime.intrinsics.js.node.PathAPI
import elide.runtime.intrinsics.js.node.ProcessAPI

/**
 * # Node Standard Library
 *
 * Offers lazily-initialized implementations of Node API modules. Properties are named for their equivalent Node module
 * name; each is initialized on first access.
 */
public object NodeStdlib {
  /**
   * ## `path`
   *
   * Provides access to a compliant implementation of the Node Path API, at the built-in module name `path`.
   */
  public val path: PathAPI by lazy { NodePaths.create() }

  /**
   * ## `process`
   *
   * Provides access to a compliant implementation of the Node Process API, at the built-in module name `process`.
   */
  public val process: ProcessAPI by lazy { NodeProcess.obtain() }
}
