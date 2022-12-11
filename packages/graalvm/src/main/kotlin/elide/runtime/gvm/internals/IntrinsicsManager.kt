package elide.runtime.gvm.internals

import elide.runtime.gvm.IntrinsicsResolver
import elide.runtime.gvm.intrinsics.BuiltinIntrinsicsResolver
import elide.runtime.gvm.intrinsics.CompoundIntrinsicsResolver
import elide.runtime.gvm.intrinsics.ServiceIntrinsicsResolver
import io.micronaut.context.annotation.Context

/** Resolves intrinsics for use with guest VMs. */
@Suppress("MnInjectionPoints")
@Context internal class IntrinsicsManager (resolvers: List<IntrinsicsResolver>) {
  private val compound = CompoundIntrinsicsResolver.of(resolvers)

  /** Resolver stub. */
  inner class GlobalResolver: IntrinsicsResolver by compound

  /** @return Global resolver stub. */
  internal fun resolver(): IntrinsicsResolver = GlobalResolver()
}
