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

import elide.annotations.API
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.intrinsics.js.URL
import org.graalvm.polyglot.Value
import kotlin.time.Duration
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration
import elide.runtime.intrinsics.js.node.childProcess.ChildProcessDefaults.decodeEnvMap
import elide.runtime.intrinsics.js.node.childProcess.SpawnDefaults.ENCODING
import elide.runtime.intrinsics.js.node.childProcess.SpawnDefaults.KILL_SIGNAL
import elide.runtime.intrinsics.js.node.childProcess.SpawnDefaults.MAX_BUFFER_DEFAULT
import elide.runtime.intrinsics.js.node.childProcess.SpawnDefaults.SERIALIZATION
import elide.runtime.intrinsics.js.node.childProcess.SpawnDefaults.WINDOWS_HIDE

// Default values used in `spawn`.
internal data object SpawnDefaults {
  // Default encoding token to use.
  const val ENCODING: String = ChildProcessDefaults.ENCODING

  // Default signal to send for killing a process.
  const val KILL_SIGNAL: String = ChildProcessDefaults.SIGNAL_SIGKILL

  // Default maximum size for buffered process I/O.
  const val MAX_BUFFER_DEFAULT = ChildProcessDefaults.MAX_BUFFER_DEFAULT

  // Whether to hide the spawned process on Windows platforms. Inert on other platforms.
  const val WINDOWS_HIDE = ChildProcessDefaults.WINDOWS_HIDE

  // Serialization to use between processes.
  const val SERIALIZATION = "json"
}

/**
 * ## Spawn Options
 *
 * Defines options for the [ChildProcessAPI.spawn] method, which is used to asynchronously spawn a child process; these
 * options extend [ProcOptions].
 *
 * Build spawn options from a foreign [Value] via the [SpawnOptions.from] method.
 *
 * @property cwdString Current working directory, as a string.
 * @property cwdUrl Current working directory, as a URL.
 * @property argv0 Explicitly set the value of `argv[0]` sent to the child process.
 * @property detached Whether to run the child process in a detached state.
 * @property stdio Standard I/O configuration.
 * @property env Environment variables.
 * @property shell Shell to invoke for the process.
 * @property uid User ID to run the process as.
 * @property gid Group ID to run the process as.
 * @property serialization Serialization to use between processes.
 * @property windowsVerbatimArguments Whether to use verbatim arguments on Windows.
 * @property timeoutSeconds Timeout for the process, in seconds.
 * @property killSignal Signal to use to kill the process.
 * @property maxBuffer Maximum buffer size for I/O.
 * @property encoding Encoding to use for I/O.
 * @property windowsHide Whether to hide the process window.
 * @property timeout Timeout for the process, in seconds.
 */
@API @JvmRecord public data class SpawnOptions private constructor (
  override val cwdString: String? = null,
  public val cwdUrl: URL? = null,
  public val argv0: String? = null,
  public val detached: Boolean = false,
  override val env: Map<String, String>? = null,
  override val shell: String? = null,
  override val uid: Int? = null,
  override val gid: Int? = null,
  public val serialization: String = SERIALIZATION,
  public val windowsVerbatimArguments: Boolean = shell == "CMD",
  override val timeout: Duration? = null,
  override val killSignal: String = KILL_SIGNAL,
  override val maxBuffer: Int = MAX_BUFFER_DEFAULT,
  override val encoding: String? = ENCODING,
  override val windowsHide: Boolean = WINDOWS_HIDE,
): IdentityProcOptions {
  override val timeoutSeconds: Long? get() = timeout?.inWholeSeconds
  override val stdio: StdioConfig get() = StdioConfig.DEFAULTS

  /** Factories and other helpers for [SpawnOptions]. */
  public companion object {
    /** Default values for [SpawnOptions]. */
    public val DEFAULTS: SpawnOptions = SpawnOptions()

    /** @return [SpawnOptions] created by hand. */
    @Suppress("LongParameterList")
    @JvmStatic public fun of(
      cwdString: String? = null,
      cwdUrl: URL? = null,
      argv0: String? = null,
      detached: Boolean = false,
      env: Map<String, String>? = null,
      shell: String? = null,
      uid: Int? = null,
      gid: Int? = null,
      timeoutSeconds: Int? = null,
      killSignal: String = KILL_SIGNAL,
      maxBuffer: Int = MAX_BUFFER_DEFAULT,
      encoding: String = ENCODING,
      serialization: String = SERIALIZATION,
      windowsHide: Boolean = WINDOWS_HIDE,
      windowsVerbatimArguments: Boolean = shell == "CMD",
    ): SpawnOptions = SpawnOptions(
      cwdString = cwdString,
      cwdUrl = cwdUrl,
      argv0 = argv0,
      detached = detached,
      env = env,
      shell = shell,
      uid = uid,
      gid = gid,
      timeout = timeoutSeconds?.toDuration(MILLISECONDS),
      killSignal = killSignal,
      serialization = serialization,
      maxBuffer = maxBuffer,
      encoding = encoding,
      windowsHide = windowsHide,
      windowsVerbatimArguments = windowsVerbatimArguments,
    )

    /** @return [SpawnOptions] derived from a guest [Value]. */
    @JvmStatic public fun from(other: Value?): SpawnOptions = when (other) {
      null -> DEFAULTS
      else -> when {
        other.isNull -> DEFAULTS
        other.isHostObject -> other.asHostObject()
        other.hasMembers() -> SpawnOptions(
          cwdString = other.getMember("cwd")?.takeIf { it.isString }?.asString(),
          cwdUrl = other.getMember("cwd")?.takeIf { it.isHostObject }?.asHostObject(),
          argv0 = other.getMember("argv0")?.takeIf { it.isString }?.asString(),
          detached = other.getMember("detached")?.takeIf { it.isBoolean }?.asBoolean() ?: false,
          env = other.getMember("env")?.takeIf { !it.isNull }?.let { decodeEnvMap(it) },
          shell = other.getMember("shell")?.takeIf { it.isString }?.asString(),
          uid = other.getMember("uid")?.takeIf { it.isNumber }?.asInt(),
          gid = other.getMember("gid")?.takeIf { it.isNumber }?.asInt(),
          serialization = other.getMember("serialization")?.takeIf { it.isString }?.asString() ?: SERIALIZATION,
          timeout = other.getMember("timeout")?.takeIf { it.isNumber }?.asLong()?.toDuration(MILLISECONDS),
          killSignal = other.getMember("killSignal")?.takeIf { it.isString }?.asString() ?: KILL_SIGNAL,
          maxBuffer = other.getMember("maxBuffer")?.takeIf { it.isNumber }?.asInt() ?: MAX_BUFFER_DEFAULT,
          encoding = other.getMember("encoding")?.takeIf { it.isString }?.asString() ?: "buffer",
          windowsHide = other.getMember("windowsHide")?.takeIf { it.isBoolean }?.asBoolean() ?: false,
          windowsVerbatimArguments = (
                  other.getMember("windowsVerbatimArguments")?.takeIf { it.isBoolean }?.asBoolean()
                    ?: false),
        )

        else -> throw JsError.typeError("Invalid type provided as `spawn` options")
      }
    }
  }
}
