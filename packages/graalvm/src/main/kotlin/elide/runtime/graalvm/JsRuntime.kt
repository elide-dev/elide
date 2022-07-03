package elide.runtime.graalvm

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import elide.server.util.ServerFlag
import elide.util.Hex
import io.micronaut.caffeine.cache.Cache
import io.micronaut.caffeine.cache.Caffeine
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Factory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.guava.asDeferred
import kotlinx.serialization.json.Json
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import com.google.common.util.concurrent.ListenableFuture as Future


/** JavaScript embedded runtime logic, for use on the JVM. */
@Suppress("MemberVisibilityCanBePrivate")
@Context class JsRuntime {
  companion object {
    // Singleton instance.
    private val singleton = JsRuntime()

    // Hard-coded JS VM options.
    private val baseOptions = listOf(
      StaticProperty("js.v8-compat", "true"),
    )

    // Options which can be controlled via user-configured inputs.
    private val configurableOptions = listOf(
      RuntimeProperty("vm.js.ecma", "js.ecmascript-version", "2020"),
    )

    // Options which must be evaluated at the time a context is created.
    private val conditionalOptions = listOf(
      ConditionalProperty(
        "vm.inspect", "inspect", { ServerFlag.inspect }, defaultValue = "false"),
    )

    /** Runnable task within a JS VM context. */
    private class VMExecution<R> constructor (
      val op: (VMContext) -> R,
      val result: AtomicReference<R?> = AtomicReference(null),
    ): Runnable, Callable<R> {
      override fun run() {
        result.set(
          ManagedContext.acquire().exec(op)
        )
      }

      override fun call(): R? {
        result.set(
          ManagedContext.acquire().exec(op)
        )
        return result.get()
      }
    }

    /** Dedicated executor for the JS Runtime. */
    class RuntimeExecutor {
      // Dedicated thread executor backing the runtime.
      private val threadPool: Executor = Executors.newSingleThreadExecutor()

      internal fun <R> submit(runnable: (VMContext) -> R): Future<R> {
        val task = VMExecution(runnable)
        return Futures.submit<R>(task) {
          threadPool.execute(it)
        }
      }
    }

    /** @return Static acquisition of the singleton JavaScript runtime. */
    @JvmStatic fun acquire(): JsRuntime = singleton

    /** @return Set of options to apply to a new JS VM context. */
    @JvmStatic private fun buildRuntimeOptions(): Map<JSVMProperty, String?> {
      return baseOptions.plus(
        configurableOptions
      ).plus(
        conditionalOptions
      ).map {
        it to it.value()
      }.filter {
        it.second?.isNotBlank() ?: false
      }.toMap()
    }

    /** @return SDK VM context pre-built for JavaScript execution. */
    @JvmStatic @Factory private fun spawnContext(): VMContext {
      val logging = LoggerFactory.getLogger(JsRuntime::class.java)
      val builder = VMContext.newBuilder("js")
        .allowExperimentalOptions(true)

      buildRuntimeOptions().forEach {
        val prop = it.key
        val value = prop.value()
        if (value != null && value != "false") {
          logging.debug(
            "Setting JS VM property: '$prop': '$value'"
          )
          builder.option(
            prop.symbol,
            value
          )
          // special case: handle inspection
        }
      }
      return builder.build()
    }
  }

  /** Abstract base interface for a JS VM property. */
  internal sealed interface JSVMProperty {
    /** Symbol to use for this property with the JS VM. */
    val symbol: String

    /** @return Resolved value for this property. */
    fun value(): String?
  }

  /**
   * Represents a hard-coded JS Runtime property.
   *
   * @param symbol Symbol to use for the VM property when passing it to a new context.
   * @param staticValue Value for this property.
   */
  data class StaticProperty(
    override val symbol: String,
    val staticValue: String,
  ): JSVMProperty {
    override fun value(): String = staticValue
  }

  /**
   * Represents a user-configurable JS Runtime property; binds a JS VM property to an Elide configuration property.
   *
   * @param name Name of the property within Elide's configuration system.
   * @param symbol Symbol to use for the VM property when passing it to a new context.
   * @param defaultValue If no configured value is available, this value should be passed instead. If null, pass no
   *   value at all.
   */
  internal data class RuntimeProperty(
    private val name: String,
    override val symbol: String,
    private val defaultValue: String? = null,
  ): JSVMProperty {
    // @TODO(sgammon): implement
    override fun value(): String? = defaultValue
  }

