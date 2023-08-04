/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress("UnstableApiUsage")

package elide.server.runtime

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.micronaut.context.annotation.Context
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import elide.server.runtime.AppExecutor.DefaultExecutor
import elide.server.runtime.jvm.UncaughtExceptionHandler


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
public interface AppExecutor {
  /** Default settings applied to an application executor. */
  public object DefaultSettings {
    /** Default size of threads available for background execution. */
    public val poolSize: Int = (Runtime.getRuntime().availableProcessors() * 2) - 1

    /** Default priority assigned to threads for background execution. */
    public const val priority: Int = Thread.NORM_PRIORITY
  }

  public companion object {
    /**
     * Run the provided [operation] asynchronously, returning whatever result is returned by the [operation].
     *
     * The operation is executed against the default dispatcher ([Dispatchers.Default]).
     *
     * @param R Return type.
     * @param operation Operation to run. Can suspend.
     * @return Deferred task providing the result of the [operation].
     */
    @JvmStatic public suspend fun <R> async(operation: suspend () -> R): Deferred<R> {
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
    @JvmStatic public suspend fun <R> io(operation: suspend () -> R): R {
      return withContext(DefaultExecutor.ioDispatcher) {
        operation.invoke()
      }
    }
  }

  /** @return Instance of the main [Executor] held by this [AppExecutor]. */
  public fun executor(): Executor = service()

  /** @return Service-oriented instance of the main [Executor] held by this [AppExecutor]. */
  public fun service(): ListeningExecutorService

  /** Implements the application-default-executor, as a bridge to Micronaut. */
  @Context
  @Singleton
  @Suppress("unused")
  public class DefaultExecutor @Inject constructor (
    uncaughtHandler: Thread.UncaughtExceptionHandler
  ): AppExecutor {
    public companion object {
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
      @JvmStatic public fun acquire(): AppExecutor = mainExec
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
    public fun overrideExecutor(exec: ListeningScheduledExecutorService) {
      this.runner = exec
    }

    /** @inheritDoc */
    override fun service(): ListeningScheduledExecutorService = runner
  }
}
