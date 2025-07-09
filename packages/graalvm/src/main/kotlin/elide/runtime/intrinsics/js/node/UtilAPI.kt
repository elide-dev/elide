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
package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.annotations.API
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.AbortController
import elide.runtime.intrinsics.js.AbortSignal
import elide.runtime.intrinsics.js.node.util.DebugLogger
import elide.runtime.intrinsics.js.node.util.InspectOptionsAPI
import elide.runtime.node.util.InspectOptions
import elide.vm.annotations.Polyglot

/**
 * # Node API: Utilities
 *
 * Provides an API surface definition for Node.js' built-in `util` module, which provides various utility functions used
 * throughout Node.js applications and libraries.
 *
 * For example, [callbackify] and [promisify] convert between callback-style functions and promise-based functions,
 * assuming the calling style is consistent with the error-first convention (i.e. `(err, value) => {}`).
 *
 * To import this module within JavaScript:
 * ```js
 * import util from "node:util";
 * // or
 * const util = require("node:util");
 * ```
 */
@API public interface UtilAPI : NodeAPI {
  /**
   * ### Callbackify
   *
   * Given an async function which produces a promise, this method returns a function that behaves like a
   * "callback-style" function, which takes a callback in the form `(err, result) => {}`.
   *
   * Example usage:
   * ```js
   * import { callbackify } from "node:util";
   * const sample = async () => 42;
   * const non_async = callbackify(sample);
   * non_async((err, result) => {
   *   // result is `42`
   * })
   * ```
   *
   * From the [Node.js documentation](https://nodejs.org/docs/latest/api/util.html#utilcallbackifyoriginal):
   *
   * "Takes an async function (or a function that returns a Promise) and returns a function following the error-first
   * callback style, i.e. taking an (err, value) => ... callback as the last argument. In the callback, the first
   * argument will be the rejection reason (or null if the Promise resolved), and the second argument will be the
   * resolved value."
   *
   * @see promisify `promisify` performs the inverse operation
   * @param asyncFn Async function to convert to a callback-style function.
   * @return Callback-style function which calls through to the provided async function.
   */
  @Polyglot public fun callbackify(asyncFn: Value?): ProxyExecutable

  /**
   * ### Promisify
   *
   * Accepts a function that follows the error-first callback style as the last argument, and returns a function which
   * converts it to behave with promises, where the returned promise resolves with the value returned in the callback,
   * or rejects with the error passed to the callback.
   *
   * Example usage:
   * ```js
   * import { promisify } from "node:util";
   * const sample = (cb) => cb(null, 42);
   * const asyncFn = promisify(sample);
   * asyncFn().then(result => {
   *   // result is `42`
   * });
   * ```
   *
   * From the [Node.js documentation](https://nodejs.org/docs/latest/api/util.html#utilpromisifyoriginal):
   *
   * "Takes a function following the common error-first callback style, i.e. taking an `(err, value) => ...` callback as
   * the last argument, and returns a version that returns promises."
   *
   * @see callbackify `callbackify` performs the inverse operation
   * @param fn Function to convert to a promise-based function.
   * @return Function that returns a promise.
   */
  @Polyglot public fun promisify(fn: Value?): ProxyExecutable

  /**
   * ### Debug Logger
   *
   * Creates a named debug logger (at [name]) which is active only when the following conditions are met:
   * - The `--js:debug` flag is provided on the command line
   * - The logger's name is specified in, or mentioned in, the `NODE_DEBUG` environment variable (comma-separated)
   *
   * This method variant is designed for implementation and host-side dispatch.
   *
   * @param name Name of the debug logger to create.
   * @return A debug logger instance which can be used to log debug messages.
   */
  public fun debuglog(name: String): DebugLogger

  /**
   * ### Debug Logger
   *
   * Creates a named debug logger (at [name]) which is active only when the following conditions are met:
   * - The `--js:debug` flag is provided on the command line
   * - The logger's name is specified in, or mentioned in, the `NODE_DEBUG` environment variable (comma-separated)
   *
   * This method variant is designed for guest-side dispatch.
   *
   * @param name Name of the debug logger to create.
   * @return A debug logger instance which can be used to log debug messages.
   */
  @Polyglot public fun debuglog(name: Value?): DebugLogger = debuglog(
    name?.takeIf { it.isString }?.asString() ?: throw JsError.typeError("Creating a logger requires a string name")
  )

  /**
   * ### Deprecate a Function
   *
   * Accepts a callable function, and, optionally, a message and code to include in the deprecation warning; then, when
   * the function is called, the first time it is called (only), the deprecation warning is emitted.
   *
   * This variant is designed for implementation and host-side dispatch.
   *
   * @param fn Function to deprecate.
   * @param message Optional message to include in the deprecation warning.
   * @param code Optional code to include in the deprecation warning.
   */
  public fun deprecate(fn: Value, message: String?, code: String?): ProxyExecutable

  /**
   * ### Deprecate a Function
   *
   * Accepts a callable function, and, optionally, a message and code to include in the deprecation warning; then, when
   * the function is called, the first time it is called (only), the deprecation warning is emitted.
   *
   * This variant is designed for guest-side dispatch.
   *
   * @param fn Function to deprecate.
   * @param message Optional message to include in the deprecation warning.
   * @param code Optional code to include in the deprecation warning.
   */
  @Suppress("SpreadOperator")
  @Polyglot public fun deprecate(fn: Value, message: Value? = null, code: Value? = null): ProxyExecutable {
    if (fn.isNull || !fn.canExecute()) {
      throw JsError.typeError("Cannot deprecate a non-callable value: $fn")
    }
    return deprecate(
      fn,
      message?.takeIf { it.isString }?.asString(),
      code?.takeIf { it.isString }?.asString(),
    )
  }

