package elide.runtime.gvm.kotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlinx.coroutines.test.runTest
import kotlin.io.path.exists
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import elide.runtime.precompiler.Precompiler.PrecompileSourceInfo
import elide.runtime.precompiler.Precompiler.PrecompileSourceRequest
import elide.runtime.precompiler.precompileSafe

class KotlinScriptingTest {
  @Test fun `exec kotlin script`() = runTest {
    // language=kotlin
    val src = """
      fun fn() {
        System.out.println("Hello, Kotlin Scripting!")
      }
      fn()
    """.trimIndent()

    val (diag, result) = assertNotNull(
      assertDoesNotThrow {
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
    )
    assertNotNull(diag)
    assertNotNull(result)
    assertTrue(result.name.isNotEmpty())
    assertTrue(result.path.exists())
    assertIs<KotlinScriptCallable>(result)
    val returnvalue = assertDoesNotThrow { (result).call() }
    assertNull(returnvalue)
  }
}
