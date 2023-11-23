/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.plugins.kotlin.shell

import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.JvmScriptEvaluationConfigurationKeys
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleInMemory
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.util.PropertiesCollection
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.plugins.jvm.interop.guestClass

/**
 * A [BasicJvmScriptEvaluator] specialization that a [GuestClassLoader] to define compiled script classes in an embedded
 * context, then loading them and creating new guest instances to execute the scripts using Espresso.
 *
 * This class can be used independently with a script compiler in order to evaluate a basic Kotlin script, or as part
 * of a REPL evaluator to execute code snippets.
 *
 * @see GuestScriptEvaluator
 * @see GuestClassLoader
 */
@DelicateElideApi internal class GuestScriptEvaluator(context: PolyglotContext) : BasicJvmScriptEvaluator() {
  /** A shared [ClassLoader] instance capable of resolving classes of previously compiled scripts. */
  private val sharedClassLoader: GuestClassLoader = GuestClassLoader(context)

  /** Cached reference to the byte[] type in the guest context. */
  private val guestByteArrayClass by context.guestClass("[B")

  /** Cached reference to the object[] type in the guest context. */
  private val guestObjectArrayClass by context.guestClass("[Ljava.lang.Object;")

  override suspend fun invoke(
    compiledScript: CompiledScript,
    scriptEvaluationConfiguration: ScriptEvaluationConfiguration
  ): ResultWithDiagnostics<EvaluationResult> {
    val configuration = scriptEvaluationConfiguration.resolveShared()
    val evaluatedScripts = configuration[ScriptEvaluationConfiguration.jvm.evaluatedScripts]

    // load the guest class from the compiled output
    val scriptClass = loadCompiledScriptClass(compiledScript, sharedClassLoader)

    // if the target script has already been evaluated, return the cached result
    evaluatedScripts?.get(scriptClass)?.asSuccess()?.let { return it }

    // consolidate the script configuration
    val refinedConfiguration = configuration
      .with { compilationConfiguration(compiledScript.compilationConfiguration) }
      .refineBeforeEvaluation(compiledScript)
      .valueOr { return ResultWithDiagnostics.Failure(it.reports) }

    // resolve the arguments for the script constructor and invoke it, obtaining a new instance of the script class;
    // during construction, the script itself is executed by the compiled class, and the result can be used later
    val scriptInstance = scriptClass.invokeMember("getConstructors").getArrayElement(0).invokeMember(
      /* identifier = */ "newInstance",
      /* ...arguments = */ collectConstructorArguments(refinedConfiguration),
    )

    // unlike default implementations, we don't store the actual compiled script class (since it's on the guest side),
    // and the 'instance' returned is actually a guest value, which is transparent to the Kotlin scripting engine
    val resultValue = compiledScript.resultField?.let { (fieldName) ->
      ResultValue.Value(
        name = fieldName,
        value = scriptInstance.getMember(fieldName),
        type = RETURN_TYPE_POLYGLOT_VALUE,
        scriptInstance = scriptInstance,
        scriptClass = null,
      )
    } ?: ResultValue.Unit(Any::class, scriptInstance)

    // wrap the value and store it for use in following evaluations
    val evaluationResult = EvaluationResult(resultValue, refinedConfiguration)
    evaluatedScripts?.put(scriptClass, evaluationResult)

    return ResultWithDiagnostics.Success(evaluationResult)
  }

  /**
   * Load and define (if required) the class for a given [script] using the provided class [loader]. Before loading,
   * the compiled bytecode contained in the script's module will be added to the loader.
   *
   * Note that this function will only work if the [script] is a [KJvmCompiledScript] with a
   * [KJvmCompiledModuleInMemory]. Any other combination will throw an exception. This is due to limitations in our
   * handling of the compiler output.
   *
   * @param script A [KJvmCompiledScript] instance whose compiled class will be resolved.
   * @param loader A dynamic class loader capable of registering new classes from compiled bytecode.
   * @return The compiled script class.
   */
  private fun loadCompiledScriptClass(script: CompiledScript, loader: GuestClassLoader): PolyglotValue {
    // for our use case, only scripts compiled by the JVM backend are supported
    check(script is KJvmCompiledScript) { "Expected a Kotlin/JVM compiled script, received: $script" }

    // we need the module to provide an in-memory map of compiled classes in order to pass them
    // to the guest class loader, any other module type will not work with this strategy
    val module = script.getCompiledModule()
    check(module is KJvmCompiledModuleInMemory) { "Expected KJvmCompiledModuleInMemory, received: $module" }

    // install compiler output into the class loader, map class names and ignore metadata
    for ((name, bytecode) in module.compilerOutputFiles) {
      if (name.startsWith("META-INF")) continue

      // move the bytecode over to the guest side
      val guestBytecode = guestByteArrayClass.newInstance(bytecode.size)
      bytecode.forEachIndexed { index, byte -> guestBytecode.setArrayElement(index.toLong(), byte) }

      loader.defineClass(name.removeSuffix(".class"), guestBytecode)
    }

    // load the compiled script class, 
    return loader.loadClass(script.scriptClassFQName)
  }

  /**
   * Resolve all arguments that should be passed to the constructor of a script class. This includes previously
   * evaluated code snippets and their results, required arguments, implicit receivers, and provided properties.
   *
   * @param configuration Configuration container used to resolve constructor arguments.
   * @return An array of constructor arguments.
   */
  private fun collectConstructorArguments(configuration: ScriptEvaluationConfiguration): Array<Any?> {
    val args = arrayListOf<Any?>()

    configuration[ScriptEvaluationConfiguration.previousSnippets]?.let {
      // convert to a guest object array
      val guestArray = guestObjectArrayClass.newInstance(it.size)
      it.forEachIndexed { index, value -> guestArray.setArrayElement(index.toLong(), value) }

      args.add(guestArray)
    }

    return args.toArray()
  }

  internal companion object {
    /** Marker string used to verify the correct type of a value returned by the evaluator. */
    internal const val RETURN_TYPE_POLYGLOT_VALUE = "elide.runtime.core.PolyglotValue"

    /** A mutable map of script classes and evaluation results which servers to reuse script executions. */
    private val JvmScriptEvaluationConfigurationKeys.evaluatedScripts by PropertiesCollection
      .key<MutableMap<PolyglotValue, EvaluationResult>>(isTransient = true)

    /** Resolve and return a configuration containing a [evaluatedScripts] map, installing it if not present. */
    private fun ScriptEvaluationConfiguration.resolveShared(): ScriptEvaluationConfiguration = with {
      if (
        this[ScriptEvaluationConfiguration.scriptsInstancesSharing] == true &&
        this[ScriptEvaluationConfiguration.jvm.evaluatedScripts] == null
      ) {
        ScriptEvaluationConfiguration.jvm.evaluatedScripts(mutableMapOf())
      }
    }
  }
}
