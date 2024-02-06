/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded.cfg

import jakarta.inject.Singleton
import elide.embedded.env.EnvResolver
import elide.embedded.env.EnvVariableName
import elide.embedded.env.SystemPropertyName

/** Resolves environment variables and system properties directly from VM intrinsics. */
@Singleton internal class SystemEnvResolver : EnvResolver {
  override fun resolveEnv(key: EnvVariableName): String? = System.getenv(key)?.ifBlank { null }
  override fun resolveProperty(key: SystemPropertyName): String? = System.getProperty(key)?.ifBlank { null }
}
