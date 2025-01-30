/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.gvm.intrinsics

import java.util.HashMap
import java.util.LinkedList
import elide.runtime.gvm.GuestLanguage
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.IntrinsicCriteria
import elide.runtime.intrinsics.IntrinsicsResolver

/** Implementation of an intrinsics resolver which caches the output of the backing resolver. */
public class CachedIntrinsicsResolver private constructor (
  private val delegate: IntrinsicsResolver,
) : IntrinsicsResolver {
  public companion object {
    /** @return Compound intrinsics resolver which is backed by the provided [target]. */
    @JvmStatic public fun of(target: IntrinsicsResolver): CachedIntrinsicsResolver = CachedIntrinsicsResolver(target)
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
