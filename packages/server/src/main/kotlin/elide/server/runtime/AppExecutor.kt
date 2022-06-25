@file:Suppress("UnstableApiUsage")

package elide.server.runtime

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.micronaut.context.annotation.Context
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor


/**
 *
 */
interface AppExecutor {
  /** */
  object DefaultSettings {
    /** */
    const val poolSize = 4

    /** */
    const val priority = Thread.NORM_PRIORITY
  }

  /** @return Instance of the main [Executor] held by this [AppExecutor]. */
  fun executor(): Executor = service()

  /** @return Service-oriented instance of the main [Executor] held by this [AppExecutor]. */
  fun service(): ListeningScheduledExecutorService

  /** Implements the application-default-executor, as a bridge to Micronaut. */
  @Context
  @Singleton
  class DefaultExecutor @Inject constructor (
    uncaughtHandler: Thread.UncaughtExceptionHandler
  ): AppExecutor {
    companion object {
      /** Uncaught exception handler (global). */
      private val errHandler = UncaughtExceptionHandler()

      /** Main executor. */
      private val mainExec = DefaultExecutor(errHandler)

      /** Acquire the main application executor. */
      @JvmStatic
      fun acquire(): AppExecutor = mainExec
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
