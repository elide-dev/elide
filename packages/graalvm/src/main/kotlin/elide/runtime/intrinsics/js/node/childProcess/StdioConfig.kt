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
import java.lang.ProcessBuilder.Redirect
import elide.annotations.API
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols.IGNORE
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols.INHERIT
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols.PIPE

/**
 * ## Standard I/O Symbols
 *
 * Symbolic names used for different [StdioConfig] operating modes; see each mode for behavior documentation.
 */
@API public object StdioSymbols {
  /**
   * ## Mode: Pipe
   *
   * Pipes the underlying stream to the child process.
   */
  public const val PIPE: String = "pipe"

  /**
   * ## Mode: Inherit
   *
   * Inherits the underlying stream from the parent process.
   */
  public const val INHERIT: String = "inherit"

  /**
   * ## Mode: Ignore
   *
   * Discards the underlying stream data.
   */
  public const val IGNORE: String = "ignore"
}

/**
 * ## Standard I/O Configuration
 *
 * Describes the standard I/O configuration for a child process, including modes for standard input, output, and error.
 *
 * Each mode can be one of the following:
 * - `pipe`: Pipe the underlying stream to the child process.
 * - `inherit`: Inherit the underlying stream from the parent process.
 *
 * @property stdin Standard input mode.
 * @property stdout Standard output mode.
 * @property stderr Standard error mode.
 */
@API public data class StdioConfig(
  internal val stdin: Any? = null,
  internal val stdout: Any? = null,
  internal val stderr: Any? = null,
) {
  /**
   * Apply this standard I/O configuration to a given [ProcessBuilder].
   *
   * @param processBuilder Process builder to apply this configuration to.
   */
  public fun applyTo(processBuilder: ProcessBuilder): ProcessBuilder = processBuilder.apply {
    applyToStream(
      stdin,
      { redirectInput(Redirect.DISCARD) },
      { redirectInput(Redirect.PIPE) },
      { redirectInput(Redirect.INHERIT) })

    applyToStream(
      stdout,
      { redirectOutput(Redirect.DISCARD) },
      { redirectOutput(Redirect.PIPE) },
      { redirectOutput(Redirect.INHERIT) })

    applyToStream(
      stderr,
      { redirectError(Redirect.DISCARD) },
      { redirectError(Redirect.PIPE) },
      { redirectError(Redirect.INHERIT) })
  }

  public companion object {
    /** Default standard I/O configuration. */
    public val DEFAULTS: StdioConfig = StdioConfig(PIPE, PIPE, PIPE)

    private fun applyToStream(value: Any?, doDrop: () -> Unit, doPipe: () -> Unit, doInherit: () -> Unit) {
      when (value) {
        PIPE -> doPipe()
        IGNORE -> doDrop()
        INHERIT -> doInherit()
        else -> error("Unexpected stdio mode: $value")
      }
    }

    /** @return Standard I/O configuration that sets all modes to `pipe`. */
    @JvmStatic public fun pipe(): StdioConfig = DEFAULTS

    /** @return Standard I/O configuration that sets all modes to `inherit`. */
    @JvmStatic public fun inherit(): StdioConfig = StdioConfig(INHERIT, INHERIT, INHERIT)

    /** @return Standard I/O configuration derived from a guest value. */
    @Suppress("MagicNumber")
    @JvmStatic public fun from(other: Value?): StdioConfig = when (val cfg = other) {
      null -> DEFAULTS
      else -> when {
        // foreign `null`: equivalent to host `null`
        cfg.isNull -> DEFAULTS

        // foreign string: interpret as a stdio configuration token
        cfg.isString -> when (val token = cfg.asString()) {
          PIPE -> pipe()
          INHERIT -> inherit()
          else -> throw JsError.typeError("Invalid stdio configuration token: $token")
        }

        // foreign array: positional stdio configuration
        //
        // expect to have between 1 and 3 arguments, which positionally align with:
        // [`stdin`, `stdout`, `stderr`]
        //
        // each is expected to be one of:
        // - a string token (`pipe` or `inherit`) describing what to do
        // - a number describing a file descriptor to use
        // - a file descriptor object
        //
        // if the array is "short," we apply partially, aligned with the same streams.
        // if the array is "long," we apply fully, ignoring any extra elements.
        cfg.hasArrayElements() -> when (cfg.arraySize) {
          // just a configuration for `stdin`
          1L -> StdioConfig(cfg.getArrayElement(0))

          // configuration for `stdin` and `stdout`
          2L -> StdioConfig(cfg.getArrayElement(0), cfg.getArrayElement(1))

          // configuration for `stdin`, `stdout`, and `stderr`
          3L -> StdioConfig(
            cfg.getArrayElement(0),
            cfg.getArrayElement(1),
            cfg.getArrayElement(2),
          )

          // anything else is a failure
          else -> throw JsError.typeError("Invalid stdio configuration: $cfg")
        }

        // otherwise, unrecognized
        else -> throw JsError.typeError("Invalid stdio configuration: $cfg")
      }
    }
  }
}
