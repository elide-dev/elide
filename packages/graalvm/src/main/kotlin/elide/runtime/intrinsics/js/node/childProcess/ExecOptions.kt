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
package elide.runtime.intrinsics.js.node.childProcess

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.intrinsics.js.URL
import elide.runtime.intrinsics.js.node.ChildProcessAPI
import elide.runtime.intrinsics.js.node.childProcess.ExecDefaults.ENCODING
import elide.runtime.intrinsics.js.node.childProcess.ExecDefaults.KILL_SIGNAL
import elide.runtime.intrinsics.js.node.childProcess.ExecDefaults.MAX_BUFFER_DEFAULT
import elide.runtime.intrinsics.js.node.childProcess.ExecDefaults.WINDOWS_HIDE

// Default values used in `exec`.
private object ExecDefaults {
  // Default encoding token to use.
  const val ENCODING: String = ChildProcessDefaults.ENCODING

  // Default signal to send for killing a process.
  const val KILL_SIGNAL: String = ChildProcessDefaults.SIGNAL_SIGKILL

  // Default maximum size for buffered process I/O.
  const val MAX_BUFFER_DEFAULT = ChildProcessDefaults.MAX_BUFFER_DEFAULT

  // Whether to hide the spawned process on Windows platforms. Inert on other platforms.
  const val WINDOWS_HIDE = ChildProcessDefaults.WINDOWS_HIDE
}

/**
 * ## Exec Options
 *
 * Defines options for the [ChildProcessAPI.exec] method, which is used to asynchronously spawn a child process; these
 * options extend [ProcOptions].
 *
 * Build exec options from a foreign [Value] via the [ExecOptions.from] method.
 *
 * @property cwdString Current working directory, as a string.
 * @property cwdUrl Current working directory, as a URL.
 * @property stdio Standard I/O configuration.
 * @property env Environment variables.
 * @property shell Shell to invoke for the process.
 * @property uid User ID to run the process as.
 * @property gid Group ID to run the process as.
 * @property timeoutSeconds Timeout for the process, in seconds.
 * @property killSignal Signal to use to kill the process.
 * @property maxBuffer Maximum buffer size for I/O.
 * @property encoding Encoding to use for I/O.
 * @property windowsHide Whether to hide the process window.
 * @property timeout Timeout for the process, in seconds.
 */
@API @JvmRecord public data class ExecOptions private constructor (
  override val cwdString: String? = null,
  public val cwdUrl: URL? = null,
  override val env: Map<String, String>? = null,
  override val shell: String? = null,
  public val uid: Int? = null,
  public val gid: Int? = null,
  public val timeoutSeconds: Int? = null,
  public val killSignal: String? = KILL_SIGNAL,
  public val maxBuffer: Int = MAX_BUFFER_DEFAULT,
  override val encoding: String? = ENCODING,
  public val windowsHide: Boolean = WINDOWS_HIDE,
): ProcOptions {
  override val timeout: Int? get() = timeoutSeconds
  override val stdio: StdioConfig get() = StdioConfig.DEFAULTS

  /** Factories and other helpers for [ExecOptions]. */
  public companion object {
    /** Default values for [ExecOptions]. */
    public val DEFAULTS: ExecOptions = ExecOptions()

    /** @return [ExecOptions] created by hand. */
    @Suppress("LongParameterList")
    @JvmStatic public fun of(
      cwdString: String? = null,
      cwdUrl: URL? = null,
      env: Map<String, String>? = null,
      shell: String? = null,
      uid: Int? = null,
      gid: Int? = null,
      timeoutSeconds: Int? = null,
      killSignal: String? = KILL_SIGNAL,
      maxBuffer: Int = MAX_BUFFER_DEFAULT,
      encoding: String = ENCODING,
      windowsHide: Boolean = WINDOWS_HIDE,
    ): ExecOptions = ExecOptions(
      cwdString = cwdString,
      cwdUrl = cwdUrl,
      env = env,
      shell = shell,
      uid = uid,
      gid = gid,
      timeoutSeconds = timeoutSeconds,
      killSignal = killSignal,
      maxBuffer = maxBuffer,
      encoding = encoding,
      windowsHide = windowsHide,
    )

    /** @return [ExecOptions] derived from a guest [Value]. */
    @JvmStatic public fun from(other: Value?): ExecOptions = when (other) {
      null -> DEFAULTS
      else -> when {
        other.isNull -> DEFAULTS
        other.isHostObject -> other.asHostObject()
        other.hasMembers() -> ExecOptions(
          cwdString = other.getMember("cwdString")?.takeIf { it.isString }?.asString(),
          cwdUrl = other.getMember("cwdUrl")?.takeIf { it.isHostObject }?.asHostObject(),
          env = other.getMember("env")?.takeIf { it.isHostObject }?.asHostObject(),
          shell = other.getMember("shell")?.takeIf { it.isString }?.asString(),
          uid = other.getMember("uid")?.takeIf { it.isNumber }?.asInt(),
          gid = other.getMember("gid")?.takeIf { it.isNumber }?.asInt(),
          timeoutSeconds = other.getMember("timeoutSeconds")?.takeIf { it.isNumber }?.asInt(),
          killSignal = other.getMember("killSignal")?.takeIf { it.isString }?.asString(),
          maxBuffer = other.getMember("maxBuffer")?.takeIf { it.isNumber }?.asInt() ?: MAX_BUFFER_DEFAULT,
          encoding = other.getMember("encoding")?.takeIf { it.isString }?.asString() ?: "buffer",
          windowsHide = other.getMember("windowsHide")?.takeIf { it.isBoolean }?.asBoolean() ?: false,
        )

        else -> throw JsError.typeError("Invalid type provided as `exec` options")
      }
    }
  }
}
