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
