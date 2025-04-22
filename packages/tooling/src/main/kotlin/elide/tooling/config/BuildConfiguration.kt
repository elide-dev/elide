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

package elide.tooling.config

import java.nio.file.Path
import elide.exec.Action
import elide.exec.TaskGraph
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.ElideProject
import elide.tooling.registry.ResolverRegistry

public object BuildConfiguration {
  @JvmStatic public fun create(): BuildConfigurator.BuildConfiguration = create(
    Path.of(System.getProperty("user.dir")),
  )

  @JvmStatic public fun create(root: Path): BuildConfigurator.BuildConfiguration = CompositeBuildConfiguration(
    Action.scope(),
    ResolverRegistry.create(),
    TaskGraph.builder(),
    root,
  )

  @JvmStatic public suspend fun ElideProject.configure(with: BuildConfigurator.BuildConfiguration) {
    when (this) {
      is ElideConfiguredProject -> this
      else -> load()
    }.let {
      BuildConfigurators.contribute(it, with)
    }
  }

  @JvmStatic public suspend fun ElideProject.configure(): BuildConfigurator.BuildConfiguration = create().also {
    configure(it)
  }
}
