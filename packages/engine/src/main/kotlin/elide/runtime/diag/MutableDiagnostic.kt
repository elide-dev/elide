package elide.runtime.diag

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import elide.runtime.diag.Severity.INFO

/**
 * ## Mutable Diagnostic
 *
 * Describes a class which carries fully mutable diagnostic information; this class is also made available reflectively
 * for spawning new diagnostics from native contexts.
 *
 * Diagnostics should always be finalized into a [Diagnostic] instance to enforce immutability and thread-safety.
 *
 * @see Diagnostic Canonical diagnostic type
 */
@ReflectiveAccess @Introspected public class MutableDiagnostic internal constructor() : MutableDiagnosticInfo {
  override var severity: Severity = Severity.WARN
  override var id: String? = null
  override var lang: String? = null
  override var tool: String? = null
  override var position: SourceLocation? = null
  override var span: SourceSpan? = null
  override var reference: SourceReference? = null
  override var source: String? = null
  override var message: String? = null
  override var advice: String? = null
  override var renderedMessage: String? = null

  public companion object {
    /** @return Empty mutable diagnostic record. */
    @JvmStatic public fun create(): MutableDiagnostic = MutableDiagnostic()
  }

  /**
   * Build this mutable diagnostic into a finalized immutable record.
   *
   * @return Finalized diagnostic record
   */
  public fun build(): Diagnostic = Diagnostic(
    severity = severity,
    id = id,
    lang = lang,
    tool = tool,
    position = position,
    span = span,
    reference = reference,
    source = source,
    message = message,
    advice = advice,
    renderedMessage = renderedMessage
  )

  /**
   * Set the severity level using the native ordinal integer representing a given enumerated value.
   *
   * @param int Ordinal integer of the severity level
   * @return This diagnostic instance
   */
  @JvmName("setSeverity") public fun setSeverity(int: Int): MutableDiagnostic = apply {
    severity = Severity.entries.getOrNull(int) ?: INFO
  }
}
