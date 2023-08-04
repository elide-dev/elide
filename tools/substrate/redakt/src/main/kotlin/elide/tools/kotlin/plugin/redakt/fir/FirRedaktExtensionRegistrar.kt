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

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/** Frontend IR extension registrar for the Redakt plugin. */
public class FirRedaktExtensionRegistrar private constructor (
  private val annotation: ClassId,
) : FirExtensionRegistrar() {
  public companion object {
    private val TO_STRING_NAME = Name.identifier("toString")

    /** @return FIR extension for Redakt, prepared against the provided annotation [target] class. */
    @JvmStatic public fun forAnnotation(target: ClassId): FirRedaktExtensionRegistrar = FirRedaktExtensionRegistrar(
      target
    )
  }

    override fun ExtensionRegistrarContext.configurePlugin() {
    +FirRedaktPredicateMatcher.factory(annotation)
    +::FirRedaktCheckers
  }

  internal class FirRedaktCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
      override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(FirRedaktDeclarationChecker)
    }
  }

  internal object FirRedaktDeclarationChecker : FirRegularClassChecker() {
    override fun check(
      declaration: FirRegularClass,
      context: CheckerContext,
      reporter: DiagnosticReporter
    ) {
      val matcher = context.session.redaktPredicateMatcher
      val classRedactedAnnotation = declaration.redactedAnnotation(matcher)
      val redactedProperties = redactedProperties(declaration, matcher)
      val hasRedactedProperty = redactedProperties.isNotEmpty()
      val hasRedactions = classRedactedAnnotation != null || hasRedactedProperty
      if (!hasRedactions) return

      if (hasRedactedProperty && classRedactedAnnotation != null) {
        reporter.reportOn(
          classRedactedAnnotation.source,
          KtErrorsRedakt.REDAKT_ON_CLASS_AND_PROPERTY_WARNING,
          context,
        )
        redactedProperties.forEach {
          reporter.reportOn(
            it.source,
            KtErrorsRedakt.REDAKT_ON_CLASS_AND_PROPERTY_WARNING,
            context,
          )
        }
      }

      val allRedactions = redactedProperties.plus(classRedactedAnnotation).filterNotNull()
      fun report(diagnosticFactory: KtDiagnosticFactory0) {
        for (redaction in allRedactions) {
          reporter.reportOn(redaction.source, diagnosticFactory, context)
        }
      }

      if (declaration.classKind != ClassKind.CLASS) {
        report(KtErrorsRedakt.REDAKT_ON_NON_CLASS_ERROR)
        return
      }

      if (!declaration.hasModifier(KtTokens.DATA_KEYWORD)) {
        report(KtErrorsRedakt.REDAKT_ON_NON_DATA_CLASS_ERROR)
        return
      }

      val customToStringFunction = declaration.declarations.find {
          it is FirFunction &&
          it.isOverride &&
          it.symbol.callableId.callableName == TO_STRING_NAME &&
          it.valueParameters.isEmpty() &&
          it.returnTypeRef.coneType.isString
      }
      if (customToStringFunction != null) {
        reporter.reportOn(
          customToStringFunction.source,
          KtErrorsRedakt.CUSTOM_TO_STRING_IN_REDAKT_CLASS_ERROR,
          context,
        )
      }
    }

    private fun FirRegularClass.redactedAnnotation(matcher: FirRedaktPredicateMatcher) =
      matcher.redactedAnnotation(this)

    private fun redactedProperties(
      declaration: FirRegularClass,
      matcher: FirRedaktPredicateMatcher
    ) = declaration.declarations
        .asSequence()
        .filterIsInstance<FirProperty>()
        .mapNotNull { matcher.redactedAnnotation(it) }
        .toList()
  }

  internal class FirRedaktPredicateMatcher(session: FirSession, private val annotation: ClassId) :
    FirExtensionSessionComponent(session) {
    companion object {
      // Create a predicate matcher with the provided `annotation`.
      @JvmStatic fun factory(annotation: ClassId): Factory {
        return Factory { session -> FirRedaktPredicateMatcher(session, annotation) }
      }
    }

    fun redactedAnnotation(declaration: FirDeclaration): FirAnnotation? {
      return declaration.annotations.firstOrNull { firAnnotation ->
        firAnnotation.annotationTypeRef.coneType.classId == annotation
      }
    }
  }
}
