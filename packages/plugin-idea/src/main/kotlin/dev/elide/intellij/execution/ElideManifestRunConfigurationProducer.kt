package dev.elide.intellij.execution

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import dev.elide.intellij.Constants
import dev.elide.intellij.project.model.ElideEntrypointInfo
import dev.elide.intellij.project.model.fullCommandLine
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.pkl.intellij.PklLanguage
import org.pkl.intellij.psi.*

/** Extension providing "run from gutter" configurations for entrypoints declared in the project manifest. */
class ElideManifestRunConfigurationProducer :
  LazyRunConfigurationProducer<ElideRunConfiguration>() {
  override fun isDumbAware(): Boolean = true

  override fun isPreferredConfiguration(self: ConfigurationFromContext?, other: ConfigurationFromContext?): Boolean {
    return self?.configuration is ElideRunConfiguration && other?.configuration !is ElideRunConfiguration
  }

  override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
    return self.configuration is ElideRunConfiguration && other.configuration !is ElideRunConfiguration
  }

  override fun getConfigurationFactory(): ConfigurationFactory {
    return (ExternalSystemUtil.findConfigurationType(Constants.SYSTEM_ID) as ElideExternalTaskConfigurationType).factory
  }

  override fun setupConfigurationFromContext(
    configuration: ElideRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement?>
  ): Boolean {
    val element = sourceElement.get() ?: return false
    if (element.language != PklLanguage) return false

    val entrypointInfo = resolveEntrypointInfo(element) ?: return false

    configuration.name = entrypointInfo.displayName
    configuration.rawCommandLine = entrypointInfo.fullCommandLine
    configuration.settings.externalProjectPath = context.project.basePath

    configuration.entrypointKind = entrypointInfo.kind
    configuration.entrypointValue = entrypointInfo.value

    return true
  }

  override fun isConfigurationFromContext(
    configuration: ElideRunConfiguration,
    context: ConfigurationContext
  ): Boolean {
    val element = context.location?.psiElement ?: return false
    if (element.language != PklLanguage) return false

    val entrypointInfo = resolveEntrypointInfo(element) ?: return false
    return entrypointInfo.kind == configuration.entrypointKind &&
            entrypointInfo.value == configuration.entrypointValue
  }

  override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
    ProgressManager.checkCanceled()
    val entrypoint = context.location?.psiElement?.let { resolveEntrypointInfo(it) } ?: return null
    return getConfigurationSettingsList(RunManager.getInstance(context.project)).find { configurationSettings ->
      val elideConfiguration = (configurationSettings.configuration as ElideRunConfiguration)
      elideConfiguration.entrypointKind == entrypoint.kind && elideConfiguration.entrypointValue == entrypoint.value
    }
  }

  private fun resolveEntrypointInfo(element: PsiElement): ElideEntrypointInfo? {
    val kind = when (val prop = element.getParentOfType<PklClassProperty>(strict = true)?.propertyName?.text) {
      "jvm" -> ElideEntrypointInfo.Kind.JvmMainClass
      "entrypoint" -> ElideEntrypointInfo.Kind.Generic
      "scripts" -> ElideEntrypointInfo.Kind.Script
      else -> return null
    }

    val value = when (kind) {
      ElideEntrypointInfo.Kind.JvmMainClass -> {
        element.getParentOfType<PklObjectProperty>(true)
          ?.takeIf { it.propertyName.text == "main" }?.expr?.resolvedText()
      }

      ElideEntrypointInfo.Kind.Script -> element.getParentOfType<PklObjectEntry>(true)?.keyExpr?.resolvedText()
      ElideEntrypointInfo.Kind.Generic -> element.getParentOfType<PklObjectElement>(true)?.expr?.resolvedText()
    } ?: return null

    return when (kind) {
      ElideEntrypointInfo.Kind.Script -> ElideEntrypointInfo.script(value)
      ElideEntrypointInfo.Kind.JvmMainClass -> ElideEntrypointInfo.jvmMain(value)
      ElideEntrypointInfo.Kind.Generic -> ElideEntrypointInfo.generic(value)
    }
  }

  private fun PklExpr.resolvedText(): String {
    return ((this as? PklUnqualifiedAccessExpr)?.memberName
      ?.reference?.resolve()
      ?.let { (it as? PklProperty)?.expr?.resolvedText() } ?: text)
      .trim('"')
  }
}
