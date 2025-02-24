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
package elide.runtime.diag

import kotlinx.serialization.Serializable
import elide.core.api.Symbolic

// Diagnostic severity constants
private const val DIAG_SEVERITY_INFO = "info"
private const val DIAG_SEVERITY_WARN = "warn"
private const val DIAG_SEVERITY_ERROR = "error"

/**
 * ## Diagnostic Severity
 *
 * Enumerates severity levels for diagnostic events issued by tools such as compilers, linters and formatters. Severity
 * is typically mapped from a tool's own internal severity levels.
 */
@Serializable
public enum class Severity (override val symbol: String) : Symbolic<String> {
  /**
   * ## Severity: Info.
   *
   * An informational diagnostic, typically a non-critical issue. By default, informational diagnostics are not shown
   * unless the user is operating from a development context (for example, running a linter).
   */
  INFO(DIAG_SEVERITY_INFO),

  /** ## Severity: Warning.
   *
   * A warning diagnostic, typically a non-critical issue which may require user attention. Warnings are shown by
   * default in most contexts.
   */
  WARN(DIAG_SEVERITY_WARN),

  /** ## Severity: Error.
   *
   * A critical diagnostic which requires user attention. Errors are shown by default in all contexts.
   */
  ERROR(DIAG_SEVERITY_ERROR),
}
