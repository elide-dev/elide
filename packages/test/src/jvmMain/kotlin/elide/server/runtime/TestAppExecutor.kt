package elide.server.runtime

import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Singleton
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory

/** Provides an implementation of [AppExecutor] that directly executes all tasks in the current thread. */
@Suppress("UnstableApiUsage")
@Replaces(AppExecutor.DefaultExecutor::class)
@Singleton class TestAppExecutor: AppExecutor {
  override fun service(): ListeningScheduledExecutorService {
    return MoreExecutors.listeningDecorator(
      MoreExecutors.getExitingScheduledExecutorService(
        ScheduledThreadPoolExecutor(1, ThreadFactory {
          Thread.currentThread()
        })
      )
    )
  }
}
