@file:Suppress("UnstableApiUsage")

package elide.server.runtime

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.micronaut.context.annotation.Context
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor


/**
 * Defines the interface expected for an application-level executor; there is a default implementation provided by the
 * framework, which uses Guava executors integrated with Kotlin Coroutines.
 *
 * See more about Guava concurrent execution tools here:
 * https://github.com/google/guava/wiki
 *
 * See more about Kotlin Coroutines here:
 * https://kotlinlang.org/docs/coroutines-overview.html
 *
 * @see DefaultExecutor for the default [AppExecutor] implementation.
 */
@Suppress("unused")
interface AppExecutor {
  /** Default settings applied to an application executor. */
  object DefaultSettings {
    /** Default size of threads available for background execution. */
    val poolSize = (Runtime.getRuntime().availableProcessors() * 2) - 1

    /** Default priority assigned to threads for background execution. */
    const val priority = Thread.NORM_PRIORITY
  }

  companion object {
    /**
     * Run the provided [operation] asynchronously, returning whatever result is returned by the [operation].
     *
     * The operation is executed against the default dispatcher ([Dispatchers.Default]).
     *
     * @param R Return type.
     * @param operation Operation to run. Can suspend.
     * @return Deferred task providing the result of the [operation].
     */
    @JvmStatic suspend fun <R> async(operation: suspend () -> R): Deferred<R> {
      return withContext(DefaultExecutor.backgroundDispatcher) {
        async {
          operation.invoke()
        }
      }
    }

    /**
     * Run the provided I/O [operation], returning whatever result is returned by the [operation].
     *
     * The operation is executed against the I/O dispatcher ([Dispatchers.IO]).
     *
     * @param R Return type.
     * @param operation Operation to run. Can suspend.
     * @return Deferred task providing the result of the [operation].
     */
    @JvmStatic suspend fun <R> io(operation: suspend () -> R): R {
      return withContext(DefaultExecutor.ioDispatcher) {
        operation.invoke()
      }
    }

    /**
     * Run the provided [operation] on the main thread, returning whatever result is returned by the [operation].
     *
     * The operation is executed against the main dispatcher ([Dispatchers.Main]).
     *
     * @param R Return type.
     * @param operation Operation to run. Can suspend.
     * @return Deferred task providing the result of the [operation].
     */
    @JvmStatic suspend fun <R> main(operation: suspend () -> R): R {
      return withContext(DefaultExecutor.mainDispatcher) {
        operation.invoke()
      }
    }

    /**
     * Run the provided [operation] in unconfined mode, where it will start a co-routine in the caller thread, but only
     * until the first suspension point.
     *
     * The operation is executed against the "unconfined" dispatcher ([Dispatchers.Unconfined]). For more about confined
     * versus unconfined co-routines, see here:
     *
     * https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html#unconfined-vs-confined-dispatcher
     *
     * @param R Return type.
     * @param operation Operation to run. Can suspend.
     * @return Deferred task providing the result of the [operation].
     */
    @JvmStatic suspend fun <R> unconfined(operation: suspend () -> R): R {
      return withContext(DefaultExecutor.mainDispatcher) {
        operation.invoke()
      }
    }
  }

  /** @return Instance of the main [Executor] held by this [AppExecutor]. */
  fun executor(): Executor = service()

  /** @return Service-oriented instance of the main [Executor] held by this [AppExecutor]. */
  fun service(): ListeningExecutorService

  /** Implements the application-default-executor, as a bridge to Micronaut. */
  @Context
  @Singleton
  @Suppress("unused")
  class DefaultExecutor @Inject constructor (
    uncaughtHandler: Thread.UncaughtExceptionHandler
  ): AppExecutor {
    companion object {
      /** Uncaught exception handler (global). */
      private val errHandler = UncaughtExceptionHandler()

      /** Main executor. */
      private val mainExec = DefaultExecutor(errHandler)

      /** IO dispatcher. */
      internal val ioDispatcher = Dispatchers.IO

      /** IO dispatcher. */
      internal val unconfinedDispatcher = Dispatchers.Unconfined

      /** Work dispatcher. */
      internal val backgroundDispatcher = Dispatchers.Default

      /** Main thread dispatcher. */
      internal val mainDispatcher = Dispatchers.Main

      /** Acquire the main application executor. */
      @JvmStatic fun acquire(): AppExecutor = mainExec
    }

    /** Base factory for acquiring threads. */
    private val threadFactory = ThreadFactoryBuilder()
      .setNameFormat("app-%d")
      .setPriority(DefaultSettings.priority)
      .setUncaughtExceptionHandler(uncaughtHandler)
      .build()

    /** Wrapped executor for use with Guava via listenable futures. */
    private var runner: ListeningScheduledExecutorService = MoreExecutors.listeningDecorator(
      MoreExecutors.getExitingScheduledExecutorService(
        ScheduledThreadPoolExecutor(
          DefaultSettings.poolSize,
          threadFactory
        )
      )
    )

    /**
     * Override the active main application executor with the provided [exec] service.
     */
    fun overrideExecutor(exec: ListeningScheduledExecutorService) {
      this.runner = exec
    }

    /** @inheritDoc */
    override fun service(): ListeningScheduledExecutorService = runner
  }
}
