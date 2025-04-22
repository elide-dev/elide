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

import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable
public enum class Status {
  /**
   * This element has no status; [READY] is implied.
   */
  NONE,

  /**
   *
   */
  QUEUED,

  /**
   *
   */
  PENDING,

  /**
   *
   */
  RUNNING,

  /**
   *
   */
  READY,

  /**
   *
   */
  FAIL,

  /**
   *
   */
  SUCCESS;

  /** Describes whether this is a terminal status. */
  public val terminal: Boolean get() = when (this) {
    QUEUED, PENDING, RUNNING, READY, NONE -> false
    FAIL, SUCCESS -> true
  }

  /** Flips to `true` when this status is successful. */
  public val success: Boolean get() = when (this) {
    SUCCESS, READY, NONE -> true
    else -> false
  }

  /** Flips to `true` when this status is a failure. */
  public val failure: Boolean get() = when (this) {
    FAIL -> true
    else -> false
  }
}
