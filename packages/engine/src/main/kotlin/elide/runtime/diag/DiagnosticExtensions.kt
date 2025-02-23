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

import java.util.*
import javax.tools.Diagnostic

// Static string used for Java diagnostics.
private const val JAVA_LANG = "java"

// Static string used for Java compiler-issued diagnostics.
private const val JAVAC_TOOL_COMPILER = "compiler"

/**
 * Create an immutable diagnostic record from a Java compiler diagnostic.
 *
 * @receiver Diagnostic emitted by a Java tool
 * @param tool Name of the tool producing this diagnostic; defaults to the static string `compiler`
 */
public fun Diagnostic<*>.toDiagnosticInfo(tool: String = JAVAC_TOOL_COMPILER): DiagnosticInfo =
  object : DiagnosticInfo {
    override val lang: String get() = JAVA_LANG
    override val tool: String get() = tool
    override val severity: Severity
      get() = when (kind) {
        Diagnostic.Kind.ERROR -> Severity.ERROR
        Diagnostic.Kind.MANDATORY_WARNING,
        Diagnostic.Kind.WARNING -> Severity.WARN
        Diagnostic.Kind.NOTE,
        Diagnostic.Kind.OTHER,
        null -> Severity.INFO
      }

    override val message: String? get() = this@toDiagnosticInfo.getMessage(Locale.getDefault())
    override val position: SourceLocation get() = SourceLocation(
      line = this@toDiagnosticInfo.lineNumber.toUInt(),
      column = this@toDiagnosticInfo.columnNumber.toUInt()
    )
  }

/**
 * Create an immutable diagnostic record from a Java compiler diagnostic.
 *
 * @receiver Diagnostic emitted by a Java tool
 * @param tool Name of the tool producing this diagnostic; defaults to the static string `compiler`
 */
public fun Diagnostic<*>.toDiagnostic(tool: String = JAVAC_TOOL_COMPILER): elide.runtime.diag.Diagnostic =
  elide.runtime.diag.Diagnostic.from(toDiagnosticInfo(tool))
