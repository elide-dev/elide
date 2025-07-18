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
package dev.elide.intellij.settings

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import java.nio.file.Path

/**
 * Execution settings used in tasks like project sync or build. The [dev.elide.intellij.ElideManager] prepares
 * instances of this container to be handed to the [elide.tooling.project.ElideProjectLoader] and other services.
 */
data class ElideExecutionSettings(
  val elideHome: Path,
  val downloadSources: Boolean,
  val downloadDocs: Boolean,
) : ExternalSystemExecutionSettings()
