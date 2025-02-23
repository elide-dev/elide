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

import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*
import kotlinx.coroutines.test.runTest
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.runtime.precompiler.Precompiler
import elide.runtime.precompiler.Precompiler.PrecompileSourceInfo
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest
import elide.runtime.precompiler.precompileSafe

class KotlinPrecompilerTest {
  @Test fun `load kotlin precompiler provider`() {
    val all = ServiceLoader.load(Precompiler.Provider::class.java)
      .stream()
      .map { it.type() }
      .toList()
    assertContains(all, KotlinPrecompiler.Provider::class.java)
  }

  @Test fun `load kotlin precompiler`() {
    val prov = ServiceLoader.load(Precompiler.Provider::class.java)
      .stream()
      .map { it.get() }
      .toList()
      .firstOrNull { it is KotlinPrecompiler.Provider }
    assertNotNull(prov)
    val svc = prov.get()
    assertNotNull(svc)
  }

  @Test fun `precompile kotlin`() = runTest {
    // language=kotlin
    val src = """
      fun main() {
        System.out.println("Hello, world!")
      }
    """.trimIndent()

    val (diag, result) = assertNotNull(
      assertDoesNotThrow {
        KotlinPrecompiler.precompileSafe(
          PrecompileSourceRequest(
            source = PrecompileSourceInfo(
              name = "Example.kt",
            ),
            config = KotlinCompilerConfig.DEFAULT,
          ),
          src,
        )
      }
    )
    assertNotNull(diag)
    assertNotNull(result)
    assertTrue(result.name.isNotEmpty())
    assertTrue(result.path.exists())
  }
}
