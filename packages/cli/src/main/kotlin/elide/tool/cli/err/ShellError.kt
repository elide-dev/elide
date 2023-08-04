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

package elide.tool.cli.err

/** Enumerates error cases that may arise during shell execution. */
@Suppress("unused") internal enum class ShellError : ToolErrorCase<ShellError> {
  /** Thrown when a requested language is not supported. */
  LANGUAGE_NOT_SUPPORTED,

  /** Thrown when an error occurs while executing user code. */
  USER_CODE_ERROR,

  /** A file could not be found. */
  FILE_NOT_FOUND,

  /** The target provided is not a file. */
  NOT_A_FILE,

  /** A file is not readable. */
  FILE_NOT_READABLE,

  /** A file type is mismatched with a guest language. */
  FILE_TYPE_MISMATCH,

  /** A filesystem bundle could not be located. */
  BUNDLE_NOT_FOUND,

  /** A filesystem bundle could not be loaded due to a permission error. */
  BUNDLE_NOT_ALLOWED,
}
