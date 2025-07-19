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
