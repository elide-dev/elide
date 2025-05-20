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
public sealed interface Result {
  /** Indicate whether this task was successful, meaning it ran without errors (as applicable). */
  public val isSuccess: Boolean

  /**
   * A successful result which yields nothing.
   */
  public data object Nothing : Result {
    override val isSuccess: Boolean get() = true
  }

  /**
   * A successful result which yields a value of some kind.
   */
  public class Something<V: Any>(public val value: V) : Result {
    override val isSuccess: Boolean get() = true
  }

  /**
   * A failed result which yields a [Throwable].
   */
  public class ThrowableFailure<E: Throwable>(public val err: E) : Result {
    override val isSuccess: Boolean get() = false
  }

  /**
   * A failed result which yields nothing.
   */
  public data object UnspecifiedFailure : Result {
    override val isSuccess: Boolean get() = false
  }
}
