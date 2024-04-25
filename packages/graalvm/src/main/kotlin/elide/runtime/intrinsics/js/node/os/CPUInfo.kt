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
package elide.runtime.intrinsics.js.node.os

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.internals.node.os.CPUInfoData
import elide.runtime.gvm.internals.node.os.CPUTimingsData
import elide.vm.annotations.Polyglot

/**
 * ## Operating System: CPU Timings
 */
public interface CPUTimes : ProxyObject {
  /**
   * The number of milliseconds the CPU has spent in user mode.
   */
  @get:Polyglot public val user: Long

  /**
   * The number of milliseconds the CPU has spent in nice mode.
   */
  @get:Polyglot public val nice: Long

  /**
   * The number of milliseconds the CPU has spent in sys mode.
   */
  @get:Polyglot public val sys: Long

  /**
   * The number of milliseconds the CPU has spent in idle mode.
   */
  @get:Polyglot public val idle: Long

  /**
   * The number of milliseconds the CPU has spent in irq mode.
   */
  @get:Polyglot public val irq: Long

  override fun getMemberKeys(): Array<String> = arrayOf(
    "user",
    "nice",
    "sys",
    "idle",
    "irq"
  )

  override fun getMember(key: String): Any = when (key) {
    "user" -> user
    "nice" -> nice
    "sys" -> sys
    "idle" -> idle
    "irq" -> irq
    else -> throw IllegalArgumentException("No such member: $key")
  }

  override fun hasMember(key: String): Boolean = key in memberKeys

  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Cannot modify `CPUTimes` record")
  }

  override fun removeMember(key: String?): Boolean {
    throw UnsupportedOperationException("Cannot modify `CPUTimes` record")
  }

  /** Factory to create new [CPUTimes] records. */
  public companion object {
    /**
     * Creates a new `CPUTimes` record.
     *
     * @param user The number of milliseconds the CPU has spent in user mode.
     * @param nice The number of milliseconds the CPU has spent in nice mode.
     * @param sys The number of milliseconds the CPU has spent in sys mode.
     * @param idle The number of milliseconds the CPU has spent in idle mode.
     * @param irq The number of milliseconds the CPU has spent in irq mode.
     * @return A new `CPUTimes` record.
     */
    @JvmStatic public fun of(user: Long, nice: Long, sys: Long, idle: Long, irq: Long): CPUTimes =
      CPUTimingsData(user, nice, sys, idle, irq)
  }
}

/**
 * ## Operating System: CPU Info
 */
public interface CPUInfo : ProxyObject {
  /**
   * The model name of the CPU.
   */
  @get:Polyglot public val model: String

  /**
   * The speed of the CPU in MHz.
   */
  @get:Polyglot public val speed: Long

  /**
   * Timing stats for this CPU.
   */
  @get:Polyglot public val times: CPUTimes

  override fun getMemberKeys(): Array<String> = arrayOf(
    "model",
    "speed",
    "times"
  )

  override fun getMember(key: String): Any = when (key) {
    "model" -> model
    "speed" -> speed
    "times" -> times
    else -> throw IllegalArgumentException("No such member: $key")
  }

  override fun hasMember(key: String): Boolean = key in memberKeys

  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Cannot modify `CPUInfo` record")
  }

  override fun removeMember(key: String?): Boolean {
    throw UnsupportedOperationException("Cannot modify `CPUInfo` record")
  }

  /** Factory to create new [CPUInfo] records. */
  public companion object {
    /**
     * Creates a new `CPUInfo` record.
     *
     * @param model The model name of the CPU.
     * @param speed The speed of the CPU in MHz.
     * @param times Timing stats for this CPU.
     * @return A new `CPUInfo` record.
     */
    @JvmStatic public fun of(model: String, speed: Long, times: CPUTimes): CPUInfo = CPUInfoData(model, speed, times)
  }
}