  /**
   * ### Transferable Abort Controller
   *
   * Creates a new [AbortController] which is marked for transferability, meaning it can safely be transferred across
   * threading contexts and used via `postMessage` or similar mechanisms.
   *
   * @return A transferable [AbortController] instance that can be used across contexts.
   */
  @Polyglot public fun transferableAbortController(): AbortController

  /**
   * ### Transferable Abort Signal
   *
   * Marks an [AbortSignal] as transferable, meaning it can be transferred across threading contexts and used via
   * `postMessage` or similar mechanisms.
   *
   * @param signal [AbortSignal] to mark as transferable.
   * @return The marked abort signal.
   */
  @Polyglot public fun transferableAbortSignal(signal: AbortSignal): AbortSignal

  /**
   * ### Is Array
   *
   * Indicate whether the provided [value] behaves like an array, or is an array-like object.
   *
   * @param value Value to check for array-like behavior.
   * @return `true` if the value is an array or array-like, `false` otherwise.
   */
  @Polyglot public fun isArray(value: Value?): Boolean

  /**
   * ### Get System Error Name
   *
   * Given a system error [id] (also known as an error "code"), returns the name of the system error, as known to the
   * Node.js API.
   *
   * This method variant is designed for implementation and host-side dispatch.
   *
   * @param id Numeric ID of the system error to look up.
   * @return The name of the system error, or `null` if the ID is not recognized.
   */
  @Polyglot public fun getSystemErrorName(id: Int): String?

  /**
   * ### Get System Error Name
   *
   * Given a system error [id] (also known as an error "code"), returns the name of the system error, as known to the
   * Node.js API.
   *
   * This method variant is designed for guest-side dispatch.
   *
   * @param id Numeric ID of the system error to look up.
   * @return The name of the system error, or `null` if the ID is not recognized.
   */
  @Polyglot public fun getSystemErrorName(id: Value): String? {
    if (!id.isNumber || !id.fitsInInt()) {
      throw JsError.typeError("System error ID must be a number: $id")
    }
    return getSystemErrorName(id.asInt())
  }

  /**
   * ### Get System Error Map
   *
   * Retrieve the mapping of known system error IDs to their names and messages, as understood by the Node.js API; the
   * structure of the returned map is as follows:
   *
   * - Key: System error ID (an `Int`).
   * - Value: A list of strings, where the first string is the name of the system error, and the second string is the
   *   message associated with the system error.
   *
   * This method variant is designed for implementation and dispatch from both host and guest contexts.
   *
   * @return A map of system error IDs to their names and messages.
   */
  @Polyglot public fun getSystemErrorMap(): Map<Int, List<String>>

  /**
   * ### Inspect
   *
   * Inspects the provided [obj] and returns a string representation of the object, formatted according to the provided
   * suite of [options], as applicable.
   *
   * @param obj Object to inspect.
   * @param options Effective inspection options to customize the output.
   * @return A string representation of the inspected object.
   */
  public fun inspect(obj: Any, options: InspectOptionsAPI): String

  /**
   * ### Inspect
   *
   * Inspects the provided [obj] and returns a string representation of the object, formatted according to the provided
   * suite of [options], as applicable.
   *
   * @param obj Object to inspect.
   * @param options Optional settings to apply to customize the output.
   * @return A string representation of the inspected object.
   */
  @Polyglot public fun inspect(obj: Any, options: Value? = null): String {
    return inspect(obj, options?.let { InspectOptions.from(it) } ?: InspectOptions.defaults())
  }

  /**
   * ### Format
   *
   * Formats the provided [format] string, and any additional [args] provided, into a string representation, optionally
   * applying any provided [options].
   *
   * This method variant is designed for implementation and host-side dispatch.
   *
   * @param format Format string to render.
   * @param options Options to apply to the formatting.
   * @param args Additional arguments to include in the formatted output.
   * @return A formatted string representation of the provided format and arguments.
   */
  public fun format(format: String, args: List<Any?>, options: InspectOptionsAPI = InspectOptions.defaults()): String

  /**
   * ### Format
   *
   * Formats the provided [format] string, and any additional [args] provided, into a string representation.
   *
   * This method variant is designed and guest-side dispatch.
   *
   * @param format Format string to render.
   * @param args Arguments to include in the formatted output.
   * @return A formatted string representation of the provided format and arguments.
   */
  @Polyglot public fun format(format: Value, vararg args: Value): String = format(
    format.takeIf { it.isString }?.asString() ?: throw JsError.typeError(
      "Format string must be a string: $format"
    ),
    args.toList(),
    InspectOptions.defaults(),
  )

  /**
   * ### Format
   *
   * Formats the provided `format` string, and any additional [args] provided, into a string representation; the first
   * argument is the format string.
   *
   * This method variant is designed and guest-side dispatch.
   *
   * @param options Options to apply to the formatting.
   * @param format Format string to render.
   * @param args Additional arguments to include in the formatted output.
   * @return A formatted string representation of the provided format and arguments.
   */
  @Polyglot public fun formatWithOptions(options: Value, format: Value, vararg args: Value): String = format(
    format.takeIf { it.isString }?.asString() ?: throw JsError.typeError(
      "Format string must be a string: $format"
    ),
    args.toList(),
    InspectOptions.from(options),
  )
}
