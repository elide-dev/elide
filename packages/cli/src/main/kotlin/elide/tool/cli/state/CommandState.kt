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
@file:Suppress("unused")

package elide.tool.cli.state

import kotlinx.atomicfu.atomic

/**
 * # Command State
 *
 * Describes early command invocation state, before command execution context is created; command state may not vary
 * across implementations. State which is carried in the [CommandState.CommandInfo] record is parsed at the first
 * opportunity of all command executions.
 */
@JvmInline value class CommandState private constructor (
  private val commandInfo: CommandInfo
) {
  /**
   * ## Command Info
   *
   * Holds early invocation info, including the current coroutine scope and other useful utilities. This record is used
   * to spawn command context.
   *
   * @param options Top-level (global) command options.
   */
  @JvmRecord internal data class CommandInfo(
    val options: CommandOptions,
  )

  internal companion object {
    // Private singleton instance container.
    private val singleton = atomic<CommandState?>(null)

    // Whether the singleton has been initialized.
    private val initialized = atomic(false)

    /** @return Root command state. */
    @JvmStatic fun of(options: CommandOptions): CommandState = CommandState(CommandInfo(
      options = options,
    ))

    /** @return Statically-available command state. */
    @JvmStatic fun resolve(): CommandState? = singleton.value

    /** @return Reset statically-available state. */
    @JvmStatic fun reset() {
      initialized.value = false
      singleton.value = null
    }

    /** Register a [CommandState] instance as the canonical global instance, so it may be resolved statically. */
    @Synchronized @JvmStatic private fun registerGlobally(target: CommandState) {
      require(!initialized.value) {
        "Cannot initialize `CommandState` singleton twice"
      }
      require(singleton.value == null) {
        "Cannot register `CommandState` singleton twice"
      }
      singleton.value = target
      initialized.compareAndSet(
        false,
        true,
      )
    }
  }

  /**
   * Register this instance as the canonical global instance, so it may be resolved statically.
   */
  internal fun register(): CommandState = apply {
    registerGlobally(this)
  }
}
