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

  override fun export(builder: StringBuilder) {
    when (val project = project) {
      null -> builder.apply {
        appendLine("No project configured with Elide.")
      }

      else -> builder.apply {
        appendLine()
        appendLine("### Elide: Project Advice")
        appendLine()
        appendLine("There is an Elide Project configuration file present for this project (`elide.pkl`).")
        appendLine("Project configuration summary:")
        appendLine()
        append(projectName(project))
        append(projectVersion(project))
        appendLine()
      }
    }
  }
}
