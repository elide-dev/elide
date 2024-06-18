/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.gvm.internals

import elide.annotations.Context
import elide.annotations.Singleton
import elide.runtime.gvm.internals.intrinsics.CompoundIntrinsicsResolver
import elide.runtime.intrinsics.IntrinsicCriteria
import elide.runtime.intrinsics.IntrinsicsResolver

/** Resolves intrinsics for use with guest VMs. */
@Suppress("MnInjectionPoints")
@Context @Singleton public class IntrinsicsManager (resolvers: List<IntrinsicsResolver>) {
  private val compound = CompoundIntrinsicsResolver.of(resolvers)
  private val filters: MutableList<IntrinsicCriteria> = mutableListOf()

  /** Resolver stub. */
  internal inner class GlobalResolver(private val criteria: IntrinsicCriteria): IntrinsicsResolver by compound {
    override fun criteria(allowInternal: Boolean): IntrinsicCriteria {
      return criteria
    }
  }

  // Global symbol resolver.
  private val globalResolver = GlobalResolver(IntrinsicCriteria.all(filters))

  /** @return Global resolver stub. */
  public fun resolver(): IntrinsicsResolver = globalResolver
}
