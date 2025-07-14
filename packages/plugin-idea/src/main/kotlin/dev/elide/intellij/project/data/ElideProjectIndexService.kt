package dev.elide.intellij.project.data

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import dev.elide.intellij.project.data.ElideProjectIndexService.ElideProjectIndex

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
