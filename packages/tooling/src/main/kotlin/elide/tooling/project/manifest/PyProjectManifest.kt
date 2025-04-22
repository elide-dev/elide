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

package elide.tooling.project.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import elide.tooling.project.ProjectEcosystem

@JvmRecord @Serializable public data class PyProjectManifest(
  @SerialName("build-system") public val buildSystem: BuildSystemConfig,
  @SerialName("project") public val project: ProjectConfig,
) : PackageManifest {
  override val ecosystem: ProjectEcosystem get() = ProjectEcosystem.Python

  @JvmRecord @Serializable public data class BuildSystemConfig(
    @SerialName("requires") public val requires: List<String> = emptyList(),
    @SerialName("build-backend") public val buildBackend: String? = null,
  )

  @JvmRecord @Serializable public data class ProjectConfig(
    @SerialName("name") public val name: String,
    @SerialName("version") public val version: String? = null,
    @SerialName("dependencies") public val dependencies: List<String> = emptyList(),
    @SerialName("optional-dependencies") public val optionalDependencies: Map<String, List<String>> = emptyMap(),
    @SerialName("requires-python") public val requiresPython: String? = null,
    @SerialName("authors") public val authors: List<ProjectPerson> = emptyList(),
    @SerialName("maintainers") public val maintainers: List<ProjectPerson> = emptyList(),
    @SerialName("description") public val description: String? = null,
    @SerialName("readme") public val readme: String? = null,
    @SerialName("license") public val license: String? = null,
    @SerialName("license-files") public val licenseFiles: List<String> = emptyList(),
    @SerialName("keywords") public val keywords: List<String> = emptyList(),
    @SerialName("classifiers") public val classifiers: List<String> = emptyList(),
    @SerialName("urls") public val urls: Map<String, String> = emptyMap(),
    @SerialName("scripts") public val scripts: Map<String, String> = emptyMap(),
    @SerialName("gui-scripts") public val guiScripts: Map<String, String> = emptyMap(),
    @SerialName("entry-points") public val entrypoints: Map<String, Map<String, String>> = emptyMap(),
  )

  @JvmRecord @Serializable public data class ProjectPerson(
    @SerialName("name") public val name: String? = null,
    @SerialName("email") public val email: String? = null,
  )
}
