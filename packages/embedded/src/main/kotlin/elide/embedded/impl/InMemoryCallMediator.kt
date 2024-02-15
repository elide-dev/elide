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

package elide.embedded.impl

import kotlinx.atomicfu.atomic
import elide.embedded.api.InFlightCallMediator

/**
 *
 */
internal class InMemoryCallMediator : InFlightCallMediator {
  companion object {
    @JvmStatic private val singleton: InMemoryCallMediator =
      InMemoryCallMediator()

    /**
     *
     */
    @JvmStatic fun obtain(): InFlightCallMediator = singleton
  }

  // Private counter of all mediated calls.
  private val callCounter = atomic(0L)

  override fun allocateCallId(): Long {
    val allocated = callCounter.incrementAndGet()
    return allocated
  }
}
