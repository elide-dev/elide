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
import elide.annotations.API
import elide.runtime.intrinsics.js.node.childProcess.ChildProcess
import elide.runtime.intrinsics.js.node.childProcess.ChildProcessSync
import elide.runtime.intrinsics.js.node.fs.StringOrBuffer
import elide.vm.annotations.Polyglot

/**
 * ## Node API: Child Process
 *
 * Describes the API made available for support of the `child_process` module from the Node API; this API can be used to
 * launch and control child processes from a given application.
 */
@API public interface ChildProcessAPI : NodeAPI {
  /**
   * ## Spawn
   *
   * Guest-side implementation of Node's [spawn] method, which is used to spawn a child process which is then returned
   * to the caller and used in an event-driven manner.
   *
   * @param command Command to execute.
   * @param args Arguments to specify for the command.
   * @param options Options for the process execution, or `null` (`undefined` in JavaScript).
   * @return Child process handle which was spawned using the provided [command], [args], and [options] inputs.
   */
  @Polyglot public fun spawn(command: Value, args: Value?, options: Value?): ChildProcess

  /**
   * ## Exec
   *
   * Guest-side implementation of Node's [exec] method, which is used to execute commands via child processes using a
   * shell.
   *
   * @param command Command to execute.
   * @param options Options for the process execution, or `null` (`undefined` in JavaScript).
   * @param callback Callback to invoke when the process completes.
   * @return Child process handle which was spawned using the provided [command], [args], and [options] inputs.
   */
  @Polyglot public fun exec(command: Value, options: Value?, callback: Value?): ChildProcess

  /**
   * ## Exec File
   *
   * Guest-side implementation of Node's [execFile] method, which is used to execute commands via child processes using
   * a shell and a specific file.
   *
   * @param file Path or URL to the file to execute.
   * @param args Arguments to specify for the command.
   * @param options Options for the process execution, or `null` (`undefined` in JavaScript).
   * @param callback Callback to invoke when the process completes.
   * @return Child process handle which was spawned using the provided [file], [args], and [options] inputs.
   */
  @Polyglot public fun execFile(file: Value, args: Value?, options: Value?, callback: Value?): ChildProcess

  /**
   * ## Fork
   *
   * Specialized version of the [spawn] method which is designed to "fork" the currently running copy of Elide, with
   * a new [modulePath] as the entrypoint.
   *
   * @param modulePath Path to the module to execute.
   * @param args Arguments to specify for the command.
   * @param options Options for the process execution, or `null` (`undefined` in JavaScript).
   * @return Child process handle which was spawned using the provided [modulePath], [args], and [options] inputs.
   */
  @Polyglot public fun fork(modulePath: Value, args: Value?, options: Value?): ChildProcess

  /**
   * ## Exec (Synchronous)
   *
   * Guest-side implementation of Node's [execSync] method, which is used to synchronously spawn a child process,
   * potentially with options specified.
   *
   * @param command Command to execute.
   * @param options Options for the process execution, or `null` (`undefined` in JavaScript).
   * @return Output from the process, as a string or buffer, or `null` if the process output was redirected or
   *   unavailable for any other reason.
   */
  @Polyglot public fun execSync(command: Value, options: Value?): StringOrBuffer?

  /**
   * ## Exec (Synchronous)
   *
   * Guest-side implementation of Node's [execSync] method, which is used to synchronously spawn a child process,
   * potentially with options specified.
   *
   * @param file Path to the file to execute (a JavaScript `URL` or `string`).
   * @param args Arguments to specify for the command.
   * @param options Options for the process execution, or `null` (`undefined` in JavaScript).
   * @return Output from the process, as a string or buffer, or `null` if the process output was redirected or
   *   unavailable for any other reason.
   */
  @Polyglot public fun execFileSync(file: Value, args: Value?, options: Value?): StringOrBuffer?

  /**
   * ## Spawn (Synchronous)
   *
   * Guest-side implementation of Node's [spawnSync] method, which is used to synchronously spawn a child process by a
   * specified command name, argument set, and options (if applicable).
   *
   * @param command Command to execute.
   * @param args Arguments to specify for the command.
   * @param options Options for the process execution, or `null` (`undefined` in JavaScript).
   * @return [ChildProcessSync] instance representing the spawned child process.
   */
  @Polyglot public fun spawnSync(command: Value, args: Value?, options: Value?): ChildProcessSync
}
