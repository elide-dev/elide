package elide.runtime.plugins.kotlin.shell

import org.jetbrains.kotlin.cli.common.repl.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptEngineFactory
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223InvocableScriptEngine
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluator
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluatorState
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext

@DelicateElideApi internal class GuestKotlinScriptEngine(
  context: PolyglotContext,
  factory: ScriptEngineFactory,
  baseCompilationConfiguration: ScriptCompilationConfiguration,
  baseEvaluationConfiguration: ScriptEvaluationConfiguration,
) : KotlinJsr223JvmScriptEngineBase(factory), KotlinJsr223InvocableScriptEngine {
  private val jsr223HostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration)

  private val evaluationConfiguration by lazy {
    ScriptEvaluationConfiguration(baseEvaluationConfiguration) {
      hostConfiguration.update { it.withDefaultsFrom(jsr223HostConfiguration) }
    }
  }

  override val replCompiler: ReplCompilerWithoutCheck by lazy {
    val configuration = ScriptCompilationConfiguration(baseCompilationConfiguration) {
      hostConfiguration.update {
        it.withDefaultsFrom(jsr223HostConfiguration)
      }

      repl {
        // Snippet classes should be named uniquely, to avoid classloading clashes
        makeSnippetIdentifier { _, snippetId -> makeDefaultSnippetIdentifier(snippetId) }
      }
    }

    JvmReplCompiler(configuration)
  }

  override val replEvaluator: ReplFullEvaluator by lazy {
    GenericReplCompilingEvaluatorBase(
      compiler = replCompiler,
      evaluator = JvmReplEvaluator(
        baseScriptEvaluationConfiguration = evaluationConfiguration,
        scriptEvaluator = GuestKotlinScriptEvaluator(context),
      ),
    )
  }

  override val invokeWrapper: InvokeWrapper?
    get() = null

  override val backwardInstancesHistory: Sequence<Any>
    get() = getCurrentState(getContext())
      .asState(JvmReplEvaluatorState::class.java)
      .history
      .asReversed()
      .asSequence()
      .map { it.item.second }
      .filterNotNull()

  override val baseClassLoader: ClassLoader
    get() = evaluationConfiguration[ScriptEvaluationConfiguration.jvm.baseClassLoader]!!

  override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> {
    return replEvaluator.createState(lock)
  }
}
