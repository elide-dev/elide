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

@file:Suppress("UnstableApiUsage")

package elide.tooling.config

import java.nio.file.Path
import elide.exec.ActionScope
import elide.exec.TaskGraphBuilder
import elide.tooling.registry.ResolverRegistry

// Build configuration state; materialized as all contributors complete.
internal class CompositeBuildConfiguration (
  override val actionScope: ActionScope,
  override val resolvers: ResolverRegistry,
  override val taskGraph: TaskGraphBuilder,
  override val projectRoot: Path,
) : BuildConfigurator.BuildConfiguration
