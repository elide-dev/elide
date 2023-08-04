/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.intrinsics

import elide.runtime.gvm.GuestLanguage
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.IntrinsicsResolver

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
