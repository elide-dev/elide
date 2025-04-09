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

import org.graalvm.polyglot.Value
import java.io.File
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import elide.runtime.gvm.kotlin.scripting.ElideKotlinScript
import elide.runtime.precompiler.Precompiler

// Kotlin script host executor.
internal object ElideKotlinScriptExecutor {
  @Suppress("UNUSED_PARAMETER")
  @JvmStatic fun execute(source: Precompiler.PrecompileSourceInfo, file: File): Value? {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ElideKotlinScript>()
    val eval = ScriptEvaluationConfiguration()
    BasicJvmScriptingHost().eval(file.toScriptSource(), compilationConfiguration, eval)
    return null
  }
}
