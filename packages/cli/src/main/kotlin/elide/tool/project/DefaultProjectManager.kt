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
package elide.tool.project

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.stream.Collectors
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.plugins.env.EnvConfig.EnvVar
import elide.tool.io.WorkdirManager

/** Default implementation of [ProjectManager]. */
@Singleton internal class DefaultProjectManager @Inject constructor (
  private val workdir: WorkdirManager,
) : ProjectManager {
  companion object {
    // Filename for `.env` files.
    private const val DOT_ENV_FILE = ".env"

    // Filename for local `.env` file.
    private const val DOT_ENV_FILE_LOCAL = ".env.local"

    // Resolve the project configuration which we are going to use as the main config.
    @Suppress("UNUSED_PARAMETER", "FunctionOnlyReturningConstant", "unused")
    @JvmStatic private fun resolveMainConfig(dir: File): ProjectConfig? = null

    // Read a .env file into a sorted map of env var strings, or return `null` if it cannot be read.
    @JvmStatic private fun readDotEnvFileToMap(file: File): Pair<String, SortedMap<String, String>>? {
      return if (file.exists() && file.canRead()) {
        try {
          file.absolutePath to file.inputStream().bufferedReader(StandardCharsets.UTF_8).use { inbuf ->
            inbuf.readText().lines().parallelStream().map {
              if (it?.isNotBlank() == true && it.contains("=")) {
                it.split("=").let { split ->
                  split.first() to split[1]
                }
              } else null
            }.filter {
              it != null
            }.collect(
              Collectors.toMap(
                { it!!.first },
                { it!!.second },
                { left, _ -> error("Duplicate env key in same `.env` file: $left") },
                ::ConcurrentSkipListMap,
              )
            )
          }
        } catch (_: IOException) {
          return null  // cannot read file
        }
      } else null  // file does not exist or is not readable
    }

    // Read any present `.env` file in the project directory.
    @JvmStatic private fun readDotEnv(dir: File): ProjectInfo.ProjectEnvironment? {
      return listOfNotNull(
        readDotEnvFileToMap(dir.resolve(DOT_ENV_FILE)),
        readDotEnvFileToMap(dir.resolve(DOT_ENV_FILE_LOCAL)),
      ).let { maps ->
        if (maps.isNotEmpty()) {
          // after parsing our env maps, merge into a single sorted map, with later entries winning in the flattened
          // stream (resulting in later maps overriding).
          ProjectInfo.ProjectEnvironment.wrapping(
            maps.stream().flatMap { (file, group) ->
              group.entries.stream().map {
                object : Map.Entry<String, EnvVar> {
                  override val key: String get() = it.key
                  override val value: EnvVar get() = EnvVar.fromDotenv(file, it.key, it.value)
                }
              }
            }.collect(
              Collectors.toMap(
                { it.key },
                { it.value },
                { _, right -> right },
                ::ConcurrentSkipListMap,
              )
            )
          )
        } else {
          null  // no environment available
        }
      }
    }

    // Read files from the root project directory.
    @JvmStatic fun readProjectInfo(dir: File): ProjectInfo? {
      val rootPath = dir.absolutePath
      val env = readDotEnv(dir)

      return ProjectInfo.of(
        root = rootPath,
        env = env,
      )
    }
  }

  override suspend fun resolveProjectAsync(): Deferred<ProjectInfo?> = coroutineScope {
    async {
      workdir.projectRoot()?.let { projectDir ->
        readProjectInfo(projectDir.toFile())
      }
    }
  }
}