  /**
   * Represents a property for the JS Runtime which applies based on some [condition], or falls back to a [defaultValue]
   * at a given [name] in Elide's configuration system.
   *
   * @param name Name of the property within Elide's configuration system.
   * @param symbol Symbol to use for the VM property when passing it to a new context.
   * @param condition Function to execute to determine whether this property should be applied.
   * @param value Runtime value bound to this property, if applicable; otherwise, just pass a [defaultValue].
   * @param defaultValue If the value is disabled, this value should be passed instead. If null, pass no value at all.
   */
  internal data class ConditionalProperty(
    private val name: String,
    override val symbol: String,
    private val condition: () -> Boolean,
    private val value: RuntimeProperty? = null,
    private val defaultValue: String? = null,
  ): JSVMProperty {
    override fun value(): String? = if (condition.invoke()) {
      value?.value() ?: defaultValue
    } else {
      defaultValue ?: "true"
    }
  }

  /** Shortcuts for creating script descriptors. */
  @Suppress("unused") object Script {
    /** @return Embedded script container for the provided [path] (and [charset], defaulting to `UTF-8`). */
    @JvmStatic fun embedded(path: String, charset: Charset = StandardCharsets.UTF_8): EmbeddedScript = EmbeddedScript(
      path = path,
      charset = charset,
    )

    /** @return Literal script container for the provided [script]. */
    @JvmStatic fun literal(script: String, id: String): ExecutableScript = LiteralScript(id, script)
  }

  /** Managed GraalVM execution context, with thread guards. */
  private class ManagedContext {
    companion object {
      @JvmStatic fun acquire(): ManagedContext {
        return context.get()
      }

      private val context: ThreadLocal<ManagedContext> = object: ThreadLocal<ManagedContext>() {
        override fun initialValue(): ManagedContext {
          val ctx = ManagedContext()
          ctx.initialize()
          return ctx
        }
      }
    }

    private val initialized: AtomicBoolean = AtomicBoolean(false)
    private val locked: AtomicBoolean = AtomicBoolean(false)
    private val vmContext: AtomicReference<VMContext?> = AtomicReference(null)

    fun initialize() {
      initialized.compareAndSet(
        false,
        true,
      )
      vmContext.compareAndSet(
        null,
        spawnContext(),
      )
    }

    // Acquire this context for execution.
    fun <R> exec(operation: (VMContext) -> R): R {
      locked.compareAndSet(
        false,
        true,
      )
      val ctx = vmContext.get() ?: throw IllegalStateException(
        "Context not initialized, cannot execute on VM"
      )
      ctx.enter()
      val result = operation.invoke(ctx)
      ctx.leave()
      locked.compareAndSet(
        true,
        false,
      )
      return result
    }
  }

  /** Script runtime manager. */
  class ScriptRuntime {
    companion object {
      private const val embeddedRoot = "embedded"
      private const val manifest = "/$embeddedRoot/runtime/runtime-js.json"
      private val initialized: AtomicBoolean = AtomicBoolean(false)

      // Runtime pre-amble from which to clone and splice executable scripts.
      private var preamble: StringBuilder

      // Runtime finalizer / loader function.
      private var loader: StringBuilder

      init {
        val (p, l) = initialize()
        preamble = p
        loader = l
      }

      // Load a JS artifact for runtime use from the JAR.
      @JvmStatic private fun loadArtifact(path: String): String {
        return (
          JsRuntime::class.java.getResourceAsStream("/$embeddedRoot/runtime/$path") ?:
            throw FileNotFoundException("Unable to locate runtime JS resource $path")
        ).bufferedReader(StandardCharsets.UTF_8).use {
          it.readText()
        }
      }

      // Initialize the script runtime by loading artifacts mentioned in the manifest.
      @JvmStatic @Synchronized private fun initialize(): Pair<StringBuilder, StringBuilder> {
        if (!initialized.get()) {
          initialized.compareAndSet(
            false,
            true
          )

          // resolve the stream
          val manifestStream = ScriptRuntime::class.java.getResourceAsStream(
            manifest
          ) ?: throw IllegalStateException(
            "Failed to resolve JS runtime manifest: '$manifest'. Please check that you have a dependency on " +
            "'graalvm-js', which is required to run embedded SSR scripts."
          )

          // grab content
          val manifestContent = manifestStream.bufferedReader().use { it.readText() }

          // deserialize as script runtime config
          val config = Json.decodeFromString(
            JsRuntimeConfig.serializer(),
            manifestContent
          )

          // load each resource
          val runtimePreamble = StringBuilder()
          config.artifacts.map {
            runtimePreamble.appendLine(loadArtifact(it.name))
          }

          // load entrypoint
          val runtimeEntry = StringBuilder()
          if (config.entry.isNotBlank()) {
            runtimeEntry.appendLine(loadArtifact(config.entry))
          }

          return (
            runtimePreamble to runtimeEntry
          )
        } else {
          throw IllegalStateException(
            "Cannot initialize JS runtime twice"
          )
        }
      }
    }

