package dev.elide.intellij.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRunConfigurationProducer
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.findConfigurationType
import dev.elide.intellij.Constants

/** Factory for the [ElideRunConfiguration]'s marker type. */
class ElideRuntimeConfigurationProducer : AbstractExternalSystemRunConfigurationProducer() {
  override fun getConfigurationFactory(): ConfigurationFactory {
    return (findConfigurationType(Constants.SYSTEM_ID) as ElideExternalTaskConfigurationType).factory
  }
}
