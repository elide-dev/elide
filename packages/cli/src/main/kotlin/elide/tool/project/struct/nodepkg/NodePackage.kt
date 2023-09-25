/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.project.struct.nodepkg

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import elide.tool.project.struct.ElidePackage

/**
 * # Node: Package
 *
 * Describes the structure of a `package.json` file.
 */
@JvmRecord @Serializable data class NodePackage(
  val name: String? = null,
  val version: String? = null,
  val description: String? = null,
  val keywords: List<String>? = null,
  val homepage: String? = null,
//  val bugs: ProjectBugs? = null,
  val license: String? = null,
//  val author: ProjectPerson? = null,
//  val contributors: List<ProjectPerson>? = null,
//  val funding: ProjectFunding? = null,
  val files: List<String>? = null,
  val main: String? = null,
  val browser: String? = null,
//  val bin: ProjectBins? = null,
//  val man: ProjectMans? = null,
//  val directories: ProjectDirectories? = null,
  val repository: String? = null,
  val scripts: Map<String, String>? = null,
  val config: Map<String, JsonPrimitive>? = null,
  val dependencies: Map<String, String>? = null,
  val devDependencies: Map<String, String>? = null,
  val peerDependencies: Map<String, String>? = null,
  val peerDependenciesMeta: Map<String, Map<String, JsonPrimitive>>? = null,
  val bundleDependencies: List<String>? = null,
  val optionalDependencies: Map<String, String>? = null,
//  val overrides: Map<String, PackageOverride>? = null,
  val engines: Map<String, String>? = null,
  val os: List<String>? = null,
  val cpu: List<String>? = null,
  val `private`: Boolean = false,
  val publishConfig: Map<String, JsonPrimitive>? = null,
  val workspaces: List<String>? = null,
  val elide: ElidePackage? = null,
) {
  /** Structure describing bug endpoints/URLs. */
  @Serializable sealed interface ProjectBugs {
    /** TBD. */
    @Serializable @JvmRecord data class ProjectBugsUrl(val url: String? = null): ProjectBugs

    /** TBD. */
    @Serializable @JvmRecord data class ProjectBugsConig(
      val url: String? = null,
      val email: String? = null,
    ): ProjectBugs
  }

  /** Structure describing a single involved project member. */
  @Serializable sealed interface ProjectPerson {
    /** TBD. */
    @Serializable @JvmRecord data class ProjectPersonString(val string: String): ProjectBugs

    /** TBD. */
    @Serializable @JvmRecord data class ProjectPersonMap(
      val name: String? = null,
      val url: String? = null,
      val email: String? = null,
    ): ProjectBugs
  }

  /** Describes a map or string entry funding info source. */
  @Serializable sealed interface ProjectFundingEntry {
    /** TBD. */
    @Serializable @JvmRecord data class ProjectFundingString(val string: String? = null): ProjectFundingEntry

    /** TBD. */
    @Serializable @JvmRecord data class ProjectFundingMap(
      val type: String? = null,
      val url: String? = null,
    ): ProjectFundingEntry
  }

  /** Describes a single or multi funding info configuration. */
  @Serializable sealed interface ProjectFunding {
    /** TBD. */
    @Serializable @JvmRecord data class ProjectMultiFunding(val list: List<ProjectFundingEntry>? = null): ProjectFunding

    /** TBD. */
    @Serializable @JvmRecord data class ProjectSingleFunding(val entry: ProjectFundingEntry? = null): ProjectFunding
  }

  /** Describes a string project bin or map of bins. */
  @Serializable sealed interface ProjectBins {
    /** TBD. */
    @Serializable @JvmRecord data class ProectBinString(val string: String? = null): ProjectBins

    /** TBD. */
    @Serializable @JvmRecord data class ProjectBinMap(val bins: Map<String, String>? = null): ProjectBins
  }

  /** Describes a string project bin or list of man-pages. */
  @Serializable sealed interface ProjectMans {
    /** TBD. */
    @Serializable @JvmRecord data class ProjectManString(val string: String? = null): ProjectMans

    /** TBD. */
    @Serializable @JvmRecord data class ProjectManList(val list: List<String>? = null): ProjectMans
  }

  /** TBD. */
  @Serializable @JvmRecord data class ProjectDirectories(
    val bin: String? = null,
    val man: String? = null,
  )

  /** Describes a string repository spec or map. */
  @Serializable sealed interface ProjectRepo {
    /** TBD. */
    @Serializable @JvmRecord data class ProjectRepoString(val string: String? = null): ProjectRepo

    /** TBD. */
    @Serializable @JvmRecord data class ProjectRepoMap(
      val type: String? = null,
      val url: String? = null,
      val directory: String? = null,
    ): ProjectRepo
  }

  /** Describes a package override string or map. */
  @Serializable sealed interface PackageOverride {
    /** TBD. */
    @Serializable @JvmRecord data class PackageOverrideString(val string: String? = null): ProjectRepo

    /** TBD. */
    @Serializable @JvmRecord data class PackageOverrideMap(
      val inner: Map<String, String>? = null,
    ): ProjectRepo
  }
}
