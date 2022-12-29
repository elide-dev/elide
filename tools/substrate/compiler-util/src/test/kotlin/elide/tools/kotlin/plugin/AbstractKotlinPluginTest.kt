@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
@file:Suppress("DEPRECATION")

package elide.tools.kotlin.plugin

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin as ksource
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.io.TempDir

/**
 * # Kotlin Plugin Test
 *
 * Base class which provides shared logic for testing Kotlin compiler plugins provided by Elide. Test compiler runs are
 * executed against the plugin.
 */
public abstract class AbstractKotlinPluginTest {
  @TempDir public lateinit var tempDir: java.io.File

  //
  public fun kotlin(
    name: String,
    @Language("kotlin") code: String,
    trimIndent: Boolean = true,
  ): KotlinCompilation.Result {
    val target = ksource(
      name = name,
      contents = code,
      trimIndent = trimIndent,
    )
    return compile(
      target,
    )
  }

  //
  public fun compile(
    @Language("kotlin") code: String,
    trimIndent: Boolean = true,
    verbose: Boolean = false,
    assertions: KotlinCompilation.Result.() -> Unit,
  ): KotlinCompilation.Result {
    val target = ksource("test.kt", code, trimIndent)
    val compilation = prepareCompilation(target)
    compilation.verbose = verbose
    val op = compilation.compile()

    require(op.exitCode == KotlinCompilation.ExitCode.OK) {
      "Compilation failed: ${op.messages}"
    }
    assertions.invoke(op)
    return op
  }

  //
  fun CommandLineProcessor.option(key: Any, value: Any?): PluginOption {
    return PluginOption(pluginId, key.toString(), value.toString())
  }

  //
  private fun prepareCompilation(vararg sourceFiles: SourceFile): KotlinCompilation {
    val registrar = registrar()
    val processor = cliProcessor()

    return KotlinCompilation().apply {
      workingDir = tempDir.absoluteFile
      compilerPlugins = listOf(registrar)
      commandLineProcessors = listOf(processor)
      pluginOptions = defaultOptions(processor)
      inheritClassPath = true
      sources = sourceFiles.asList() + extraSources()
      verbose = false
      jvmTarget = JvmTarget.fromString(System.getenv()["ci_java_version"] ?: "1.8")!!.description
      // https://github.com/tschuchortdev/kotlin-compile-testing/issues/302
      // kotlincArguments = listOf("-Xuse-k2")
    }
  }

  /**
   *
   */
  protected open fun compile(vararg sourceFiles: SourceFile): KotlinCompilation.Result {
    return prepareCompilation(*sourceFiles).compile()
  }

  /**
   *
   */
  abstract fun defaultOptions(processor: CommandLineProcessor): List<PluginOption>

  /**
   *
   */
  abstract fun registrar(): ComponentRegistrar

  /**
   *
   */
  abstract fun cliProcessor(): CommandLineProcessor

  /**
   *
   */
  abstract fun extraSources(): List<SourceFile>
}
