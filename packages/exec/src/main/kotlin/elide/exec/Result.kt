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
 * Transform a regular [kotlin.Result] into an [elide.exec.Result].
 *
 * @receiver Kotlin result
 * @return Execution result
 */
public fun <T> kotlin.Result<T>.asExecResult(): Result = when (this.isSuccess) {
  false -> this.exceptionOrNull()?.let { Result.ThrowableFailure(it) } ?: Result.UnspecifiedFailure
  true -> when (val value = this.getOrNull()) {
    is Result -> value
    is kotlin.Result<*> -> value.asExecResult()
    null, Unit, is Unit -> Result.Nothing
    else -> Result.Something(value)
  }
}

/**
 *
 */
@Serializable
public sealed interface Result {
  /** Indicate whether this task was successful, meaning it ran without errors (as applicable). */
  public val isSuccess: Boolean

  /** Provide the failure exception for this result, if applicable. */
  public fun exceptionOrNull(): Throwable?

  /**
   * A successful result which yields nothing.
   */
  public data object Nothing : Result {
    override val isSuccess: Boolean get() = true
    override fun exceptionOrNull(): Throwable? = null
    override fun toString(): String = "Nothing"
  }

  /**
   * A successful result which yields a value of some kind.
   */
  public class Something<V: Any>(public val value: V) : Result {
    override val isSuccess: Boolean get() = true
    override fun exceptionOrNull(): Throwable? = null
    override fun toString(): String = "Something(${value})"
  }

  /**
   * A failed result which yields a [Throwable].
   */
  public class ThrowableFailure<E: Throwable>(public val err: E) : Result {
    override val isSuccess: Boolean get() = false
    override fun exceptionOrNull(): Throwable? = err
    override fun toString(): String = "ThrowableFailure(${err::class.simpleName}): ${err.message ?: "No message"}"
  }

  /**
   * A failed result which yields nothing.
   */
  public data object UnspecifiedFailure : Result {
    override val isSuccess: Boolean get() = false
    override fun exceptionOrNull(): Throwable? = null
    override fun toString(): String = "UnspecifiedFailure"
  }
}
