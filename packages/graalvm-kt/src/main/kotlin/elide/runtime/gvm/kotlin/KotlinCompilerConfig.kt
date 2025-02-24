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

package elide.runtime.gvm.kotlin

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import elide.runtime.precompiler.Precompiler

/**
 * Configures the Kotlin compiler which is embedded within Elide.
 *
 * @property apiVersion The API version of the Kotlin compiler.
 * @property languageVersion The language version of the Kotlin compiler.
 */
public data class KotlinCompilerConfig(
  public val apiVersion: ApiVersion,
  public val languageVersion: LanguageVersion,
) : Precompiler.Configuration {
  public companion object {
    /** Default Kotlin compiler configuration. */
    public val DEFAULT: KotlinCompilerConfig = KotlinPrecompiler.currentConfig()
  }
}
