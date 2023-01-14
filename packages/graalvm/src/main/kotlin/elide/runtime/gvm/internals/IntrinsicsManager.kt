package elide.runtime.gvm.internals

import elide.annotations.Context
import elide.annotations.Singleton
import elide.runtime.gvm.internals.intrinsics.CompoundIntrinsicsResolver

/** Resolves intrinsics for use with guest VMs. */
@Suppress("MnInjectionPoints")
@Context @Singleton internal class IntrinsicsManager (resolvers: List<IntrinsicsResolver>) {
  private val compound = CompoundIntrinsicsResolver.of(resolvers)

  /** Resolver stub. */
  inner class GlobalResolver: IntrinsicsResolver by compound

  /** @return Global resolver stub. */
  internal fun resolver(): IntrinsicsResolver = GlobalResolver()
}
