package elide.tools.kotlin.plugin.redakt.fir

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0 as DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.error0 as err
import org.jetbrains.kotlin.diagnostics.warning0 as warning

// TODO expose custom error messages when K2 supports it:
// https://youtrack.jetbrains.com/issue/KT-53510
public object KtErrorsRedakt {
  public val REDAKT_ON_CLASS_AND_PROPERTY_WARNING: DiagnosticFactory by warning<PsiElement>(
    SourceElementPositioningStrategies.NAME_IDENTIFIER
  )

  public val REDAKT_ON_NON_CLASS_ERROR: DiagnosticFactory by err<PsiElement>(
    SourceElementPositioningStrategies.NAME_IDENTIFIER
  )

  public val REDAKT_ON_NON_DATA_CLASS_ERROR: DiagnosticFactory by err<PsiElement>(
    SourceElementPositioningStrategies.NAME_IDENTIFIER
  )

  public val CUSTOM_TO_STRING_IN_REDAKT_CLASS_ERROR: DiagnosticFactory by err<PsiElement>(
    SourceElementPositioningStrategies.NAME_IDENTIFIER
  )
}
