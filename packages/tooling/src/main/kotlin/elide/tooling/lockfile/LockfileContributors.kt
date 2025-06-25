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
package elide.tooling.lockfile

import java.util.ServiceLoader

/**
 * # Lockfile Contributors
 *
 * Static utilities for loading lockfile contributors and allowing them a chance to contribute to the lockfile.
 */
public object LockfileContributors {
  /**
   * Collect all visible/installed lockfile contributors.
   *
   * @return A list of all lockfile contributors.
   */
  @JvmStatic public fun collect(): Sequence<LockfileContributor> {
    return ServiceLoader.load<LockfileContributor>(LockfileContributor::class.java).asSequence()
  }
}
