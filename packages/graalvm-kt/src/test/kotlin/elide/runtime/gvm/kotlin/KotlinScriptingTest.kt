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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.kotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.io.path.Path
import kotlin.test.Ignore
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.kotlin.Kotlin
import elide.runtime.precompiler.Precompiler.PrecompileSourceInfo
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest
import elide.runtime.precompiler.precompileSafe

class KotlinScriptingTest {
  @Ignore
  @Test fun `exec kotlin script`() {
    // language=kotlin
    val src = """
      fun fn() {
        System.out.println("Hello, Kotlin Scripting!")
      }
      fn()
    """.trimIndent()

    val (diag, result) = assertNotNull(
      assertDoesNotThrow {
        runBlocking {
          KotlinPrecompiler.precompileSafe(
            PrecompileSourceRequest(
              source = PrecompileSourceInfo(
                name = "Example.kts",
              ),
              config = KotlinCompilerConfig.DEFAULT,
            ),
            src,
          )
        }
      }
    )
    assertNotNull(diag)
    assertNotNull(result)
    assertTrue(result.name.isNotEmpty())
    assertIs<KotlinScriptCallable>(result)
    val engine = PolyglotEngine {
      configure(Kotlin) {
        guestClasspathRoots.add(requireNotNull(System.getenv("ELIDE_KOTLIN_HOME")).let {
          Path(it)
        })
        val langHomeResources = Path(System.getProperty("user.home"))
          .resolve("elide")
          .resolve("resources")
          .resolve("kotlin")
          .resolve(KotlinLanguage.VERSION)
          .resolve("lib")

        guestClasspathRoots.add(langHomeResources)
      }
    }
    val ctx = engine.acquire {
      // Nothing to configure.
    }
    val returnvalue = assertDoesNotThrow { (result).apply(ctx) }
    assertNotNull(returnvalue)
  }
}
