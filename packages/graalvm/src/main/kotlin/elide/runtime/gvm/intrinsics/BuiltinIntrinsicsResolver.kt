@file:Suppress("MnInjectionPoints")

package elide.runtime.gvm.intrinsics

import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.IntrinsicsResolver
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Infrastructure
import jakarta.inject.Inject

/** Resolve intrinsics known at build-time via annotation processing. */
@Context @Infrastructure internal class BuiltinIntrinsicsResolver : IntrinsicsResolver {
  /** All candidate guest intrinsics. */
  @Inject lateinit var intrinsics: Collection<GuestIntrinsic>

  /** @inheritDoc */
  override fun resolve(language: GuestLanguage): Set<GuestIntrinsic> = intrinsics.filter {
    it.supports(language)
  }.toSet()
}
