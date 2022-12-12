package elide.runtime.gvm.internals

import elide.server.ServerInitializer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import org.graalvm.polyglot.Engine
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * TBD.
 */
internal interface ContextManager<Context, Builder>: ServerInitializer {
  companion object {
    /** Default context execution timeout (hard limit). */
    private val DEFAULT_TIMEOUT = 30.seconds

    /** Default allowable CPU time per context execution. */
    private val DEFAULT_CPU_MS_PER_EXECUTION = 500.milliseconds
  }

  /**
   * TBD.
   */
  fun engine(): Engine

  /**
   * TBD.
   */
  fun installContextFactory(factory: (Engine) -> Builder)

  /**
   * TBD.
   */
  fun installContextSpawn(factory: (Builder) -> Context)

  /**
   * TBD.
   */
  fun <R> executeAsync(operation: Context.() -> R): CompletableFuture<R>

  /**
   * TBD.
   */
  suspend fun <R> acquireSuspendAsync(operation: Context.() -> R): Deferred<R> = executeAsync(
    operation
  ).asDeferred()

  /**
   * TBD.
   */
  suspend fun <R> acquireSuspend(operation: Context.() -> R): R = acquireSuspendAsync(operation).await()

  /**
   * TBD.
   */
  suspend operator fun <R> invoke(operation: Context.() -> R): R = acquireSuspend(operation)

  /**
   *
   */
  fun <R> executeBlocking(timeout: Duration = DEFAULT_TIMEOUT, operation: Context.() -> R): R = executeAsync(
    operation
  ).get()
}
