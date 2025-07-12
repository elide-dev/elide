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
package elide.tooling.project.agents

import kotlin.io.path.name
import elide.tooling.project.ElideConfiguredProject

/**
 * ## Project Advice
 *
 * Builds [AdviceStanza] entries for a [project].
 */
public class ProjectAdvice internal constructor(private val project: ElideConfiguredProject?): RenderableAdvice {
  public companion object {
    /**
     * Build advice for the provided [project].
     *
     * @return [ProjectAdvice] instance for the provided [project].
     */
    @JvmStatic public fun build(project: ElideConfiguredProject): ProjectAdvice = ProjectAdvice(project)
  }

  private fun projectKeyValue(key: String, value: String? = null): String = buildString {
    append("- ")
    append(key)
    append(": ")
    when (value) {
      null -> append("None set")
      else -> append("`${value}`")
    }
    append("\n")
  }

  private fun projectName(project: ElideConfiguredProject): String {
    return (project.manifest.name?.let {
      it to null
    } ?: (
      project.root.name to "No project name set, but the directory is named "
    )).let { (name, prefixIfAny) ->
      projectKeyValue("Project Name", prefixIfAny?.let { "$it$name" } ?: name)
    }
  }

  private fun projectVersion(project: ElideConfiguredProject): String =
    projectKeyValue("Version", project.manifest.version?.toString())

  private fun projectDescription(project: ElideConfiguredProject): String =
    projectKeyValue("Description", project.manifest.description)

  override fun export(builder: StringBuilder) {
    when (val project = project) {
      null -> builder.apply {
        appendLine("No project configured with Elide.")
      }

      else -> builder.apply {
        appendLine("## Project Advice")
        appendLine()
        appendLine("There is an Elide Project configuration file present for this project (`elide.pkl`).")
        appendLine("Project configuration summary:")
        appendLine()
        append(projectName(project))
        append(projectVersion(project))
        append(projectDescription(project))
        if (project.manifest.scripts.isNotEmpty()) {
          appendLine()
          appendLine("### Project Scripts")
          appendLine()
          append("The project has the following scripts defined, all usable with `elide <script>`")
          append(" or `elide run <script>`:")
          appendLine()
          project.manifest.scripts.forEach { (name, script) ->
            appendLine()
            appendLine("- `${name}`: `$script`")
          }
        }
        appendLine()
        appendLine("### Project Dependencies")
        appendLine()
        var hasDeps = false
        if (project.manifest.dependencies.maven.hasPackages()) {
          hasDeps = true
          appendLine("The project has the following Maven dependencies defined:")
          appendLine()
          project.manifest.dependencies.maven.packages.forEach { artifact ->
            appendLine("- `${artifact.group}:${artifact.name}@${artifact.version}`")
          }
          appendLine()
        }
        if (project.manifest.dependencies.npm.hasPackages()) {
          hasDeps = true
          appendLine("The project has the following NPM dependencies defined:")
          appendLine()
          project.manifest.dependencies.npm.packages.forEach { artifact ->
            append("- `${artifact.name}")
            when (val version = artifact.version) {
              null -> append("`")
              else -> append("@$version`")
            }
            append("\n")
          }
        }
        if (!hasDeps) {
          appendLine("No dependencies defined for this project via Elide.")
        }
      }
    }
  }
}
