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

package elide.runtime.core.internals

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycle
import elide.runtime.core.EngineLifecycleEvent

/**
 * An internal implementation of the [EngineLifecycle] interface that allows emitting events.
 * @see emit
 */
@DelicateElideApi internal class MutableEngineLifecycle : EngineLifecycle {
  /** A map associating event keys with a stack of listeners. */
  private val listeners = mutableMapOf<Any, MutableList<Any>>()

  override fun <T> on(event: EngineLifecycleEvent<T>, consume: (T) -> Unit) {
    listeners.compute(event) { _, present ->
      // add the consumer to the list if the entry exists, otherwise create a new list
      present?.apply { add(consume) } ?: mutableListOf(consume)
    }
  }

  @Suppress("unchecked_cast")
  fun <T> emit(event: EngineLifecycleEvent<T>, payload: T) {
    // trigger the event for each listener in this entry
    listeners[event]?.forEach { (it as (T) -> Unit).invoke(payload) }
  }
}
