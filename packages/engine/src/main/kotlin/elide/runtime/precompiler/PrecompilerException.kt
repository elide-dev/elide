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
package elide.runtime.precompiler

import elide.runtime.diag.DiagnosticsSuite
import elide.runtime.diag.Severity

/**
 * ## Precompiler Exception
 *
 * Thrown when a [Precompiler] encounters an error; this includes cases where diagnostics are present which may indicate
 * compiler errors or warnings despite the precompiler producing output.
 */
public sealed class PrecompilerException (
  public open val severity: Severity,
  message: String?,
  cause: Throwable?,
) : RuntimeException(message ?: "Precompilation failed", cause) {
  public open val fatal: Boolean get() = severity == Severity.ERROR
  protected constructor(message: String?) : this(Severity.ERROR, message, null)
  protected constructor() : this(null)
}

/** Exception which is thrown when a precompiler suffers a critical error. */
public class PrecompilerError(message: String, cause: Throwable?) :
  PrecompilerException(Severity.ERROR, message, cause) {
  override val fatal: Boolean get() = true
  public constructor(message: String) : this(message, null)
}

/** Exception which is thrown when a precompiler yields diagnostics; may be non-fatal. */
public open class PrecompilerNotice(
  public val diagnostics: DiagnosticsSuite,
  severity: Severity,
  message: String? = null,
  cause: Throwable? = null,
  override val fatal: Boolean = false,
) : PrecompilerException(severity, message, cause) {
  public companion object {
    @JvmStatic public fun from(suite: DiagnosticsSuite): PrecompilerNotice =
      PrecompilerNotice(suite, suite.severity, "Precompiler failed with diagnostics")
  }
}

/** Exception which is thrown when a precompiler yields diagnostics and produces output. */
public class PrecompilerNoticeWithOutput(
  diagnostics: DiagnosticsSuite,
  public val output: Any,
  severity: Severity,
  message: String? = null,
  cause: Throwable? = null,
  fatal: Boolean = false,
) : PrecompilerNotice(diagnostics, severity, message, cause, fatal) {
  public companion object {
    @JvmStatic public fun from(suite: DiagnosticsSuite, output: Any): PrecompilerNoticeWithOutput =
      PrecompilerNoticeWithOutput(suite, output, suite.severity, "Precompiler failed with diagnostics")
  }
}
