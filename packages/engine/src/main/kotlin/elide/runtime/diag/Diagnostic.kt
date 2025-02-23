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

/**
 * # Diagnostic
 *
 * Describes a diagnostic event which was emitted by a tool such as a compiler or linter; such events carry a [severity]
 * along with some [source] and [span] information.
 *
 * Diagnostics take the shape created by a given tool. Tools may elect to fill a subset of all properties.
 *
 * @property severity Describes the severity of the diagnostic
 * @property id ID or class for this diagnostic type, if supported.
 * @property lang Name of a guest language from which this diagnostic arose, if known.
 * @property lang Tool which is reporting this diagnostic, if known/applicable.
 * @property position Position in the source for the diagnostic, if any
 * @property span Source span for the diagnostic, if any
 * @property reference Reference to the source file or snippet of the diagnostic, if any
 * @property source Refers to the original processed source code
 * @property message Message associated with the diagnostic
 * @property advice Optional corrective action to take, if any
 * @property renderedMessage Optionally contains a pre-formatted version of the message
 */
@Serializable
@ConsistentCopyVisibility
@JvmRecord public data class Diagnostic internal constructor (
  @JvmField public val severity: Severity,
  @JvmField public val id: String? = null,
  @JvmField public val lang: String? = null,
  @JvmField public val tool: String? = null,
  @JvmField public val position: SourceLocation? = null,
  @JvmField public val span: SourceSpan? = null,
  @JvmField public val reference: SourceReference? = null,
  @JvmField public val source: String? = null,
  @JvmField public val message: String? = null,
  @JvmField public val advice: String? = null,
  @JvmField public val renderedMessage: String? = null,
) {
  /**
   * @return [DiagnosticInfo] created from this [Diagnostic].
   */
  public fun asInfo(): DiagnosticInfo = object : DiagnosticInfo {
    override val severity: Severity = this@Diagnostic.severity
    override val id: String? = this@Diagnostic.id
    override val lang: String? = this@Diagnostic.lang
    override val tool: String? = this@Diagnostic.tool
    override val position: SourceLocation? = this@Diagnostic.position
    override val span: SourceSpan? = this@Diagnostic.span
    override val reference: SourceReference? = this@Diagnostic.reference
    override val source: String? = this@Diagnostic.source
    override val message: String? = this@Diagnostic.message
    override val advice: String? = this@Diagnostic.advice
    override val renderedMessage: String? = this@Diagnostic.renderedMessage
  }

  /** Factory methods and builders for [Diagnostic] objects. */
  public companion object {
    /**
     * Create a [Diagnostic] from a [DiagnosticInfo].
     *
     * @param info Info-compliant diagnostic
     * @return Instance of [Diagnostic] created from the supplied [info]
     */
    @JvmStatic public fun from(info: DiagnosticInfo): Diagnostic = Diagnostic(
      severity = info.severity,
      id = info.id,
      lang = info.lang,
      tool = info.tool,
      position = info.position,
      span = info.span,
      reference = info.reference,
      source = info.source,
      message = info.message,
      advice = info.advice,
      renderedMessage = info.renderedMessage,
    )
  }
}
