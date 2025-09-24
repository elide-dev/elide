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
import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.process.ProcessEnvironmentAPI
import elide.runtime.intrinsics.js.node.process.ProcessStandardInputStream
import elide.runtime.intrinsics.js.node.process.ProcessStandardOutputStream
import elide.vm.annotations.Polyglot

// Properties available on the Node process object and module.
private val NODE_PROCESS_PROPS = arrayOf(
  "env",
  "argv",
  "cwd",
  "pid",
  "platform",
  "arch",
  "stdout",
  "stderr",
  "stdin",
  "title",
)

/**
 * # Node API: Process
 *
 * Describes the "process" object, as specified by the Node JS "process" built-in module. The "process" object provides
 * information about the current process, and allows the user to interact with the process environment. This includes
 * such information as the process' environment variable access, current working directory, process ID, and so on.
 *
 * &nbsp;
 *
 * ## Summary
 *
 * The `process` module is special in that it is both a module and a globally-available VM intrinsic; running under pure
 * Node, one does not need to import or require anything at all and can just specify `process.x`. Additionally, the
 * `process` module is made available as a standard built-in module. Elide supports both use styles.
 *
 * &nbsp;
 *
 * ## Behavior
 *
 * Depending on the access rights in the current environment, the `process` object behaves roughly the same in Elide as
 * it does in Node. Where values are forbidden for access or otherwise unavailable, sensible values are provided instead
 * of their real counterparts (for example, `-1` as the process ID).
 *
 * Behavior and requisite access rights are described below for each property, along with any other applicable behavior:
 *
 * - `process.env`: The environment variables of the current process. This is a read-only property. The values provided
 *   as "process environment" may be the real host environment or stubbed values, or both, and may include values from
 *   `.env` files loaded on behalf of the user. By default, host environment access is not available to guests in Elide.
 *
 * - `process.argv`: The command-line arguments of the current process. This is a read-only property. The values
 *   provided here are available when a JavaScript file is used as the program entrypoint, and under normal operating
 *   circumstances, even if the entrypoint is implemented in another language. Thus, the first argument to `argv` is
 *   always the Elide binary. In some circumstances, for example, render-only scripts, no arguments are available.
 *
 * - `process.exit(code)`: Exit the current process with the specified exit code. This method is available to all guests
 *   and will, by default, exit the entire VM, including the host context; if VM exit is disabled or forbidden, this
 *   method halts guest execution and returns control to the host.
 *
 * - `process.nextTick(callback, ...args)`: Schedule a callback to be invoked in the next tick of the event loop. This
 *   method is not implemented in Elide at the time of this writing.
 *
 * &nbsp;
 *
 * See the [Node Process API](https://nodejs.org/api/process.html) for more information.
 * @see ProcessEnvironmentAPI for details about how Elide handles Node-style environment access.
 */
@API public interface ProcessAPI : NodeAPI, ProxyObject {
  /**
   * ## Process Environment
   *
   * Access the environment variables of the current process as an arbitrary guest value.
   *
   * See also: [Node Process API: `env`](https://nodejs.org/api/process.html#process_process_env).
   */
  @get:Polyglot public val env: PolyglotValue

  /**
   * ## Process Arguments
   *
   * Access the command-line arguments of the current process.
   *
   * See also: [Node Process API: `argv`](https://nodejs.org/api/process.html#process_process_argv).
   */
  @get:Polyglot public val argv: Array<String>

  /**
   * ## Process ID
   *
   * Access the process ID of the current process.
   *
   * See also: [Node Process API: `pid`](https://nodejs.org/api/process.html#process_process_pid).
   */
  @get:Polyglot public val pid: Long

  /**
   * ## Platform
   *
   * Access the current operating platform; this describes the operating system in use.
   *
   * See also: [Node Process API: `platform`](https://nodejs.org/api/process.html#process_process_platform).
   */
  @get:Polyglot public val platform: String

  /**
   * ## Architecture
   *
   * Access the current architecture; this describes the CPU type in use.
   *
   * See also: [Node Process API: `arch`](https://nodejs.org/api/process.html#process_process_arch).
   */
  @get:Polyglot public val arch: String

  /**
   * ## Streams: Standard Output
   *
   * Access to a stream for emitting content to `stdout`.
   *
   * See also: [Node Process API: `stdout`](https://nodejs.org/api/process.html#process_process_stdout).
   * @see ProcessStandardOutputStream for the layout of a standard output stream.
   */
  @get:Polyglot public val stdout: ProcessStandardOutputStream

  /**
   * ## Streams: Standard Error
   *
   * Access to a stream for emitting content to `stderr`.
   *
   * See also: [Node Process API: `stderr`](https://nodejs.org/api/process.html#process_process_stderr).
   * @see ProcessStandardOutputStream for the layout of a standard output stream.
   */
  @get:Polyglot public val stderr: ProcessStandardOutputStream

  /**
   * ## Streams: Standard Input
   *
   * Access to a stream for consuming content from `stdin`.
   *
   * See also: [Node Process API: `stdin`](https://nodejs.org/api/process.html#process_process_stdin).
   * @see ProcessStandardInputStream for the layout of a standard input stream
   */
  @get:Polyglot public val stdin: ProcessStandardInputStream

  /**
   * ## Process Title
   *
   * Access to retrieve the current process title.
   *
   * See also: [Node Process API: `title`](https://nodejs.org/api/process.html#process_process_title).
   */
  @get:Polyglot @set:Polyglot public var title: String?

  /**
   * ## Current Working Directory
   *
   * Access the current working directory of the current process.
   *
   * See also: [Node Process API: `cwd`](https://nodejs.org/api/process.html#process_process_cwd).
   */
  @Polyglot public fun cwd(): String

  /**
   * ## Process Exit
   *
   * Exit the current process with the specified exit code.
   * This variant works on a host exit code [Int].
   *
   * See also: [Node Process API: `exit`](https://nodejs.org/api/process.html#process_process_exit_code).
   *
   * @param code Exit code to use
   */
  @Polyglot public fun exit(code: Int?)

  /**
   * ## Process Exit
   *
   * Exit the current process with the specified exit code.
   * This variant works on a guest [Value].
   *
   * See also: [Node Process API: `exit`](https://nodejs.org/api/process.html#process_process_exit_code).
   *
   * @param code Exit code to use
   */
  @Polyglot public fun exit(code: Value?)

  /**
   * ## Process Exit
   *
   * Exit the current process with the specified exit code; uses code `0`.
   *
   * See also: [Node Process API: `exit`](https://nodejs.org/api/process.html#process_process_exit_code).
   */
  @Polyglot public fun exit()

  /**
   * ## Process Next Tick
   *
   * Schedule a callback to be invoked in the next tick of the event loop.
   *
   * See: [Node Process API: `nextTick`](https://nodejs.org/api/process.html#process_process_nexttick_callback_args).
   *
   * @param callback Callback to invoke
   * @param args Arguments to pass to the callback
   */
  @Polyglot public fun nextTick(callback: (args: Array<Any>) -> Unit, vararg args: Any)

  override fun hasMember(key: String): Boolean = key in NODE_PROCESS_PROPS
  override fun getMemberKeys(): Array<String> = NODE_PROCESS_PROPS

  override fun putMember(key: String, value: Value?) {
    if (key == "title") {
      if (value == null || !value.isString)
        throw JsError.valueError("Cannot set process title to non-string value")
      title = value.asString()
      return
    }

    // ignore
  }

  override fun removeMember(key: String?): Boolean {
    return false
  }
}
