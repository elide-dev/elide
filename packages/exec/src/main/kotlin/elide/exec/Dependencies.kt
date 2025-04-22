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

package elide.exec

import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable

/**
 * # Dependencies
 */
@Serializable
public sealed interface Dependencies : Satisfiable {
  /**
   *
   */
  public data object None : Dependencies {
    private val emptyJob by lazy { Job() }
    override val status: Status get() = Status.READY
    override val job: Job get() = emptyJob
  }

  /**
   *
   */
  public data object Implied : Dependencies {
    private val emptyJob by lazy { Job() }
    override val status: Status get() = Status.NONE
    override val job: Job get() = emptyJob
  }
}
