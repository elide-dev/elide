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

package elide.tool.ssg

/**
 * Error case base class.
 *
 * @param message Message for this error. Required.
 * @param cause Cause of this error. Optional.
 * @param exitCode Exit code to use, as applicable.
 */
public sealed class SSGCompilerError(
  message: String,
  cause: Throwable? = null,
  public val exitCode: Int = -1,
) : Throwable(message, cause) {
  /** Generic error case. */
  public class Generic(cause: Throwable? = null): SSGCompilerError(
    "An unknown error occurred.",
    cause,
  )

  /** Invalid argument error. */
  public class InvalidArgument(message: String, cause: Throwable? = null): SSGCompilerError(
    message,
    cause,
    -2,
  )

  /** I/O error. */
  public class IOError(message: String, cause: Throwable? = null): SSGCompilerError(
    message,
    cause,
    -3,
  )

  /** Output error. */
  public class OutputError(message: String, cause: Throwable? = null): SSGCompilerError(
    message,
    cause,
    -4,
  )
}

