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
package elide.tooling.project

import java.nio.file.Path
import elide.tooling.project.flags.ProjectFlagKey
import elide.tooling.project.flags.ProjectFlagValue

public interface ProjectManager {
  @JvmRecord public data class ProjectParams(
    val flags: List<Pair<ProjectFlagKey, ProjectFlagValue>> = emptyList(),
    val tasks: List<String> = emptyList(),
    val debug: Boolean = false,
    val release: Boolean = false,
  ) {
    public companion object {
      public val EMPTY: ProjectParams = ProjectParams()
    }
  }

  public interface ProjectEvaluatorInputs {
    public val params: List<String>?
    public val debug: Boolean
    public val release: Boolean

    public companion object {
      public val DEFAULTS: ProjectEvaluatorInputs = object: ProjectEvaluatorInputs {
        override val params: List<String>? get() = null
        override val debug: Boolean get() = false
        override val release: Boolean get() = false
      }
    }
  }

  public suspend fun resolveProject(
    pathOverride: Path? = null,
    inputs: ProjectEvaluatorInputs = ProjectEvaluatorInputs.DEFAULTS,
  ): ElideProject?
}
