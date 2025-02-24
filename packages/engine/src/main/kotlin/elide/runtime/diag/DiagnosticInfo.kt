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

import javax.tools.Diagnostic

/**
 * # Diagnostic Information
 *
 * Describes information supported by a [Diagnostic]; extended into mutable form where this type is used by builders.
 *
 * @property severity Describes the severity of the diagnostic
 * @property id ID or class for this diagnostic type, if supported.
 * @property lang Name of a guest language from which this diagnostic arose, if known.
 * @property tool Name of the tool that generated the diagnostic, if known.
 * @property position Position in the source for the diagnostic, if any
 * @property span Source span for the diagnostic, if any
 * @property reference Reference to the source file or snippet of the diagnostic, if any
 * @property source Refers to the original processed source code
 * @property message Message associated with the diagnostic
 * @property advice Optional corrective action to take, if any
 * @property renderedMessage Optionally contains a pre-formatted version of the message
 */
public interface DiagnosticInfo {
  public val severity: Severity
  public val id: String? get() = null
  public val lang: String? get() = null
  public val tool: String? get() = null
  public val position: SourceLocation? get() = null
  public val span: SourceSpan? get() = null
  public val reference: SourceReference? get() = null
  public val source: String? get() = null
  public val message: String? get() = null
  public val advice: String? get() = null
  public val renderedMessage: String? get() = null

  public companion object {
    /** @return Empty mutable diagnostic record. */
    public fun mutable(): MutableDiagnostic = MutableDiagnostic.create()
  }
}
