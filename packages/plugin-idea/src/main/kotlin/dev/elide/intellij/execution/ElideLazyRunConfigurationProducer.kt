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
import dev.elide.intellij.project.model.ElideEntrypointInfo
import dev.elide.intellij.service.elideProjectIndex
import dev.elide.intellij.project.model.fullCommandLine
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
    val startClassFQName = context.location
      ?.let(::getEntryPointContainer)
      ?.let(::getMainClassJvmName)
      ?: return false

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
    val startClassFQName = getEntryPointContainer(context.location)
      ?.let(::getMainClassJvmName)
      ?: return false

    val storedEntrypoint = configuration.entrypoint?.takeIf { it.kind == ElideEntrypointInfo.Kind.JvmMainClass }
      ?: return false

    return storedEntrypoint.value == startClassFQName
  }

  override fun findExistingConfiguration(context: ConfigurationContext): RunnerAndConfigurationSettings? {
    val entryPointContainer = getEntryPointContainer(context.location) ?: return null
    val startClassFQName = getMainClassJvmName(entryPointContainer) ?: return null

    ProgressManager.checkCanceled()
    return getConfigurationSettingsList(RunManager.getInstance(context.project)).find { configurationSettings ->
      val elideConfiguration = (configurationSettings.configuration as ElideRunConfiguration)
      val storedEntrypoint = elideConfiguration.entrypoint?.takeIf { it.kind == ElideEntrypointInfo.Kind.JvmMainClass }
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
  ): Pair<String, ElideEntrypointInfo>? {
    for ((path, project) in context.project.elideProjectIndex.entries) {
      val jvmMain = project.entrypoints.find {
        it.kind == ElideEntrypointInfo.Kind.JvmMainClass && it.value == qualifiedName
      } ?: continue

      return path to jvmMain
    }

    return null
  }
}
