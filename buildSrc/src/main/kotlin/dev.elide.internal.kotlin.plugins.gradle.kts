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

import dev.elide.internal.ElideInternalPlugin
import dev.elide.internal.kotlin.plugin.InternalRedaktPlugin

apply<ElideInternalPlugin>()

the<ElideInternalPlugin.ElideInternalExtension>().apply {
  // Library version.
  version.set(Elide.version)

  kotlinPlugins.apply {
    // Plugin: Redakt
    redakt.enabled.set(true)
    redakt.verbose.set(true)
    redakt.mask.set("●●●●")
    redakt.annotation.set("elide.annotations.data.Sensitive")
  }
}

apply<InternalRedaktPlugin>()
