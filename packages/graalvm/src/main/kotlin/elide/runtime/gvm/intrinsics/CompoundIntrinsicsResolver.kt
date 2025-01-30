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

import elide.runtime.gvm.GuestLanguage
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.IntrinsicsResolver

/** Implementation of an intrinsics resolver which is backed by one or more foreign resolvers. */
public class CompoundIntrinsicsResolver private constructor (
  private val resolvers: List<IntrinsicsResolver>
) : IntrinsicsResolver {
  public companion object {
    /** @return Compound intrinsics resolver which is backed by the provided [list]. */
    @JvmStatic public fun of(list: List<IntrinsicsResolver>): CompoundIntrinsicsResolver =
      CompoundIntrinsicsResolver(list)
  }

  override fun generate(language: GuestLanguage, internals: Boolean): Sequence<GuestIntrinsic> =
    resolvers.asSequence().flatMap { it.resolve(language) }
}
