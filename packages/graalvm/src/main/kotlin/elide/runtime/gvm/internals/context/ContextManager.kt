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

package elide.runtime.gvm.internals.context

import org.graalvm.polyglot.Engine
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import elide.runtime.gvm.ContextFactory
import elide.runtime.gvm.ExecutionInputs

/**
 * TBD.
 */
public interface ContextManager<Context, Builder> : ContextFactory<Context, Builder> {
  public companion object {
    /** Default context execution timeout (hard limit). */
    private val DEFAULT_TIMEOUT = 30.seconds
  }

  /**
   * TBD.
   */
  public interface VMInvocation<T: ExecutionInputs>

  /**
   * TBD.
   */
  public fun engine(): Engine

  /**
   * TBD.
   */
  public fun <R> executeAsync(operation: Context.() -> R): CompletableFuture<R>

  /**
   * TBD.
   */
  public suspend fun <R> acquireSuspendAsync(operation: Context.() -> R): Deferred<R> = executeAsync(
    operation
  ).asDeferred()

  /**
   * TBD.
   */
  public suspend fun <R> acquireSuspend(operation: Context.() -> R): R = acquireSuspendAsync(operation).await()

  /**
   *
   */
  public fun <R> executeBlocking(timeout: Duration = DEFAULT_TIMEOUT, operation: Context.() -> R): R = executeAsync(
    operation
  ).get()
}
