package dev.elide.intellij.execution

import com.intellij.execution.target.LanguageRuntimeConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent

class ElideRuntimeTargetConfiguration : LanguageRuntimeConfiguration(ElideRuntimeType.TYPE_ID),
    PersistentStateComponent<ElideRuntimeTargetConfiguration.State> {
  var elideHome: String = ""

  override fun getState(): State {
    return State().also { it.elideHome = elideHome }
  }

  override fun loadState(state: State) {
    elideHome = state.elideHome.orEmpty()
  }

  class State : BaseState() {
    var elideHome by string()
  }
}
