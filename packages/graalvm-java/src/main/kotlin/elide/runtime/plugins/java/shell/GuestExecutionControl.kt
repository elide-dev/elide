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

package elide.runtime.plugins.java.shell

import jdk.jshell.spi.ExecutionControl
import jdk.jshell.spi.ExecutionControl.ClassBytecodes
import jdk.jshell.spi.ExecutionControl.NotImplementedException
import org.graalvm.polyglot.PolyglotException
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.plugins.jvm.interop.asStringOrNull

/**
 * An implementation of JShell's [ExecutionControl] interface that uses a guest value as [delegate], serving as an
 * espresso-powered backend for interactive Java sessions.
 *
 * The [delegate] itself should be an instance of [ExecutionControl] created in an espresso context. Currently, a
 * [LocalExecutionControl][jdk.jshell.execution.LocalExecutionControl] is used. Method calls will be delegated to the
 * guest value and their results will be translated before being returned to the caller if needed.
 *
 * All exceptions raised by the delegate in the guest context during evaluation will be correctly translated into
 * standard JShell exceptions if applicable, before being re-thrown.
 *
 * @param delegate A [PolyglotValue] wrapping a guest [ExecutionControl] instance.
 * @param provider A [GuestExecutionProvider] used to map guest exceptions and host bytecode.
 *
 * @see GuestExecutionProvider
 */
@DelicateElideApi internal class GuestExecutionControl(
  private val delegate: PolyglotValue,
  private val provider: GuestExecutionProvider,
) : ExecutionControl {
  /**
   * Attempt to invoke a [method] of the [delegate] value with the given [args], returning the result. If the guest
   * method raises a JShell-related exception, it will be mapped to a host exception before being re-thrown.
   */
  private fun tryInvoke(method: String, vararg args: Any?): PolyglotValue = try {
    // use the guest-side execution control
    delegate.invokeMember(method, *args)
  } catch (error: PolyglotException) {
    // map to a host exception and re-throw
    throw provider.mapException(error)
  }

  override fun close() {
    tryInvoke("close")
  }

  override fun load(cbcs: Array<out ClassBytecodes>) {
    tryInvoke("load", provider.mapBytecodes(cbcs))
  }

  override fun redefine(cbcs: Array<out ClassBytecodes>) {
    tryInvoke("redefine", provider.mapBytecodes(cbcs))
  }

  override fun invoke(className: String?, methodName: String?): String? {
    return tryInvoke("invoke", className, methodName).asStringOrNull()
  }

  override fun varValue(className: String?, varName: String?): String? {
    return tryInvoke("varValue", className, varName).asStringOrNull()
  }

  override fun addToClasspath(path: String?) {
    tryInvoke("addToClasspath", path)
  }

  override fun stop() {
    tryInvoke("stop")
  }

  override fun extensionCommand(command: String?, arg: Any?): Any {
    throw NotImplementedException("Extension commands are not supported: $command")
  }
}
