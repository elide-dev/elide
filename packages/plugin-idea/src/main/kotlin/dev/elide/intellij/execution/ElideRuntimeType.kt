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

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.target.LanguageRuntimeType
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetEnvironmentType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants
import java.util.function.Supplier
import javax.swing.Icon

/** Provides a runtime type for Elide, which can be referenced by run configurations and tasks. */
class ElideRuntimeType : LanguageRuntimeType<ElideRuntimeTargetConfiguration>(TYPE_ID) {
  override val configurableDescription: String = "Elide runtime configuration"
  override val launchDescription: String = "Run with Elide"

  override fun isApplicableTo(runConfig: RunnerAndConfigurationSettings): Boolean = true

  override fun createConfigurable(
    project: Project,
    config: ElideRuntimeTargetConfiguration,
    targetEnvironmentType: TargetEnvironmentType<*>,
    targetSupplier: Supplier<TargetEnvironmentConfiguration>
  ): Configurable {
    return ElideRuntimeTargetUI(config, targetEnvironmentType, targetSupplier, project)
  }

  override fun findLanguageRuntime(target: TargetEnvironmentConfiguration): ElideRuntimeTargetConfiguration? {
    return target.runtimes.findByType()
  }

  override val displayName: String = "Elide"
  override val icon: Icon = Constants.Icons.RELOAD_PROJECT

  override fun createSerializer(config: ElideRuntimeTargetConfiguration): PersistentStateComponent<*> = config
  override fun createDefaultConfig() = ElideRuntimeTargetConfiguration()
  override fun duplicateConfig(config: ElideRuntimeTargetConfiguration) = duplicatePersistentComponent(this, config)

  companion object {
    const val TYPE_ID = "ElideRuntime"
  }
}

