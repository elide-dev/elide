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
internal interface ContextManager<Context, Builder> : ContextFactory<Context, Builder> {
  companion object {
    /** Default context execution timeout (hard limit). */
    private val DEFAULT_TIMEOUT = 30.seconds
  }

  /**
   * TBD.
   */
  interface VMInvocation<T : ExecutionInputs>

  /**
   * TBD.
   */
  fun engine(): Engine

  /**
   * TBD.
   */
  fun <R> executeAsync(operation: Context.() -> R): CompletableFuture<R>

  /**
   * TBD.
   */
  suspend fun <R> acquireSuspendAsync(operation: Context.() -> R): Deferred<R> = executeAsync(
    operation,
  ).asDeferred()

  /**
   * TBD.
   */
  suspend fun <R> acquireSuspend(operation: Context.() -> R): R = acquireSuspendAsync(operation).await()

  /**
   *
   */
  fun <R> executeBlocking(timeout: Duration = DEFAULT_TIMEOUT, operation: Context.() -> R): R = executeAsync(
    operation,
  ).get()
}
