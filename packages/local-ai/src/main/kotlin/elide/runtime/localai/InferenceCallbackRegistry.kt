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
package elide.runtime.localai

import java.util.concurrent.ConcurrentSkipListMap
import elide.runtime.localai.NativeLocalAi.InferenceChunkCallbackImpl

/**
 * ## Inference Callback Registry
 *
 * Maintains a static registry of live inference callbacks, indexed by their assigned operation ID. Callbacks flow in
 * statically from the inference layer, and are dispatched to their actual JVM callables here.
 */
public object InferenceCallbackRegistry {
  // Registered callbacks.
  private val callbacks: MutableMap<Int, InferenceChunkCallbackImpl> = ConcurrentSkipListMap()

  // Notify the registry that a cycle has completed.
  @JvmStatic internal fun notifyComplete(cycle: Int) {
    require(cycle in callbacks) { "No callback registered for cycle $cycle" }
    println("cycle $cycle complete")
    callbacks.remove(cycle)
  }

  // Register a callback for a given cycle.
  @JvmStatic internal fun register(cycle: Int, cbk: InferenceChunkCallbackImpl) {
    require(cycle !in callbacks) { "Callback already registered for cycle $cycle" }
    callbacks[cycle] = cbk
  }

  /**
   * Static entrypoint for chunks of inference results.
   *
   * When new results are seen for the first time, they are held statically until polled and flushed. After all chunks
   * are received (indicated by an empty chunk), the results are moved to a weak queue and callbacks are dispatched.
   *
   * @param cycle The cycle number of the chunk; groups all chunks for a given inference run
   * @param chunk The chunk of inference results
   */
  @JvmStatic @JvmName("onChunkReady") public fun onChunkReady(cycle: Int, chunk: String) {
    callbacks[cycle]?.onChunk(chunk)
  }
}
