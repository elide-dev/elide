package elide.tool.ssg

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Provides a [CoroutineDispatcher] based on the current environment. */
@Requires(notEnv = ["test"])
@Factory internal class DefaultDispatcherProvider {
  /** @return I/O dispatcher. */
  @Singleton fun acquireDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
