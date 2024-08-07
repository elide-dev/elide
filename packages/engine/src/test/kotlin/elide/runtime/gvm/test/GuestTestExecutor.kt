package elide.runtime.gvm.test

import io.micronaut.context.annotation.Requires
import elide.annotations.Eager
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutor
import elide.runtime.exec.GuestExecutorProvider

/**
 * Guest Test Executor Factory
 *
 * Initializes a direct executor for use during test execution.
 */
@Requires(env = ["test"])
@Eager @Factory internal class GuestTestExecutorFactory : GuestExecutorProvider {
  @Singleton override fun executor(): GuestExecutor = GuestExecution.direct()
}
