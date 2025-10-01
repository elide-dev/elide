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
package elide.runtime.core

import java.util.*
import java.util.concurrent.Phaser
import elide.annotations.Singleton

@Singleton public class RuntimeLatch {
  private val latch = Phaser()
  private val awaitListeners = Collections.synchronizedList(mutableListOf<() -> Unit>())

  public fun onAwait(block: () -> Unit) {
    awaitListeners.add(block)
  }

  public fun retain() {
    latch.register()
  }

  public fun release() {
    latch.arrive()
  }

  public fun await() {
    awaitListeners.forEach { it.invoke() }

    if (latch.unarrivedParties == 0) return
    latch.awaitAdvance(latch.phase)
  }
}
