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

@file:Suppress("DataClassPrivateConstructor")

package elide.embedded.api

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 */
@JvmRecord public data class InFlightCallInfo private constructor(
  val callId: InFlightCallID,
  private val open: AtomicBoolean = AtomicBoolean(false),
): Closeable, AutoCloseable {
  public companion object {
    /**
     *
     */
    @JvmStatic public fun of(callId: InFlightCallID, native: UnaryNativeCall? = null): InFlightCallInfo =
      InFlightCallInfo(callId)
  }

  override fun close() {
    open.set(false)
  }

  /**
   *
   */
  public fun isOpen(): Boolean = open.get()

  /**
   *
   */
  public fun <R> withLock(block: () -> R): R {
    return try {
      open.compareAndSet(false, true)
      block()
    } catch (thr: Throwable) {
      open.set(false)
      throw thr
    } finally {
      close()
    }
  }
}
