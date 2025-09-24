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

package elide.tool.cli.cmd.runner

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.EnvVariableSource
import elide.runtime.plugins.env.environment
import elide.tool.cli.cfg.ElideCLITool
import elide.tooling.project.ElideProject

/** Specifies settings for application environment. */
@Introspected @ReflectiveAccess class EnvironmentConfig {
  /** Specifies whether the runtime should honor dotenv files. */
  @CommandLine.Option(
    names = ["--env:dotenv"],
    description = ["Whether to honor .env files; defaults to `true`"],
    defaultValue = "true",
  )
  internal var dotenv: Boolean = true

  /** Specifies whether the runtime should honor dotenv files. */
  @CommandLine.Option(
    names = ["--env"],
    description = ["Additional environment variables to set, in x=y format"],
    arity = "0..N",
  )
  internal var envVars: Map<String, String> = emptyMap()

  /** Apply these settings to created execution contexts. */
  @Suppress("KotlinConstantConditions")
  internal fun apply(
    project: ElideProject?,
    config: PolyglotEngineConfiguration,
    secretsEnv: Map<String, String>,
    host: Boolean = false,
    dotenv: Boolean = true,
  ) = config.environment {
    // inject `NODE_ENV`
    environment("NODE_ENV", if (ElideCLITool.ELIDE_RELEASE_TYPE == "DEV") {
      "development"
    } else {
      "production"
    })

    if (host) System.getenv().entries.forEach {
      mapToHostEnv(it.key)
    }

    // apply secret environment variables first
    secretsEnv.forEach { environment(it.key, it.value) }

    // apply project-level environment variables (if applicable)
    project?.env?.vars?.forEach {
      if (it.value.isPresent) {
        if (it.value.source == EnvVariableSource.DOTENV && !dotenv) {
          return@forEach  // skip .env vars if so instructed
        }
        environment(it.key, it.value.value)
      }
    }

    // apply manually-installed environment variables
    envVars.forEach {
      if (it.value.isNotBlank() && it.value.isNotBlank()) {
        environment(it.key, it.value)
      }
    }
  }
}