    // Thread-pool executor where we should acquire execution contexts.
    internal val executor: RuntimeExecutor by lazy {
      RuntimeExecutor()
    }

    // Private cache of warmed sources.
    private val sourceCache: Cache<ScriptID, Value> = Caffeine.newBuilder()
      .executor(MoreExecutors.directExecutor())
      .build()

    /** @return Executable [script] prepared with an entrypoint and runtime glue code. */
    private fun prepare(script: ExecutableScript): ExecutableScript {
      val content = script.load()
      val container = StringBuilder()
      if (script.installShims) {
        container.append(
          preamble
        )
      }
      container.append(
        content
      )
      if (script.installEntry) {
        container.append(
          loader
        )
      }
      script.assignRendered(
        container
      )
      return script
    }

    /** @return Interpreted and warmed [script] -- in re-used form, or on the fly, as applicable. */
    internal fun resolve(script: ExecutableScript): Value {
      return sourceCache.get(script.fingerprint()) { _ ->
        val prepped = prepare(script)
        ManagedContext.acquire().exec {
          it.eval(prepped.interpret())
        } ?: throw IllegalStateException(
          "Failed to resolve value from VM execution: got `null`"
        )
      }!!
    }
  }

  /** Embedded script descriptor object. */
  sealed class ExecutableScript(
    internal val installShims: Boolean = true,
    internal val installEntry: Boolean = true,
    val invocationBase: String? = null,
    val invocationTarget: String? = null,
    private val fingerprint: ScriptID,
  ) {
    private var renderedContent: StringBuilder? = null
    private var interpreted: AtomicReference<Source> = AtomicReference(null)

    /** @return The path or some module ID for the embedded script. */
    abstract fun getId(): String

    /** @return Script content, loaded synchronously. */
    abstract fun load(): String

    // Assign rendered preamble+script content before execution.
    internal fun assignRendered(builder: StringBuilder) {
      renderedContent = builder
    }

    // Assign VM-interpreted source object.
    private fun assignSource(source: Source) {
      interpreted.compareAndSet(
        null,
        source,
      )
    }

    // Evaluate/interpret the rendered output for this script.
    private fun render(): Source {
      val content = renderedContent?.toString() ?: throw IllegalStateException(
        "Cannot render script before it has been prepared by the JS runtime"
      )
      val source = Source.create(
        "js",
        content
      )
      assignSource(
        source
      )
      return source
    }

    // Unique and stable ID for this script.
    internal fun fingerprint(): ScriptID {
      return fingerprint
    }

    // Acquire VM-interpreted source object.
    internal fun interpret(): Source {
      return interpreted.get() ?: render()
    }
  }

  /** Embedded script implementation which pulls from local JAR resources. */
  class EmbeddedScript(
    val path: String,
    private val charset: Charset = StandardCharsets.UTF_8,
    invocationBase: String? = null,
    invocationTarget: String? = null,
  ): ExecutableScript(
    invocationBase = invocationBase,
    invocationTarget = invocationTarget,
    fingerprint = fingerprintScriptPath(path),
  ) {
    companion object {
      private const val hashAlgo = "SHA-256"

      // Hash an embedded script path to determine a stable fingerprint value.
      @JvmStatic private fun fingerprintScriptPath(path: String): ScriptID {
        val hasher = MessageDigest.getInstance(hashAlgo)
        hasher.update(path.toByteArray(StandardCharsets.UTF_8))
        return String(
          hasher.digest(),
          StandardCharsets.UTF_8
        )
      }
    }

    /** @inheritDoc */
    override fun getId(): String = path

    /** @inheritDoc */
    override fun load(): String {
      val stream = javaClass.getResourceAsStream(path) ?:
      throw FileNotFoundException("Embedded script not found: '$path'")

      return stream.bufferedReader(charset).use {
        it.readText()
      }
    }
  }

