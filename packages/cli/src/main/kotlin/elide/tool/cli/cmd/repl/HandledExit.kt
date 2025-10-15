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
package elide.tool.cli.cmd.repl

// Internal signal which lets the outer entrypoint know that the runner has exited in a handled manner, and asks that
// the final exit code be set to the provided value.
internal class HandledExit private constructor (
  internal val exitCode: Int,
  internal val causeMessage: String? = null,
  cause: Throwable? = null,
) : RuntimeException(SENTINEL, cause, true, false) {
  companion object {
    private const val SENTINEL = "ಠ_ಠ"

    @JvmStatic fun isHandledExit(exc: Throwable): Boolean {
      return exc is HandledExit && exc.message == SENTINEL
    }

    @JvmStatic fun notify(exitCode: Int, message: String? = null, cause: Throwable? = null): Nothing {
      throw create(
        exitCode = exitCode,
        message = message,
        cause = cause,
      )
    }

    @JvmStatic fun create(exitCode: Int, message: String? = null, cause: Throwable? = null): HandledExit {
      return HandledExit(
        exitCode = exitCode,
        causeMessage = message,
        cause = cause,
      )
    }
  }
}
