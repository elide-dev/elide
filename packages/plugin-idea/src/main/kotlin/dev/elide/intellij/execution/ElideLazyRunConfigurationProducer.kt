package dev.elide.intellij.execution

import com.intellij.execution.Location
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.findConfigurationType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import dev.elide.intellij.Constants
import dev.elide.intellij.project.data.ElideEntrypointInfo
import dev.elide.intellij.project.data.ElideProjectData
import dev.elide.intellij.project.data.ExternalProjectPath
import dev.elide.intellij.project.data.fullCommandLine
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinMainFunctionDetector
import org.jetbrains.kotlin.idea.base.codeInsight.findMainOwner
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationProducer.Companion.getMainClassJvmName
import org.jetbrains.kotlin.psi.KtDeclarationContainer

/** Extension responsible for providing "run from gutter icon" configurations. */
@Suppress("UnstableApiUsage") class ElideLazyRunConfigurationProducer :
  LazyRunConfigurationProducer<ElideRunConfiguration>() {
  override fun isDumbAware(): Boolean = true

  override fun isPreferredConfiguration(self: ConfigurationFromContext?, other: ConfigurationFromContext?): Boolean {
    return self?.configuration is ElideRunConfiguration && other?.configuration !is ElideRunConfiguration
  }

  override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
    return self.configuration is ElideRunConfiguration && other.configuration !is ElideRunConfiguration
  }

  override fun getConfigurationFactory(): ConfigurationFactory {
    return (findConfigurationType(Constants.SYSTEM_ID) as ElideExternalTaskConfigurationType).factory
  }

  override fun setupConfigurationFromContext(
    configuration: ElideRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement?>
  ): Boolean {
    val location = context.location ?: return false
    val container = getEntryPointContainer(location) ?: return false
    val startClassFQName = getMainClassJvmName(container) ?: return false

    val (externalProject, entrypoint) = findJvmEntrypoint(context, startClassFQName) ?: return false

    configuration.name = entrypoint.displayName
    configuration.rawCommandLine = entrypoint.fullCommandLine
    configuration.settings.externalProjectPath = externalProject
    configuration.entrypoint = entrypoint

    return true
  }

  override fun isConfigurationFromContext(
    configuration: ElideRunConfiguration,
    context: ConfigurationContext
  ): Boolean {
    val entryPointContainer = getEntryPointContainer(context.location) ?: return false
    val startClassFQName = getMainClassJvmName(entryPointContainer) ?: return false

    val storedEntrypoint = configuration.entrypoint as? ElideEntrypointInfo.JvmMainEntrypoint ?: return false
    return storedEntrypoint.value == startClassFQName
  }

  override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
    val entryPointContainer = getEntryPointContainer(context.location) ?: return null
    val startClassFQName = getMainClassJvmName(entryPointContainer) ?: return null

    ProgressManager.checkCanceled()
    return getConfigurationSettingsList(RunManager.getInstance(context.project)).find { configurationSettings ->
      val elideConfiguration = (configurationSettings.configuration as ElideRunConfiguration)
      val storedEntrypoint = elideConfiguration.entrypoint as? ElideEntrypointInfo.JvmMainEntrypoint
      storedEntrypoint?.value == startClassFQName
    }
  }

  private fun getEntryPointContainer(location: Location<*>?): KtDeclarationContainer? {
    val element = location?.psiElement ?: return null
    return KotlinMainFunctionDetector.getInstanceDumbAware(location.project).findMainOwner(element)
  }

  private fun findJvmEntrypoint(
    context: ConfigurationContext,
    qualifiedName: String
  ): Pair<ExternalProjectPath, ElideEntrypointInfo>? {
    for ((path, project) in ElideProjectData.load(context.project).entries) {
      val jvmMain = project.entrypoints.find {
        it is ElideEntrypointInfo.JvmMainEntrypoint && it.value == qualifiedName
      } ?: continue

      return path to jvmMain
    }

    return null
  }
}
