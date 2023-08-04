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

package elide.tool.cli.state

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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
    private val singleton: AtomicReference<CommandState?> = AtomicReference(null)

    // Whether the singleton has been initialized.
    private val initialized = AtomicBoolean(false)

    /** @return Root command state. */
    @JvmStatic fun of(options: CommandOptions): CommandState = CommandState(CommandInfo(
      options = options,
    ))

    /** @return Statically-available command state. */
    @JvmStatic fun resolve(): CommandState? = singleton.get()

    /** @return Reset statically-available state. */
    @JvmStatic fun reset() {
      initialized.set(false)
      singleton.set(null)
    }

    /** Register a [CommandState] instance as the canonical global instance, so it may be resolved statically. */
    @Synchronized @JvmStatic private fun registerGlobally(target: CommandState) {
      require(!initialized.get()) {
        "Cannot initialize `CommandState` singleton twice"
      }
      require(singleton.compareAndSet(null, target)) {
        "Cannot register `CommandState` singleton twice"
      }
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
