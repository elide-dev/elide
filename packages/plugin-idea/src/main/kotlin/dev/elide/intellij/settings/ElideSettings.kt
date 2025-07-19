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

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.XCollection
import java.util.*

/**
 * Global settings for Elide, managed through the IDE's settings panel. The preferences stored using this class are
 * collected along others into [ElideExecutionSettings], which are then used to control plugin features.
 *
 * @see ElideConfigurable
 */
@Service(Service.Level.PROJECT)
@State(name = "dev.elide.intellij.settings.ElideSettings", storages = [Storage("elide.xml")])
class ElideSettings(
  project: Project
) : AbstractExternalSystemSettings<ElideSettings, ElideProjectSettings, ElideSettingsListener>(
  ElideSettingsListener.TOPIC,
  project,
), PersistentStateComponent<ElideSettings.ElideSettingsState> {
  var downloadSources: Boolean = true
  var downloadDocs: Boolean = true

  override fun copyExtraSettingsFrom(other: ElideSettings) {
    downloadSources = other.downloadSources
    downloadDocs = other.downloadDocs
  }

  override fun checkSettings(old: ElideProjectSettings, new: ElideProjectSettings) {
    // noop
  }

  override fun subscribe(
    listener: ExternalSystemSettingsListener<ElideProjectSettings?>,
    parentDisposable: Disposable
  ) {
    doSubscribe(
      object : ElideSettingsListener {
        override fun onBulkChangeStart() = listener.onBulkChangeStart()
        override fun onBulkChangeEnd() = listener.onBulkChangeEnd()
        override fun onProjectRenamed(oldName: String, newName: String) = listener.onProjectRenamed(oldName, newName)
        override fun onProjectsLinked(settings: Collection<ElideProjectSettings?>) = listener.onProjectsLinked(settings)
        override fun onProjectsLoaded(settings: Collection<ElideProjectSettings?>) = listener.onProjectsLoaded(settings)
        override fun onProjectsUnlinked(linkedProjectPaths: Set<String?>) =
          listener.onProjectsUnlinked(linkedProjectPaths)
      },
      parentDisposable,
    )
  }

  override fun getState(): ElideSettingsState {
    return ElideSettingsState().also {
      fillState(it)

      it.downloadSources = downloadSources
      it.downloadDocs = downloadDocs
    }
  }

  override fun loadState(state: ElideSettingsState) {
    super.loadState(state)

    downloadSources = state.downloadSources
    downloadDocs = state.downloadDocs
  }

  /**
   * Serializable state persisted by the IDE. This class contains the [ElideProjectSettings] for all linked projects.
   */
  class ElideSettingsState : State<ElideProjectSettings> {
    private val linkedSettings = TreeSet<ElideProjectSettings>()

    var downloadSources: Boolean = true
    var downloadDocs: Boolean = true

    @XCollection(elementTypes = [ElideProjectSettings::class])
    override fun getLinkedExternalProjectsSettings(): Set<ElideProjectSettings> = linkedSettings

    override fun setLinkedExternalProjectsSettings(settings: Set<ElideProjectSettings>?) {
      settings?.let(linkedSettings::addAll)
    }
  }

  companion object {
    @JvmStatic fun getSettings(project: Project): ElideSettings {
      return project.getService(ElideSettings::class.java)
    }
  }
}
