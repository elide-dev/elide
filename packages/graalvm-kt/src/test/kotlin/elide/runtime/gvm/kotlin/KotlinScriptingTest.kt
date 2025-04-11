@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.kotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlinx.coroutines.test.runTest
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
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
    assertIs<KotlinScriptCallable>(result)
    val engine = PolyglotEngine {
      configure(Kotlin) {
        guestClasspathRoots.add(requireNotNull(System.getenv("ELIDE_KOTLIN_HOME")).let {
          Path(it).absolutePathString()
        })
        val langHomeResources = Path(System.getProperty("user.home"))
          .resolve("elide")
          .resolve("resources")
          .resolve("kotlin")
          .resolve(KotlinLanguage.VERSION)
          .resolve("lib")

        guestClasspathRoots.add(langHomeResources.absolutePathString())
      }
    }
    val ctx = engine.acquire {
      // Nothing to configure.
    }
    val returnvalue = assertDoesNotThrow { (result).apply(ctx) }
    assertNotNull(returnvalue)
  }
}
