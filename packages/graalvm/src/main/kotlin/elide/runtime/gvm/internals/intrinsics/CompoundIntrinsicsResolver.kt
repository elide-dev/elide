package elide.runtime.gvm.internals.intrinsics

import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.internals.IntrinsicsResolver

/** Implementation of an intrinsics resolver which is backed by one or more foreign resolvers. */
internal class CompoundIntrinsicsResolver private constructor (
  private val resolvers: List<IntrinsicsResolver>
) : IntrinsicsResolver {
  companion object {
    /** @return Compound intrinsics resolver which is backed by the provided [list]. */
    @JvmStatic fun of(list: List<IntrinsicsResolver>): CompoundIntrinsicsResolver = CompoundIntrinsicsResolver(list)
  }

  /** @inheritDoc */
  override fun resolve(language: GuestLanguage): Set<GuestIntrinsic> = resolvers.flatMap {
    it.resolve(language)
  }.toSet()
}
