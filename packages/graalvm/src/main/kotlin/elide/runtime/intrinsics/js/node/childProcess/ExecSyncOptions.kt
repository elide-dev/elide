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
package elide.runtime.intrinsics.js.node.childProcess

import org.graalvm.polyglot.Value
import kotlin.time.Duration
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration
import elide.annotations.API
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.URL
import elide.runtime.intrinsics.js.node.ChildProcessAPI
import elide.runtime.intrinsics.js.node.childProcess.ChildProcessDefaults.decodeEnvMap
import elide.runtime.intrinsics.js.node.childProcess.ExecSyncDefaults.ENCODING
import elide.runtime.intrinsics.js.node.childProcess.ExecSyncDefaults.KILL_SIGNAL
import elide.runtime.intrinsics.js.node.childProcess.ExecSyncDefaults.MAX_BUFFER_DEFAULT
import elide.runtime.intrinsics.js.node.childProcess.ExecSyncDefaults.WINDOWS_HIDE

// Default values used in `execSync`.
internal data object ExecSyncDefaults {
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
 * ## Exec-Sync Options
 *
 * Defines options for the [ChildProcessAPI.execSync] method, which is used to synchronously spawn a child process;
 * these options extend [ProcOptions].
 *
 * Build exec sync options from a foreign [Value] via the [ExecSyncOptions.from] method.
 *
 * @property cwdString Current working directory, as a string.
 * @property cwdUrl Current working directory, as a URL.
 * @property input Input to the process.
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
@ConsistentCopyVisibility
@API @JvmRecord public data class ExecSyncOptions internal constructor (
  override val cwdString: String? = null,
  public val cwdUrl: URL? = null,
  public val input: Any? = null,
  override val stdio: StdioConfig = StdioConfig.DEFAULTS,
  override val env: Map<String, String>? = null,
  override val shell: String? = null,
  override val uid: Int? = null,
  override val gid: Int? = null,
  override val timeout: Duration? = null,
  override val killSignal: String = KILL_SIGNAL,
  override val maxBuffer: Int = MAX_BUFFER_DEFAULT,
  override val encoding: String? = ENCODING,
  override val windowsHide: Boolean = WINDOWS_HIDE,
): IdentityProcOptions {
  override val timeoutSeconds: Long? get() = timeout?.inWholeSeconds

  /** @return Copy of these options with [value] applied as a timeout, in seconds. */
  public fun withTimeout(value: Duration): ExecSyncOptions = copy(timeout = value)

  /** Factories and other helpers for [ExecSyncOptions]. */
  public companion object {
    /** Default values for [ExecSyncOptions]. */
    public val DEFAULTS: ExecSyncOptions = ExecSyncOptions()

    /** @return [ExecSyncOptions] created by hand. */
    @Suppress("LongParameterList")
    @JvmStatic public fun of(
      cwdString: String? = null,
      cwdUrl: URL? = null,
      input: Any? = null,
      stdio: StdioConfig = StdioConfig.DEFAULTS,
      env: Map<String, String>? = null,
      shell: String? = null,
      uid: Int? = null,
      gid: Int? = null,
      timeoutSeconds: Int? = null,
      killSignal: String = KILL_SIGNAL,
      maxBuffer: Int = MAX_BUFFER_DEFAULT,
      encoding: String = ENCODING,
      windowsHide: Boolean = WINDOWS_HIDE,
    ): ExecSyncOptions = ExecSyncOptions(
      cwdString = cwdString,
      cwdUrl = cwdUrl,
      input = input,
      stdio = stdio,
      env = env,
      shell = shell,
      uid = uid,
      gid = gid,
      timeout = timeoutSeconds?.toDuration(MILLISECONDS),
      killSignal = killSignal,
      maxBuffer = maxBuffer,
      encoding = encoding,
      windowsHide = windowsHide,
    )

    /** @return [ExecSyncOptions] derived from a guest [Value]. */
    @JvmStatic public fun from(other: Value?): ExecSyncOptions = when (other) {
      null -> DEFAULTS
      else -> when {
        other.isNull -> DEFAULTS
        other.isHostObject -> other.asHostObject()

        other.hasMembers() -> ExecSyncOptions(
          stdio = StdioConfig.from(other.getMember("stdio")),
          cwdString = other.getMember("cwd")?.takeIf { it.isString }?.asString(),
          cwdUrl = other.getMember("cwd")?.takeIf { it.isHostObject }?.asHostObject(),
          input = other.getMember("input")?.takeIf { it.isHostObject }?.asHostObject(),
          env = other.getMember("env")?.takeIf { !it.isNull }?.let { decodeEnvMap(it) },
          shell = other.getMember("shell")?.takeIf { it.isString }?.asString(),
          uid = other.getMember("uid")?.takeIf { it.isNumber }?.asInt(),
          gid = other.getMember("gid")?.takeIf { it.isNumber }?.asInt(),
          timeout = other.getMember("timeout")?.takeIf { it.isNumber }?.asInt()?.toDuration(MILLISECONDS),
          killSignal = other.getMember("killSignal")?.takeIf { it.isString }?.asString() ?: KILL_SIGNAL,
          maxBuffer = other.getMember("maxBuffer")?.takeIf { it.isNumber }?.asInt() ?: MAX_BUFFER_DEFAULT,
          encoding = other.getMember("encoding")?.takeIf { it.isString }?.asString() ?: "buffer",
          windowsHide = other.getMember("windowsHide")?.takeIf { it.isBoolean }?.asBoolean() ?: false,
        )

        else -> throw JsError.typeError("Invalid type provided as `execSync` options")
      }
    }
  }
}
