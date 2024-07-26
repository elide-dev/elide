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
package elide.runtime.exec

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.micronaut.context.annotation.Requires
import java.util.concurrent.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlin.coroutines.CoroutineContext
import elide.annotations.Eager
import elide.annotations.Factory
import elide.annotations.Singleton

/**
 * # Guest Executor Provider
 *
 * Factory interface for creating a [GuestExecutor] early in the runtime boot process; see [GuestExecutorFactory], and
 * note that this interface is overridden for testing purposes.
 */
@FunctionalInterface public fun interface GuestExecutorProvider {
  public fun executor(): GuestExecutor
}

/**
 * Guest Executor Factory
 *
 * Initializes the [GuestExecution.workStealing] executor and mounts it within the injection context; the guest executor
 * is initialized eagerly at runtime boot.
 */
@Requires(notEnv = ["test"])
@Eager @Factory internal class GuestExecutorFactory : GuestExecutorProvider {
  @Singleton override fun executor(): GuestExecutor = GuestExecution.workStealing()
}

/**
 * # Guest Execution Utilities
 *
 * Provides pre-made implementations of [GuestExecutor] classes, which are used at guest VM run-time to provide managed
 * and background-enabled execution services, which are aware of lock-state with regard to guest context.
 *
 * @see [GuestExecutor] main `GuestExecutor` interface.
 */
@Suppress("DuplicatedCode")
public object GuestExecution {
  // Direct executor instance, which invokes the target operation within the calling thread, blocking until completion.
  private val directExecutor: GuestExecutor by lazy {
    val followup = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor())
    val exec = MoreExecutors.newDirectExecutorService()
    val dispatcher = exec.asCoroutineDispatcher()

    object : GuestExecutor, ListeningExecutorService by exec, CoroutineContext by dispatcher {
      override val dispatcher: CoroutineDispatcher get() = dispatcher

      override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> =
        followup.schedule(command, delay, unit)

      override fun <V : Any?> schedule(callable: Callable<V>, delay: Long, unit: TimeUnit): ScheduledFuture<V> =
        followup.schedule(callable, delay, unit)

      override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
      ): ScheduledFuture<*> = followup.scheduleAtFixedRate(command, initialDelay, period, unit)

      override fun scheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit
      ): ScheduledFuture<*> = followup.scheduleWithFixedDelay(command, initialDelay, delay, unit)
    }
  }

  // Work-stealing executor instance with context-aware scheduling.
  private val parallelExecutor: GuestExecutor by lazy {
    val scheduled = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())
    val exec = MoreExecutors.listeningDecorator(scheduled)
    val dispatcher = exec.asCoroutineDispatcher()

    object : GuestExecutor, ListeningExecutorService by exec, CoroutineContext by dispatcher {
      override val dispatcher: CoroutineDispatcher get() = dispatcher

      override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> =
        scheduled.schedule(command, delay, unit)

      override fun <V : Any?> schedule(callable: Callable<V>, delay: Long, unit: TimeUnit): ScheduledFuture<V> =
        scheduled.schedule(callable, delay, unit)

      override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
      ): ScheduledFuture<*> = scheduled.scheduleAtFixedRate(command, initialDelay, period, unit)

      override fun scheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit
      ): ScheduledFuture<*> = scheduled.scheduleWithFixedDelay(command, initialDelay, delay, unit)
    }
  }

  /** @return Direct executor implementation; meant for use in testing or single-threaded modes only. */
  public fun direct(): GuestExecutor = directExecutor

  /** @return Work-stealing executor implementation, with parallelism set to the active number of CPUs. */
  public fun workStealing(): GuestExecutor = parallelExecutor
}
