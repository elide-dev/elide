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
package elide.runtime.intrinsics.js.node.util

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.runtime.gvm.js.JsError
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.node.util.DebugLoggerImpl
import elide.vm.annotations.Polyglot

// Constants for debug logger properties and methods.
private const val P_LOGGER_NAME = "loggerName"
private const val P_LOGGER_ENABLED = "enabled"

// All properties and methods on logger instances.
private val loggerMethodsAndProps = arrayOf(
  P_LOGGER_NAME,
  P_LOGGER_ENABLED,
)

/**
 * ## Debug Logger
 *
 * Instance which is produced by the method `util.debuglog`, and which can be called from a guest or host context to
 * emit a debug log to a named logger; if, at runtime, the `NODE_DEBUG` environment variable is set to a value which
 * specifies or includes the logger's name, then debug logs will be emitted to the console.
 *
 * @property loggerName Host and guest-side property providing the name of the logger instance.
 */
public interface DebugLogger : ProxyExecutable, ReadOnlyProxyObject {
  public companion object {
    /**
     * JavaScript debug logging logger name.
     */
    public const val JS_DEBUG_LOGGER_NAME: String = "js.debug"

    /**
     * Shorthand to create a named debug logger instance.
     *
     * Note: The first call to this method in the program's lifecycle will parse the value of the `NODE_DEBUG`
     * environment variable, if set.
     *
     * @param named Name of the logger to create.
     * @return A new debug logger instance with the given name.
     */
    @JvmStatic public fun named(named: String): DebugLogger = DebugLoggerImpl.Factory.createLogger(named)
  }

  override fun getMemberKeys(): Array<String> = loggerMethodsAndProps

  override fun getMember(key: String?): Any? = when (key) {
    P_LOGGER_NAME -> loggerName
    P_LOGGER_ENABLED -> enabled
    else -> null
  }

  /**
   * Name of the logger instance.
   */
  @get:Polyglot public val loggerName: String

  /**
   * Whether the logger is enabled.
   */
  @get:Polyglot public val enabled: Boolean

  @Suppress("SpreadOperator")
  override fun execute(vararg arguments: Value?): Any? = invoke(*arguments)

  /**
   * Invoke this debug logger with the provided arguments, which are passed to a rendering function that stringifies
   * them, and then emits them as a debug line to [emit].
   *
   * @param arguments Arguments to render in the debug log line.
   * @return Always returns `null`.
   */
  public operator fun invoke(vararg arguments: Any?): Any?

  /**
   * Emit a rendered debug log to the console, or to other debug logging facilities, as applicable, based on active
   * runtime configuration.
   *
   * @param builder String builder which contains the rendered debug log to emit.
   */
  public fun emit(builder: StringBuilder)

  /**
   * ## Debug Logger: Factory
   *
   * Models the factory function which creates [DebugLogger] instances; the function takes a single required argument,
   * which is the name of the logger to create.
   */
  public fun interface Factory : ProxyInstantiable {
    /**
     * Create a new debug logger instance with the given name.
     *
     * @param name Name of the logger to create.
     * @return A new debug logger instance.
     */
    public fun createLogger(name: String): DebugLogger

    override fun newInstance(vararg arguments: Value?): DebugLogger {
      return createLogger((arguments.getOrNull(0)?.takeIf { it.isString } ?: throw JsError.typeError(
        "Creating a debug logger requires a string name argument",
      )).asString())
    }
  }
}
