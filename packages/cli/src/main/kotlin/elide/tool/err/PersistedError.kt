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

@file:Suppress("DataClassPrivateConstructor")

package elide.tool.err

import dev.elide.uuid.Uuid
import org.graalvm.nativeimage.ImageInfo
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.HostPlatform
import elide.tool.cli.ElideTool
import elide.tool.cli.Statics
import elide.tool.err.ErrorHandler.*
import elide.tool.err.ErrorHandler.ErrorCoordinates.Companion.coordinates
import elide.tool.err.PersistedError.ErrorInfo.Companion.info

/**
 * # Persisted Error
 *
 * Describes a single persisted error object, which was written by the [DefaultStructuredErrorRecorder], and may be
 * loaded and reported to GitHub at a later time.
 *
 * @param version Format version; defaults to `1`.
 * @param id ID of the error record.
 * @param timestamp Timestamp indicating when the error occurred.
 * @param runtime Info about the Elide runtime which produced this error.
 * @param args Command-line arguments which were active when this error was produced, as applicable.
 * @param error Information about the error itself -- the stacktrace, exception type, and so on.
 */
@Serializable @JvmRecord data class PersistedError @OptIn(DelicateElideApi::class) private constructor (
  val version: Int = 1,
  val id: String,
  val timestamp: Instant,
  val error: ErrorInfo,
  val runtime: RuntimeInfo = RuntimeInfo(),
  val os: HostPlatform.OperatingSystem,
  val arch: HostPlatform.Architecture,
  val args: List<String> = Statics.args.get() ?: emptyList(),
) {
  companion object {
    /**
     * Create a persisted error record from the provided [event].
     *
     * @param event Error event.
     * @param id UUID to use; defaults to the event's UUID.
     * @param timestamp Timestamp to use; defaults to the event's timestamp.
     */
    @OptIn(DelicateElideApi::class)
    @JvmStatic fun create(
      event: ErrorEvent,
      id: Uuid = event.uuid,
      timestamp: Instant = event.timestamp,
    ): PersistedError = HostPlatform.resolve().let { platform ->
      return PersistedError(
        id = id.toString(),
        timestamp = timestamp,
        error = event.info(),
        os = platform.os,
        arch = platform.arch,
      )
    }
  }

  /**
   * ## Persisted Error: Runtime Info
   *
   * Captures information about the current Elide [version], whether we are running in [native] mode, the active VM
   * [properties], and a selection of [env] variables.
   *
   * @param version Version of Elide.
   * @param native Whether we are currently running in native mode.
   * @param env Select environment variables.
   * @param properties Properties active in the VM.
   */
  @Serializable @JvmRecord data class RuntimeInfo(
    val version: String = ElideTool.version(),
    val native: Boolean = ImageInfo.inImageCode(),
    val env: Map<String, String> = capturedEnvVariables.associateWith { System.getenv(it) }.toSortedMap(),
    val properties: Map<String, String> = System.getProperties().mapNotNull {
      try {
        it.key.toString() to it.value.toString()
      } catch (err: Throwable) {
        null
      }
    }.toMap().toSortedMap(),
  ) {
    companion object {
      // Environment variables captured in the report.
      private val capturedEnvVariables = listOf(
        "PATH",
        "JAVA_HOME",
        "GRAALVM_HOME",
      )
    }
  }

  /**
   * ## Persisted Error: Error Info
   *
   * @param className Type of exception which was thrown.
   * @param message Message provided by the exception, if any.
   * @param stacktrace Stacktrace lines, if known.
   * @param cause Cause information (a nested [ErrorInfo]), if known and applicable.
   */
  @Serializable @JvmRecord data class ErrorInfo(
    @SerialName("class") val className: String,
    val message: String? = null,
    val stacktrace: String? = null,
    val cause: ErrorInfo? = null,
    val coordinates: ErrorCoordinates? = null,
    val frames: List<ErrorStackFrame> = emptyList(),
  ) {
    internal companion object {
      /**
       * Create an [ErrorInfo] from an [ErrorEvent].
       *
       * @receiver Error event.
       * @return Error info record.
       */
      @JvmStatic fun ErrorEvent.info(): ErrorInfo {
        return ErrorInfo(
          className = errorType,
          message = message,
          stacktrace = stacktrace,
          coordinates = coordinates(),
          frames = frames ?: emptyList(),
          cause = error.cause?.info(),
        )
      }

      /**
       * Create an [ErrorInfo] from a [Throwable].
       *
       * @receiver Throwable (error).
       * @return Error info record.
       */
      @JvmStatic fun Throwable.info(): ErrorInfo {
        return ErrorInfo(
          className = javaClass.name,
          message = message,
          cause = if (cause !== this) cause?.info() else null,
        )
      }
    }
  }
}
