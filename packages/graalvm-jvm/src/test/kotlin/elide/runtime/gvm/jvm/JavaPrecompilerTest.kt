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
package elide.runtime.gvm.jvm

import org.junit.jupiter.api.assertDoesNotThrow
import java.util.ServiceLoader
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.runtime.precompiler.Precompiler
import elide.runtime.precompiler.Precompiler.PrecompileSourceInfo
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest
import elide.testing.annotations.Test

class JavaPrecompilerTest {
  @Test fun `load java precompiler provider`() {
    val all = ServiceLoader.load(Precompiler.Provider::class.java)
      .stream()
      .map { it.type() }
      .toList()
    assertContains(all, JavaPrecompiler.Provider::class.java)
  }

  @Test fun `load java precompiler`() {
    val prov = ServiceLoader.load(Precompiler.Provider::class.java)
      .stream()
      .map { it.get() }
      .toList()
      .firstOrNull { it is JavaPrecompiler.Provider }
    assertNotNull(prov)
    val svc = prov.get()
    assertNotNull(svc)
  }

  @Test fun `precompile java`() {
    // language=Java
    val src = """
      package example;

      public class Example {
        public static void main(String[] args) {
          System.out.println("Hello, world!");
        }
      }
    """.trimIndent()

    val bytes = assertNotNull(
      assertDoesNotThrow {
        runBlocking {
          JavaPrecompiler.precompile(
            PrecompileSourceRequest(
              source = PrecompileSourceInfo(
                name = "Example.java",
              ),
              config = JavaCompilerConfig.DEFAULT,
            ),
            src,
          )
        }
      }
    )
    assertTrue(bytes.array().isNotEmpty())
  }
}
