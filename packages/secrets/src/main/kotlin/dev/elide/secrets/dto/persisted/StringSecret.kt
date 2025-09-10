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
package dev.elide.secrets.dto.persisted

import dev.elide.secrets.Utils
import kotlinx.serialization.Serializable

/**
 * [Secret] containing string data.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class StringSecret(override val name: String, override val value: String, val env: String? = null) :
  Secret<String> {
  init {
    Utils.checkName(name, "Secret")
  }
}
