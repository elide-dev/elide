package elide.runtime.plugins.kotlin.shell

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.File
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jvm.JvmScriptCompilationConfigurationBuilder
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.jvmhost.createJvmScriptDefinitionFromTemplate
import kotlin.script.templates.standard.SimpleScriptTemplate
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext

@DelicateElideApi internal class GuestKotlinScriptEngineFactory(
  private val context: PolyglotContext
) : ScriptEngineFactory {
  private val scriptDefinition = createJvmScriptDefinitionFromTemplate<SimpleScriptTemplate>()
  private var lastClassLoader: ClassLoader? = null
  private var lastClassPath: List<File>? = null

  override fun getLanguageName(): String = "kotlin"
  override fun getLanguageVersion(): String = KotlinCompilerVersion.VERSION
  override fun getEngineName(): String = "elide-kotlin-espresso"
  override fun getEngineVersion(): String = KotlinCompilerVersion.VERSION
  override fun getExtensions(): List<String> = listOf("kts")
  override fun getMimeTypes(): List<String> = listOf("text/x-kotlin")
  override fun getNames(): List<String> = listOf("elide-kotlin-espresso")

  override fun getOutputStatement(toDisplay: String?): String {
    return "print(\"$toDisplay\")"
  }

  override fun getMethodCallSyntax(obj: String, m: String, vararg args: String): String {
    return "$obj.$m(${args.joinToString()})"
  }

  override fun getProgram(vararg statements: String): String {
    val sep = System.lineSeparator()
    return statements.joinToString(sep) + sep
  }

  override fun getParameter(key: String?): Any? =
    when (key) {
      ScriptEngine.NAME -> engineName
      ScriptEngine.LANGUAGE -> languageName
      ScriptEngine.LANGUAGE_VERSION -> languageVersion
      ScriptEngine.ENGINE -> engineName
      ScriptEngine.ENGINE_VERSION -> engineVersion
      else -> null
    }

  override fun getScriptEngine(): ScriptEngine {
    val configuration = scriptDefinition.compilationConfiguration.with {
      jvm { dependenciesFromCurrentContext() }
    }

    return GuestKotlinScriptEngine(
      context = context,
      factory = this,
      baseCompilationConfiguration = configuration,
      baseEvaluationConfiguration = scriptDefinition.evaluationConfiguration,
    )
  }

  @Synchronized private fun JvmScriptCompilationConfigurationBuilder.dependenciesFromCurrentContext() {
    // resolve the thread-local class loader and update the reference if required
    val currentClassLoader = Thread.currentThread().contextClassLoader
    if (lastClassLoader != null && lastClassLoader == currentClassLoader) return

    // resolve a new classpath with the current loader
    val currentClassPath = scriptCompilationClasspathFromContext(
      classLoader = currentClassLoader,
      wholeClasspath = true,
      unpackJarCollections = true,
    )

    lastClassLoader = currentClassLoader
    lastClassPath = currentClassPath

    updateClasspath(lastClassPath)
  }
}