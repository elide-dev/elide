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
@file:Suppress("JSUnusedLocalSymbols")

package elide.runtime.javascript

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertDoesNotThrow
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import elide.runtime.lang.javascript.JavaScriptCompilerConfig
import elide.runtime.lang.javascript.JavaScriptPrecompiler
import elide.runtime.precompiler.Precompiler.PrecompileSourceInfo
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest
import elide.testing.annotations.Test

class NativeParserTest {
  companion object {
    @BeforeAll @JvmStatic fun load() {
      try {
        System.loadLibrary("js")
      } catch (e: UnsatisfiedLinkError) {
        throw IllegalStateException("Could not load library 'js'", e)
      }
    }
  }

  // lowering javascript should just return the javascript
  @Test fun `parse javascript`() = runTest {
    // language=JavaScript
    val code = """console.log(5);"""
    val lowered = assertNotNull(assertDoesNotThrow {
      JavaScriptPrecompiler.precompile(
        PrecompileSourceRequest(
          source = PrecompileSourceInfo("example.mjs"),
          config = JavaScriptCompilerConfig.DEFAULT,
        ),
        code,
      )
    })
    // drop spaces for comparison
    val cleanup: (String) -> String = { str ->
      str.replace("\t", "")
        .replace("\n", "")
    }
    assertEquals(
      cleanup(code),
      cleanup(lowered),
    )
  }

  @Test fun `parse and lower typescript`() = runTest {
    // language=TypeScript
    val code = """
      function xyz(param: number): string {
          return `hello: ` + param.toString();
      }
      const x: string = xyz(42);
    """
    val lowered = assertNotNull(assertDoesNotThrow {
      JavaScriptPrecompiler.precompile(
        PrecompileSourceRequest(
          source = PrecompileSourceInfo("hello-world.ts"),
          config = JavaScriptCompilerConfig.DEFAULT,
        ),
        code,
      )
    })
    assertContains(
      lowered,
      "function xyz",
    )
    assertContains(
      lowered,
      "const x="
    )
    assertFalse("number" in lowered)
    assertFalse("string" in lowered)
  }

  @Test fun `parse and lower jsx`() = runTest {
    // language=JSX
    val code = """console.log(<h1>Hello, world!</h1>);"""
    val lowered = assertNotNull(assertDoesNotThrow {
      JavaScriptPrecompiler.precompile(
        PrecompileSourceRequest(
          source = PrecompileSourceInfo("example.jsx"),
          config = JavaScriptCompilerConfig.DEFAULT,
        ),
        code,
      )
    })
    assertNotEquals(code, lowered)
  }

  @Test fun `parse and lower tsx`() = runTest {
    // language=TSX
    val code = """const x: string = "hi"; console.log(<h1>Hello, {x}!</h1>);"""
    val lowered = assertNotNull(assertDoesNotThrow {
      JavaScriptPrecompiler.precompile(
        PrecompileSourceRequest(
          source = PrecompileSourceInfo("example.tsx"),
          config = JavaScriptCompilerConfig.DEFAULT,
        ),
        code,
      )
    })
    assertNotEquals(code, lowered)
  }
}
