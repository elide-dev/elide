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
package elide.secrets.remote

import elide.secrets.dto.persisted.Named

/**
 * Initializer for connecting to a [Remote].
 *
 * @author Lauri Heino <datafox>
 */
internal interface RemoteInitializer : Named {
  suspend fun initialize(prompts: MutableList<String>): Remote
}
