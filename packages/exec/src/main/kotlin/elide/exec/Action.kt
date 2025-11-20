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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.coroutines.CoroutineContext

/**
 * # Action
 */
@Serializable
public sealed interface Action {
  /**
   * ## Inert Action
   */
  public data object Inert : Action {
    // Always nothing.
    override suspend fun execute(ctx: ActionScope): Result = Result.Nothing
  }

  public sealed interface ActionContext : Context

  public fun interface SuspendFn : Action {
    override suspend fun execute(ctx: ActionScope): Result {
      return invoke(ctx, ctx.taskScope)
    }

    public suspend fun invoke(ctx: ActionScope, scope: ActionScope.TaskGraphScope): Result
  }

  public fun interface ActionFn : Action {
    override suspend fun execute(ctx: ActionScope): Result {
      return invoke(ctx, ctx.taskScope)
    }

    public fun invoke(ctx: ActionScope, scope: ActionScope.TaskGraphScope): Result
  }

  /**
   * Execute this action in the provided [ActionContext].
   *
   * @receiver Context in which this action should run
   * @return Result of the action execution
   */
  public suspend fun execute(ctx: ActionScope): Result

  public suspend operator fun invoke(scope: ActionScope): Result {
    return execute(scope)
  }

  /**
   * ## Default Action Context
   */
  public class DefaultActionContext : ActionContext

  /** Factories for creating or obtaining [Action] instances. */
  public companion object {
    /**
     * Create an action context from scratch.
     *
     * @return An [ActionContext] instance
     */
    @JvmStatic public fun context(): ActionContext = DefaultActionContext()

    /**
     * Create an action scope from scratch or from the provided parameters.
     *
     * @param context The action context to use
     * @param within The coroutine context to use
     * @return An [ActionScope] wrapping the provided context and coroutine context
     */
    @JvmStatic
    public fun scope(context: ActionContext = context(), within: CoroutineContext = Dispatchers.Default): ActionScope {
      return ActionScope.create(context, within)
    }

    /**
     * Create an action context from scratch or from the provided parameters, and then run the provided block within it.
     *
     * @param context The action context to use; if not provided, one is created via [context].
     * @param within The coroutine context to use; if not provided, [Dispatchers.Default] is used.
     * @param block The block to run within the action context
     * @return Return value from the [block]
     */
    @JvmStatic public suspend fun <R> withScope(
      context: ActionContext = context(),
      within: CoroutineContext = Dispatchers.Default,
      block: suspend ActionScope.() -> R,
    ): R {
      return ActionScope.create(
        context,
        within,
      ).let { actionScope ->
        actionScope.use {
          block.invoke(it)
        }
      }
    }

    /**
     * Wrap the provided [block] as an [ActionFn].
     *
     * @param block The block to wrap
     * @return An [ActionFn] wrapping the provided block
     */
    @JvmStatic public fun of(ctx: ActionContext, block: ActionContext.() -> Result,): Action =
      ActionFn @JvmSerializableLambda { _, _ ->
        block.invoke(ctx)
      }

    /**
     * Wrap the provided [job] as an [ActionFn].
     *
     * @param job The co-routine job to wrap
     * @return An [ActionFn] wrapping the provided block
     */
    @JvmStatic public fun of(job: Deferred<Result>): Action = SuspendFn @JvmSerializableLambda { ctx, _ ->
      withContext(ctx.coroutineContext) {
        job.await()
      }
    }
  }
}
