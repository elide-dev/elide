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

package elide.tools.kotlin.plugin.redakt

import org.jetbrains.kotlin.config.CompilerConfigurationKey

/** Configuration key indicating whether the Redakt plugin is enabled. */
public val KEY_ENABLED: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey<Boolean>(
  "enabled"
)

/** Configuration key specifying the replacement mask string for the Redakt plugin. */
public val KEY_MASK: CompilerConfigurationKey<String> = CompilerConfigurationKey<String>(
  "mask"
)

/** Configuration key specifying the annotation to scan for to trigger the Redakt plugin. */
public val KEY_ANNOTATION: CompilerConfigurationKey<String> = CompilerConfigurationKey<String>(
  "annotation"
)
