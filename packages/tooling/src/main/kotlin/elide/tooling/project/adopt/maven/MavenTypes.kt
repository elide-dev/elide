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

package elide.tooling.project.adopt.maven

import java.nio.file.Path

/**
 * Represents a parent POM reference.
 */
public data class ParentPom(
  val groupId: String,
  val artifactId: String,
  val version: String,
  val relativePath: String? = null
)

/**
 * Represents a Maven repository.
 */
public data class MavenRepository(
  val id: String,
  val url: String,
  val name: String? = null
)

/**
 * Represents a Maven profile.
 */
public data class MavenProfile(
  val id: String,
  val properties: Map<String, String> = emptyMap(),
  val dependencies: List<MavenDependency> = emptyList(),
  val dependencyManagement: Map<String, String> = emptyMap(),
  val repositories: List<MavenRepository> = emptyList()
)

/**
 * Represents a Maven build plugin.
 */
public data class MavenPlugin(
  val groupId: String,
  val artifactId: String,
  val version: String? = null
)

/**
 * Represents a dependency in Maven POM.
 */
public data class MavenDependency(
  val groupId: String,
  val artifactId: String,
  val version: String?,
  val scope: String = "compile",
  val type: String = "jar",
  val classifier: String? = null,
  val optional: Boolean = false
) {
  /**
   * Get Maven coordinate string for this dependency.
   */
  public fun coordinate(): String = buildString {
    append("$groupId:$artifactId")
    if (version != null) append(":$version")
  }
}

/**
 * Represents a Maven POM file.
 */
public data class PomDescriptor(
  val groupId: String,
  val artifactId: String,
  val version: String,
  val name: String?,
  val description: String?,
  val packaging: String = "jar",
  val dependencies: List<MavenDependency> = emptyList(),
  val dependencyManagement: Map<String, String> = emptyMap(),
  val modules: List<String> = emptyList(),
  val properties: Map<String, String> = emptyMap(),
  val parent: ParentPom? = null,
  val repositories: List<MavenRepository> = emptyList(),
  val profiles: List<MavenProfile> = emptyList(),
  val plugins: List<MavenPlugin> = emptyList(),
  val path: Path
)
