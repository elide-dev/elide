@file:Suppress("MnInjectionPoints")

package elide.runtime.gvm.internals.intrinsics

import io.micronaut.context.annotation.Infrastructure
import elide.annotations.Context
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.GuestLanguage
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.IntrinsicsResolver

/** Resolve intrinsics known at build-time via annotation processing. */
@Context @Singleton @Infrastructure internal class BuiltinIntrinsicsResolver : IntrinsicsResolver {
  /** All candidate guest intrinsics. */
  @Inject lateinit var intrinsics: Collection<GuestIntrinsic>

  /** @inheritDoc */
  override fun resolve(language: GuestLanguage): Set<GuestIntrinsic> = intrinsics.filter {
    it.supports(language)
  }.toSet()
}
