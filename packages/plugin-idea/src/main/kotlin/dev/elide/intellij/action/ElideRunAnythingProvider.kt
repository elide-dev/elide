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
package dev.elide.intellij.action

import com.intellij.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY
import com.intellij.ide.actions.runAnything.RunAnythingContext
import com.intellij.ide.actions.runAnything.RunAnythingUtil
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandLineProvider
import com.intellij.ide.actions.runAnything.getPath
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import dev.elide.intellij.Constants
import dev.elide.intellij.project.model.fullCommandLine
import dev.elide.intellij.service.ElideExecutionService
import dev.elide.intellij.service.elideProjectIndex
import dev.elide.intellij.settings.ElideSettings
import javax.swing.Icon

/**
 * Extension used to add Elide to the "run anything" feature, including some basic completion for user input.
 */
class ElideRunAnythingProvider : RunAnythingCommandLineProvider() {
  override fun getIcon(value: String): Icon = Constants.Icons.ELIDE
  override fun getHelpIcon(): Icon = Constants.Icons.ELIDE

  override fun getCompletionGroupTitle(): String = Constants.Strings["actions.runAnything.completionGroup"]
  override fun getHelpCommandPlaceholder(): String = Constants.Strings["actions.runAnything.helpPlaceholder"]
  override fun getHelpCommand(): String = Constants.Strings["actions.runAnything.helpCommand"]
  override fun getHelpGroupTitle(): String {
    return Constants.SYSTEM_ID.readableName
  }

  override fun suggestCompletionVariants(dataContext: DataContext, commandLine: CommandLine): Sequence<String> {
    val project = RunAnythingUtil.fetchProject(dataContext)
    val projectPath = (dataContext.getData(EXECUTING_CONTEXT) ?: RunAnythingContext.ProjectContext(project))
      .workingDirectory()

    val tasks = projectPath?.let { project.elideProjectIndex[it] }?.entrypoints?.map {
      it.fullCommandLine
    }?.sorted()?.asSequence().orEmpty()

    return tasks + Constants.DEFAULT_COMMANDS.asSequence()
  }

  override fun run(
    dataContext: DataContext,
    commandLine: CommandLine
  ): Boolean {
    val project = RunAnythingUtil.fetchProject(dataContext)
    val context = dataContext.getData(EXECUTING_CONTEXT) ?: RunAnythingContext.ProjectContext(project)
    val workDirectory = context.workingDirectory() ?: return false

    project.getService(ElideExecutionService::class.java).execute(
      fullCommandLine = commandLine.command,
      externalProjectPath = workDirectory,
      executor = EXECUTOR_KEY.getData(dataContext),
    )

    return true
  }

  private fun RunAnythingContext.workingDirectory(): String? {
    return when (this) {
      is RunAnythingContext.ProjectContext -> getLinkedProjectPath() ?: getPath()
      is RunAnythingContext.ModuleContext -> getLinkedModulePath() ?: getPath()
      else -> getPath()
    }
  }

  private fun RunAnythingContext.ProjectContext.getLinkedProjectPath(): String? {
    return ElideSettings.getSettings(project)
      .linkedProjectsSettings
      .firstOrNull()
      ?.let { ExternalSystemApiUtil.findProjectNode(project, Constants.SYSTEM_ID, it.externalProjectPath) }
      ?.data
      ?.linkedExternalProjectPath
  }

  private fun RunAnythingContext.ModuleContext.getLinkedModulePath(): String? {
    return ExternalSystemApiUtil.getExternalProjectPath(module)
  }
}
