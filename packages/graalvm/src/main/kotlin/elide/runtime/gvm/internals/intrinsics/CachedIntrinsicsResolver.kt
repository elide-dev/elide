package elide.runtime.gvm.internals.intrinsics

import java.util.HashMap
import java.util.LinkedList
import elide.runtime.gvm.GuestLanguage
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.IntrinsicCriteria
import elide.runtime.intrinsics.IntrinsicsResolver

/** Implementation of an intrinsics resolver which caches the output of the backing resolver. */
internal class CachedIntrinsicsResolver private constructor (
  private val delegate: IntrinsicsResolver,
) : IntrinsicsResolver {
  companion object {
    /** @return Compound intrinsics resolver which is backed by the provided [target]. */
    @JvmStatic fun of(target: IntrinsicsResolver): CachedIntrinsicsResolver = CachedIntrinsicsResolver(target)
  }

  // Cache of resolved intrinsics.
  private val cache: MutableMap<GuestLanguage, LinkedList<GuestIntrinsic>> = HashMap()

  private fun resolveAll(language: GuestLanguage): Sequence<GuestIntrinsic> {
    val targetList = cache[language] ?: LinkedList<GuestIntrinsic>()
    if (targetList.isEmpty()) {
      targetList.addAll(delegate.generate(language, true).toList())
      cache[language] = targetList
    }
    val defaults = defaultCriteria(true)
    val overlay = criteria(true)
    val filter = IntrinsicCriteria {
      defaults.filter(it) && overlay.filter(it)
    }
    return targetList.asSequence().filter {
      filter.filter(it)
    }
  }

  override fun generate(language: GuestLanguage, internals: Boolean): Sequence<GuestIntrinsic> =
    resolveAll(language).filter { !it.isInternal || internals }
}
