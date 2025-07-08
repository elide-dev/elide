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
import elide.runtime.intrinsics.js.node.util.DebugLogger
import elide.vm.annotations.Polyglot

/**
 * ## Node API: Utilities
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
   * ## Debug Logger
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
   * ## Debug Logger
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
}
