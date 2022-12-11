package elide.runtime.gvm.intrinsics

import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.IntrinsicsResolver
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Infrastructure
import java.util.ServiceLoader

/** Resolves installed guest intrinsics via the JVM service loader mechanism. */
@Context @Infrastructure internal class ServiceIntrinsicsResolver : IntrinsicsResolver {
  /** @inheritDoc */
  override fun resolve(language: GuestLanguage): Set<GuestIntrinsic> = ServiceLoader.load(
    GuestIntrinsic::class.java
  ).filter {
    it.supports(language)
  }.toSet()
}
