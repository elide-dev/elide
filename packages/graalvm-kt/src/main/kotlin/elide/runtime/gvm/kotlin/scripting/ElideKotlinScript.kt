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

package elide.runtime.gvm.kotlin.scripting

import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.api.with
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.resolveFromScriptSourceAnnotations
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import elide.runtime.gvm.kotlin.scripting.ScriptWithMavenDepsConfiguration.configureMavenDepsOnAnnotations

@KotlinScript(
  fileExtension = "elide.kts",
  compilationConfiguration = ScriptWithMavenDepsConfiguration::class,
)
public abstract class ElideKotlinScript

public object ScriptWithMavenDepsConfiguration : ScriptCompilationConfiguration(
  {
    defaultImports(
      DependsOn::class,
      Repository::class,
    )
    jvm {
      dependenciesFromCurrentContext(wholeClasspath = true)
    }
    refineConfiguration {
      onAnnotations(
        DependsOn::class,
        Repository::class,
        handler = ::configureMavenDepsOnAnnotations,
      )
    }
  }
) {
  private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

  // Handler that reconfigures the compilation on the fly
  internal fun configureMavenDepsOnAnnotations(
    context: ScriptConfigurationRefinementContext,
  ): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
      ?: return context.compilationConfiguration.asSuccess()
    return runBlocking {
      resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
      context.compilationConfiguration.with {
        dependencies.append(JvmDependency(it))
      }.asSuccess()
    }
  }

  @Suppress("unused")
  private fun readResolve(): Any = ScriptWithMavenDepsConfiguration
}
