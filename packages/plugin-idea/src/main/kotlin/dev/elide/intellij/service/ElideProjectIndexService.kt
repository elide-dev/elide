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
package dev.elide.intellij.service

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import dev.elide.intellij.service.ElideProjectIndexService.ElideProjectIndex
import dev.elide.intellij.project.model.ElideProjectData

/** Retrieves the index service for this project. */
inline val Project.elideProjectIndex: ElideProjectIndexService
  get() = service()

/**
 * Provides access to the persistent project index, which holds information about linked Elide projects. Use the
 * [contains] and [get] operators for quick access to project data.
 *
 * Changes to the index via [update] will be automatically persisted by the IDE.
 */
@Service(Service.Level.PROJECT)
@State(name = "dev.elide.intellij.project.data.ElideProjectDataService", storages = [Storage("elideProjects.xml")])
class ElideProjectIndexService : SerializablePersistentStateComponent<ElideProjectIndex>(ElideProjectIndex()) {
  /** Returns a set of all entries in the index. */
  val entries: Set<Map.Entry<String, ElideProjectData>> get() = state.projects.entries

  /** Update the full project index to a new value. */
  fun update(projects: Map<String, ElideProjectData>) {
    updateState { it.copy(projects = projects) }
  }

  /** Update the index for a project at the given [externalProjectPath]. */
  fun update(externalProjectPath: String, data: ElideProjectData) {
    updateState { it.copy(projects = it.projects + (externalProjectPath to data)) }
  }

  operator fun contains(linkedProjectPath: String): Boolean = state.projects.containsKey(linkedProjectPath)
  operator fun get(linkedProjectPath: String): ElideProjectData? = state.projects[linkedProjectPath]

  /**
   * Persistent state holder for the [ElideProjectIndexService]. This class should not be used directly, see
   * [ElideProjectIndexService.get] and [ElideProjectIndexService.update] instead.
   */
  data class ElideProjectIndex(
    @JvmField val projects: Map<String, ElideProjectData> = emptyMap(),
  )
}