  /** Embedded script implementation which pulls from a string literal. */
  class LiteralScript(
    private val moduleId: String,
    private val script: String
  ): ExecutableScript(
    fingerprint = fingerprintScriptContent(moduleId, script)
  ) {
    companion object {
      private const val hashAlgorithm = "SHA-256"

      // Hash a script to determine a stable fingerprint value.
      @JvmStatic private fun fingerprintScriptContent(moduleId: String, content: String): ScriptID {
        val hasher = MessageDigest.getInstance(hashAlgorithm)
        hasher.update(
          moduleId.toByteArray(StandardCharsets.UTF_8)
        )
        hasher.update(
          content.toByteArray(StandardCharsets.UTF_8)
        )
        return String(Hex.encode(
          hasher.digest()
        ), StandardCharsets.UTF_8)
      }
    }

    /** @inheritDoc */
    override fun getId(): String = moduleId

    /** @inheritDoc */
    override fun load(): String {
      return script
    }
  }

  // Create the singleton script runtime.
  private val runtime: ScriptRuntime = ScriptRuntime()

  @VisibleForTesting
  internal fun <R> evalExecuteScript(
    script: ExecutableScript,
    returnType: Class<R>,
    vararg arguments: Any?
  ): R? {
    val interpreted = runtime.resolve(script)
    val base = script.invocationBase
    val target = script.invocationTarget
    val baseTarget: Value = if (target != null) {
      var baseSegment: Value = interpreted
      val baseResolved = if (base != null) {
        base.split(".").forEach {
          baseSegment = baseSegment.getMember(
            it
          ) ?: throw IllegalStateException(
            "Failed to resolve base segment: '$it' in '$base' was not found"
          )
        }
        baseSegment
      } else {
        interpreted
      }

      // from the resolved base segment, pluck the executable member
      val found = baseResolved.getMember(
        script.invocationTarget,
      ) ?: throw IllegalStateException(
        "Failed to invoke script member: '${script.getId()}' (fn: '${script.invocationTarget}')"
      )

      if (!found.canExecute()) {
        throw IllegalStateException(
          "Member found, but not executable, at '${base}.${script.invocationTarget}'"
        )
      } else {
        found
      }
    } else {
      // execute the script directly
      interpreted
    }

    // if we are handed back an executable, execute it, providing the input arguments. in both cases, cast the return
    // value to the expected type.
    return if (baseTarget.canExecute()) {
      baseTarget.execute(
        *arguments
      )?.`as`(
        returnType
      )
    } else {
      interpreted.`as`(
        returnType
      )
    }
  }

  /**
   * Asynchronously execute the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  private fun <R> executeBackground(
    script: ExecutableScript,
    returnType: Class<R>,
    arguments: Array<out Any?>,
  ): Future<R?> {
    // interpret the script
    return runtime.executor.submit {
      evalExecuteScript(
        script,
        returnType,
        *arguments
      )
    }
  }

  /**
   * Asynchronously execute the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  fun <R> executeAsync(script: ExecutableScript, returnType: Class<R>, vararg arguments: Any?): Deferred<R?> {
    // interpret the script
    return executeBackground(
      script,
      returnType,
      arguments,
    ).asDeferred()
  }

  /**
   * Suspension execution of the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  suspend fun <R> execute(script: ExecutableScript, returnType: Class<R>, vararg arguments: Any?): R? {
    // interpret the script
    return executeAsync(
      script,
      returnType,
      arguments,
    ).await()
  }

  /**
   * Blocking execution of the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  fun <R> executeBlocking(script: ExecutableScript, returnType: Class<R>, vararg arguments: Any?): R? {
    // interpret the script
    return executeBackground(
      script,
      returnType,
      arguments,
    ).get()
  }
}
