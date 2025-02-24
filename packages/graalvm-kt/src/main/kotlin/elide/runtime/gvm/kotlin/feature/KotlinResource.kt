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

package elide.runtime.gvm.kotlin.feature

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.InternalResource
import com.oracle.truffle.api.InternalResource.Env
import com.oracle.truffle.api.InternalResource.Id
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

private const val KOTLIN_RESOURCES_ID = "kotlin-home"

// Informs the compiler about Kotlin Home language resources.
@Id(KOTLIN_RESOURCES_ID, optional = false)
internal class KotlinResource : InternalResource {
  private val kotlinVersion by lazy {
    System.getProperty("elide.kotlin.version")
      ?.ifBlank { null }
      ?.ifEmpty { null }
      ?.takeIf { it.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(-[a-zA-Z0-9]+)?$")) }
      ?: error("Invalid embedded Kotlin version")
  }

  @Throws(IOException::class)
  override fun unpackFiles(env: Env, targetDirectory: Path) {
    val base = basePath()
    val files = base.resolve("elidekotlin.files")
    val kotlinHome = targetDirectory.resolve(kotlinVersion)
    val libpath = kotlinHome.resolve("lib")
    Files.createDirectories(libpath)
    env.unpackResourceFiles(files, libpath, base)
  }

  override fun versionHash(env: Env): String {
    try {
      val base = basePath()
      return env.readResourceLines(base.resolve("elidekotlin.sha256"))[0] as String
    } catch (ioe: IOException) {
      throw CompilerDirectives.shouldNotReachHere(ioe)
    }
  }

  private fun basePath(): Path = Path.of("META-INF", "elide", "embedded", "runtime", "kt")
  companion object {
    @Suppress("unused")
    const val ID: String = KOTLIN_RESOURCES_ID
  }
}
