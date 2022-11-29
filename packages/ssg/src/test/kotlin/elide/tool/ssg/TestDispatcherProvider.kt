package elide.tool.ssg

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Provides a [CoroutineDispatcher] for use in test environments. */
@Requires(env = ["test"])
@Factory internal class TestDispatcherProvider {
  /** @return I/O dispatcher. */
  @Singleton fun acquireDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
