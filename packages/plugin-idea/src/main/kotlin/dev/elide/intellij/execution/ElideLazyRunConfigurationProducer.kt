package dev.elide.intellij.execution

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.findConfigurationType
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import dev.elide.intellij.Constants

// TODO: stubbed
// requires proper implementation of run configurations with regard to project entry points
/** Extension responsible for providing "run from gutter icon" configurations. */
class ElideLazyRunConfigurationProducer : LazyRunConfigurationProducer<ElideRunConfiguration>() {
  override fun getConfigurationFactory(): ConfigurationFactory {
    return (findConfigurationType(Constants.SYSTEM_ID) as ElideExternalTaskConfigurationType).factory
  }

  override fun setupConfigurationFromContext(
    configuration: ElideRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement?>
  ): Boolean {
    return false
  }

  override fun isConfigurationFromContext(
    configuration: ElideRunConfiguration,
    context: ConfigurationContext
  ): Boolean {
    return false
  }
}
