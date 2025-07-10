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
