package elide.runtime.gvm.internals.intrinsics

import elide.annotations.Context
import elide.annotations.Singleton
import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.internals.IntrinsicsResolver
import io.micronaut.context.annotation.Infrastructure
import java.util.*

/** Resolves installed guest intrinsics via the JVM service loader mechanism. */
@Context @Singleton @Infrastructure internal class ServiceIntrinsicsResolver : IntrinsicsResolver {
  /** @inheritDoc */
  override fun resolve(language: GuestLanguage): Set<GuestIntrinsic> = ServiceLoader.load(
    GuestIntrinsic::class.java
  ).filter {
    it.supports(language)
  }.toSet()
}
