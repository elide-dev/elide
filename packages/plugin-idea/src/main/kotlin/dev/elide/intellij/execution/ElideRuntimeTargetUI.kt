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

import com.intellij.execution.target.BrowsableTargetEnvironmentType
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetEnvironmentType
import com.intellij.execution.target.getRuntimeType
import com.intellij.execution.target.textFieldWithBrowseTargetButton
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import java.util.function.Supplier

class ElideRuntimeTargetUI<C : TargetEnvironmentConfiguration>(
    private val config: ElideRuntimeTargetConfiguration,
    private val targetType: TargetEnvironmentType<C>,
    private val targetSupplier: Supplier<TargetEnvironmentConfiguration>,
    private val project: Project,
) : BoundConfigurable(config.displayName, config.getRuntimeType().helpTopic) {
  override fun createPanel(): DialogPanel = panel {
      row("Elide Home") {
          if (targetType is BrowsableTargetEnvironmentType) {
              textFieldWithBrowseTargetButton(
                  targetType = targetType,
                  targetSupplier = targetSupplier,
                  project = project,
                  title = "Elide Home",
                  property = config::elideHome.toMutableProperty(),
              )
          } else {
              textField()
                  .bindText(config::elideHome)
                  .align(AlignX.FILL)
                  .comment("Select Elide home directory")
          }
      }
  }
}
