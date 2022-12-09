package elide.runtime.graalvm

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import elide.annotations.core.Polyglot
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.ssr.ServerResponse
import elide.server.Application
import elide.server.EMBEDDED_ROOT
import elide.server.NODE_SSR_DEFAULT_PATH
import elide.server.ServerInitializer
import elide.server.annotations.Eager
import elide.server.type.RequestState
import elide.server.util.ServerFlag
import elide.util.Hex
import io.micronaut.caffeine.cache.Cache
import io.micronaut.caffeine.cache.Caffeine
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Factory
import io.micronaut.core.annotation.ReflectiveAccess
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.ImageSingletons
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.EnvironmentAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.UncaughtExceptionHandler
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.LinkedList
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import org.graalvm.polyglot.Context as VMContext

/** JavaScript embedded runtime logic, for use on the JVM. */
@Suppress("MemberVisibilityCanBePrivate")
public class JsRuntime private constructor() {
  public companion object {
    private const val RENDER_ENTRYPOINT = "renderContent"
    private const val STREAM_ENTRYPOINT = "renderStream"
    private const val manifest = "/$EMBEDDED_ROOT/runtime/runtime-js.json"

    // Hard-coded JS VM options.
    private val baseOptions : List<JSVMProperty> = listOf(
      StaticProperty("js.strict", "true"),
      StaticProperty("js.intl-402", "true"),
      StaticProperty("js.annex-b", "true"),
      StaticProperty("js.atomics", "true"),
      StaticProperty("engine.BackgroundCompilation", "true"),
      StaticProperty("engine.PreinitializeContexts", "js"),
      StaticProperty("engine.UsePreInitializedContext", "true"),
      StaticProperty("engine.Compilation", "true"),
      StaticProperty("engine.Inlining", "true"),
      StaticProperty("engine.MultiTier", "true"),
    )

    // Options which can be controlled via user-configured inputs.
    private val configurableOptions : List<JSVMProperty> = listOf(
      RuntimeProperty("vm.js.ecma", "js.ecmascript-version", "2022"),
      RuntimeProperty("vm.js.wasm", "js.webassembly", "false"),
    )

    // Options which must be evaluated at the time a context is created.
    private val conditionalOptions : List<JSVMProperty> = listOf(
      ConditionalMultiProperty(main = ConditionalProperty(
        "vm.inspect",
        "inspect",
        { ServerFlag.inspect },
        defaultValue = "false"
      ), properties = listOf(
        // Inspection: Path.
        RuntimeProperty("vm.inspect.path", "inspect.Path") { ServerFlag.inspectPath },

        // Inspection: Suspend.
        RuntimeProperty("vm.inspect.suspend", "inspect.Suspend", "true"),

        // Inspection: Secure.
        RuntimeProperty("vm.inspect.secure", "inspect.Secure", "false") {
          ServerFlag.inspectSecure.toString()
        },

        // Inspection: Wait for debugger.
        RuntimeProperty("vm.inspect.wait", "inspect.WaitAttached", "false"),

        // Inspection: Runtime sources.
        RuntimeProperty("vm.inspect.internal", "inspect.Internal", "false"),
      )),
    )

    // Singleton instance.
    private val singleton = JsRuntime()

    // Shared JS engine.
    private val jsEngine: Engine = Engine.newBuilder()
      .allowExperimentalOptions(true)
      .useSystemProperties(false)
      .err(StubbedOutputStream.SINGLETON)
      .out(StubbedOutputStream.SINGLETON)
      .`in`(StubbedInputStream.SINGLETON)
      .let {
        baseOptions.forEach { opt ->
          it.option(opt.symbol, opt.value())
        }
        it.build()
      }

    /** Stubbed output stream. */
    private class StubbedOutputStream : OutputStream() {
      companion object {
        /** Singleton instance for internal use. */
        internal val SINGLETON = StubbedOutputStream()
      }

      override fun write(b: Int): Unit = error(
        "Cannot write to stubbed stream from inside the JS VM."
      )
    }

    /** Stubbed input stream. */
    private class StubbedInputStream : InputStream() {
      companion object {
        /** Singleton instance for internal use. */
        internal val SINGLETON = StubbedInputStream()
      }

      override fun read(): Int = error(
        "Cannot read from stubbed stream from inside the JS VM."
      )
    }

    /** @return Static acquisition of the singleton JavaScript runtime. */
    @JvmStatic public fun acquire(): JsRuntime = singleton
  }

  /** Factory which wires in the singleton JS runtime as a bean. */
  @Context @Eager public class JsRuntimeProvider : ServerInitializer, Supplier<JsRuntime> {
    @Factory @Singleton override fun get(): JsRuntime = acquire()

    /** @inheritDoc */
    override fun initialize() {
      Application.Initialization.initializeWithServer {
        // pre-warm the context pool
        singleton.runtime.warm()

        if (ImageInfo.inImageBuildtimeCode()) {
          ImageSingletons.add(JsRuntime::class.java, singleton)
        }
      }
    }
  }

  /** Describes inputs to be made available during a VM execution. */
  public class ExecutionInputs<State : Any> public constructor(
    public val data: Map<String, Any?> = ConcurrentSkipListMap(),
  ) {
    public companion object {
      /** Key where shared state is placed in the execution input data map. */
      public const val STATE: String = "_state_"

      /** Key where combined state is placed in the execution input data map. */
      public const val CONTEXT: String = "_ctx_"

      // Shortcut for empty inputs.
      public val EMPTY: ExecutionInputs<Any> = ExecutionInputs()

      /** @return Execution inputs from the provided request state object. */
      @JvmStatic public fun <State : Any> fromRequestState(
        context: RequestState,
        state: State?,
      ): ExecutionInputs<State> {
        return ExecutionInputs(mapOf(
          STATE to state,
          CONTEXT to context,
        ))
      }
    }

    // Build the execution inputs into a set of arguments for the program.
    internal fun buildArguments(): Array<out Any?> {
      return arrayOf(this)
    }

    /**
     * Host access to fetch the current state; if no state is available, `null` is returned.
     *
     * The "state" for an execution is modeled by the developer, via a serializable data class. If state is provided,
     * then it is made available to the JavaScript context.
     *
     * @return Instance of execution state provided at invocation time, or `null`.
     */
    @Suppress("UNCHECKED_CAST")
    @Polyglot
    @ReflectiveAccess
    public fun state(): State? {
      return data[STATE] as? State
    }

    /**
     * Host access to fetch the current context; if no execution context is available, `null` is returned.
     *
     * The "context" is modeled by the [RequestState] class, which provides a consistent structure with guest language
     * accessors for notable context properties, such as the active HTTP request.
     *
     * @return Instance of execution context provided at invocation time, or `null`.
     */
    @Polyglot
    @ReflectiveAccess
    public fun context(): RequestState {
      return data[CONTEXT] as RequestState
    }

    /**
     * Host access to fetch the current request path.
     *
     * @return Current request path.
     */
    @Polyglot
    @ReflectiveAccess
    public fun path(): String {
      return context().path
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
  internal data class StaticProperty(
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
    private val getter: (() -> String?)? = null,
  ): JSVMProperty {
    // @TODO(sgammon): implement
    override fun value(): String? = getter?.invoke() ?: defaultValue
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
    override fun value(): String = if (condition.invoke()) {
      value?.value() ?: "true"
    } else {
      defaultValue ?: "false"
    }
  }

  /**
   * Represents a property for the JS Runtime which applies based on some `condition`, or falls back to a `defaultValue`
   * at a given `name` in Elide's configuration system; this is similar to a [ConditionalProperty], but allows for
   * multiple properties to be efficiently applied based on a single condition.
   *
   * @param main Conditional property which should trigger this set of properties.
   * @param properties Other property configurations which should apply if this one applies.
   */
  internal data class ConditionalMultiProperty(
    private val main: ConditionalProperty,
    private val properties: List<RuntimeProperty>,
  ): JSVMProperty {
    /** @return Main value for this conditional multi-property set. */
    override fun value(): String = main.value()

    /** @return Main property symbol for this conditional multi-property set. */
    override val symbol: String get() = main.symbol

    /** @return Full list of properties that should apply for this set, including the root property. */
    internal fun explode(): List<JSVMProperty> {
      return listOf(
        main
      ).plus(
        properties
      )
    }
  }

  /** Shortcuts for creating script descriptors. */
  @Suppress("unused") public object Script {
    /** @return Embedded script container for the provided [path] (and [charset], defaulting to `UTF-8`). */
    @JvmStatic public fun embedded(
      path: String = NODE_SSR_DEFAULT_PATH,
      charset: Charset = StandardCharsets.UTF_8,
      embeddedRoot: String = EMBEDDED_ROOT,
    ): EmbeddedScript = EmbeddedScript(
      path = "/$embeddedRoot/$path",
      charset = charset,
    )

    /** @return Literal script container for the provided [script]. */
    @JvmStatic public fun literal(
      script: String,
      id: String,
    ): ExecutableScript = LiteralScript(
      id,
      script,
    )
  }

  /** Uncaught error handler. */
  internal inner class VMErrorHandler : UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
      logging.error("Uncaught exception while processing VM script", e)
    }
  }

  /** Script runtime manager. */
  private class ScriptRuntime constructor (
    concurrency: Int,
    private val threadPool: ListeningExecutorService,
    private val logging: Logger,
  ) : ListeningExecutorService by threadPool {
    companion object {
      // Whether the inner script runtime has initialized yet.
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

      // Spawn a fresh VM thread (un-started).
      @JvmStatic internal fun spawn(task: Runnable): VMThread = VMThread(task)

      // Load a JS artifact for runtime use from the JAR.
      @JvmStatic private fun loadArtifact(path: String): String {
        return (
          JsRuntime::class.java.getResourceAsStream("/$EMBEDDED_ROOT/runtime/$path") ?:
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
        } else error(
          "Cannot initialize JS runtime twice"
        )
      }
    }

    /** Resource pool which manages access to a set of JavaScript execution contexts. */
    private inner class JSRuntimeContextPool constructor (private val concurrency: Int) {
      private val mutex = Mutex()
      private val semaphore = Semaphore(permits = concurrency)
      private val resources = LinkedList<VMContext>()
      private val live = AtomicInteger(0)

      /** @return Set of options to apply to a new JS VM context. */
      private fun buildRuntimeOptions(): Map<JSVMProperty, String?> {
        return configurableOptions.plus(
          conditionalOptions
        ).map {
          it to it.value()
        }.filter {
          it.second?.isNotBlank() ?: false
        }.toMap()
      }

      // Resolved static context options (computed at startup).
      private val resolvedContextOptions: List<JSVMProperty> by lazy {
        buildRuntimeOptions().flatMap {
          val prop = it.key
          val value = prop.value()
          if (value != null && value != "false") {
            if (prop is ConditionalMultiProperty) {
              // if it's a multi-property, explode into the individual property values.
              prop.explode()
            } else {
              // otherwise, just consider the single value.
              listOf(prop)
            }
          } else {
            // if there's no value for this property, then we don't need to consider it.
            emptyList()
          }
        }.mapNotNull { prop ->
          val value = prop.value()
          if (value != null && value != "false") {
            prop
          } else {
            null
          }
        }
      }

      /** @return SDK VM context pre-built for JavaScript execution. */
      private fun spawnContext(): VMContext {
        val builder = VMContext.newBuilder("js")
          .engine(jsEngine)
          .allowIO(false)
          .allowExperimentalOptions(true)
          .allowValueSharing(true)
          .allowCreateProcess(false)
          .allowCreateThread(false)
          .allowHostClassLoading(false)
          .allowNativeAccess(false)
          .allowEnvironmentAccess(EnvironmentAccess.NONE)

        resolvedContextOptions.forEach { prop ->
          val value = prop.value()
          if (value != null && value != "false") {
            logging.debug(
              "Setting JS VM property: '$prop': '$value'"
            )
            builder.option(
              prop.symbol,
              value
            )
          }
        }
        live.incrementAndGet()
        return builder.build()
      }

      fun warm() {
        // pre-spawn several contexts
        repeat(concurrency) {
          resources.add(
            warmContext(spawnContext())
          )
        }
      }

      // Run warmup steps for a newly-provisioned context.
      private fun warmContext(context: VMContext): VMContext = withLocked(context) {
        initialize("js")
        this
      }

      // Perform a guarded code block with the provided context.
      inline fun <R> withLocked(context: VMContext, reset: Boolean = true, op: VMContext.() -> R): R {
        return try {
          context.enter()
          val value = op.invoke(context)
          context.leave()
          value
        } finally {
          if (reset) {
            context.resetLimits()
          }
        }
      }

      // Inner suspending invocation method for VM context use.
      suspend operator fun <R> invoke(handler: suspend (VMContext) -> R): R {
        return semaphore.withPermit {
          val borrowed = if (resources.isEmpty() || live.get() < (concurrency * 2)) {
            warmContext(spawnContext())
          } else {
            mutex.withLock {
              resources.removeLast()
            }
          }
          try {
            withLocked(borrowed) {
              handler.invoke(borrowed)
            }
          } finally {
            mutex.withLock {
              resources.add(borrowed)
            }
          }
        }
      }
    }

    /** VM execution thread. */
    private class VMThread constructor(task: Runnable) : Thread(task)

    // Co-routine dispatcher.
    private val dispatcher: CoroutineDispatcher = threadPool.asCoroutineDispatcher()

    // Central managed context pool.
    private val contextPool = JSRuntimeContextPool(concurrency)

    // Private cache of warmed sources.
    private val sourceCache: Cache<ScriptID, Value> = Caffeine.newBuilder()
      .build()

    /** @return Co-routine dispatcher to use for VM executions. */
    fun dispatcher(): CoroutineDispatcher = dispatcher

    /**
     * TBD.
     */
    fun warm() {
      contextPool.warm()
    }

    /**
     * TBD.
     */
    suspend fun <R> acquire(op: suspend (VMContext) -> R): R {
      return contextPool {
        op(it)
      }
    }

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

    // Resolve the invocation target for the given script details.
    private fun resolveEntrypoint(
      interpreted: Value,
      script: ExecutableScript,
      streaming: Boolean,
      base: String? = null,
      target: String? = null,
    ): Value {
      var baseSegment: Value = interpreted
      val baseResolved = if (base != null) {
        base.split(".").forEach {
          baseSegment = baseSegment.getMember(
            it
          ) ?: error(
            "Failed to resolve base segment: '$it' in '$base' was not found"
          )
        }
        baseSegment
      } else {
        interpreted
      }

      return if (target != null) {
        // from the resolved base segment, pluck the executable member
        baseResolved.getMember(
          target,
        ) ?: error(
          "Failed to resolve script member: '${script.getId()}' (fn: '$target')"
        )
      } else {
        val targetName = "(default)"
        if (!interpreted.canExecute()) {
          val (method, entry) = when {
            streaming && interpreted.hasMember(STREAM_ENTRYPOINT) ->
              interpreted.getMember(STREAM_ENTRYPOINT) to STREAM_ENTRYPOINT
            !streaming && interpreted.hasMember(RENDER_ENTRYPOINT) ->
              interpreted.getMember(RENDER_ENTRYPOINT) to RENDER_ENTRYPOINT
            else -> if (streaming && !interpreted.hasMember(STREAM_ENTRYPOINT)) error(
              "Member found, but is for synchronous render (expected streaming), at '$base.$targetName'"
            ) else if (!streaming && !interpreted.hasMember(RENDER_ENTRYPOINT)) error(
              "Member found, but is for streaming (expected non-streaming), at '$base.$targetName'"
            ) else error(
              "Failed to resolve expected invocation target for SSR script at '$base.$targetName'"
            )
          }
          if (!method.canExecute()) error(
            "Member found at target '$entry', but not executable"
          )
          method
        } else {
          // execute the script directly
          interpreted
        }
      }
    }

    /** @return Interpreted and warmed [script] -- in re-used form, or on the fly, as applicable. */
    suspend fun resolve(
      script: ExecutableScript,
      streaming: Boolean = true,
      base: String? = null,
      target: String? = null,
    ): Value {
      val print = script.fingerprint()
      val cached = sourceCache.getIfPresent(print)
      return if (cached != null) {
        cached
      } else {
        val prepped = prepare(script)
        val result = acquire {
          resolveEntrypoint(
            it.eval(prepped.interpret()),
            script,
            streaming,
            base,
            target,
          )
        }
        sourceCache.put(print, result)
        result
      }
    }
  }

  /** Embedded script descriptor object. */
  public sealed class ExecutableScript(
    internal val installShims: Boolean = true,
    internal val installEntry: Boolean = true,
    private val fingerprint: ScriptID,
  ) {
    private var renderedContent: StringBuilder? = null
    private var interpreted: AtomicReference<Source> = AtomicReference(null)

    /** @return Whether the script backing this [EmbeddedScript] is available for execution. */
    internal abstract fun valid(): Boolean

    /** @return The path or some module ID for the embedded script. */
    internal abstract fun getId(): String

    /** @return Script content, loaded synchronously. */
    internal abstract fun load(): String

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
      val content = renderedContent?.toString() ?: error(
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
    internal fun fingerprint(): ScriptID = fingerprint

    // Acquire VM-interpreted source object.
    internal fun interpret(): Source = interpreted.get() ?: render()
  }

  /** Embedded script implementation which pulls from local JAR resources. */
  public class EmbeddedScript(
    public val path: String,
    private val charset: Charset = StandardCharsets.UTF_8,
  ): ExecutableScript(fingerprint = fingerprintScriptPath(path)) {
    public companion object {
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

    /** @return Whether the embedded script exists. */
    override fun valid(): Boolean = javaClass.getResourceAsStream(path) != null
  }

  /** Embedded script implementation which pulls from a string literal. */
  public class LiteralScript(
    private val moduleId: String,
    private val script: String,
  ): ExecutableScript(fingerprint = fingerprintScriptContent(moduleId, script)) {
    public companion object {
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

    /** @inheritDoc */
    override fun valid(): Boolean = true  // always valid (literal)
  }

  // Logging.
  private val logging: Logger = Logging.of(JsRuntime::class.java)

  // Background execution error handler.
  private val errHandler = VMErrorHandler()

  // Number of concurrent VM contexts to maintain.
  private val concurrency = Runtime.getRuntime().availableProcessors()

  // Dedicated thread executor backing the runtime.
  private val threadPool: ListeningExecutorService = MoreExecutors.listeningDecorator(
    Executors.newFixedThreadPool(
      concurrency,
      ThreadFactoryBuilder()
        .setNameFormat("js-runtime-%d")
        .setDaemon(true)
        .setPriority(Thread.NORM_PRIORITY)
        .setUncaughtExceptionHandler(errHandler)
        .setThreadFactory(ScriptRuntime::spawn)
        .build()
    )
  )

  // Create the singleton script runtime.
  private val runtime: ScriptRuntime = ScriptRuntime(concurrency, threadPool, logging)

  // Convert a JS object representing a `ServerResponse` back into a `ServerResponse` after crossing the VM boundary.
  public fun serverResponseFromRaw(value: Value): ServerResponse {
    val check: (String, ((Value) -> Boolean)?, (Value) -> Unit) -> Unit = { name, checker, callable ->
      if (value.hasMember(name)) {
        val target = value.getMember(name)
        if (checker == null || checker.invoke(target)) {
          callable.invoke(target)
        }
      }
    }

    val fin = AtomicBoolean(false)
    val status = AtomicInteger(-1)
    val headers: AtomicReference<Map<String, String>> = AtomicReference(emptyMap())
    val content: AtomicReference<String> = AtomicReference("")
    val hasContent = AtomicBoolean(false)

    check("status", Value::isNumber) {
      status.set(it.asInt())
    }
    check("headers", null) {
      headers.set(it.asHostObject() as Map<String, String>)
    }
    check("hasContent", Value::isBoolean) {
      hasContent.compareAndSet(false, true)
      check("content", Value::isString) {
        content.set(it.asString())
        hasContent.set(true)
      }
    }
    check("fin", Value::isBoolean) {
      fin.set(it.asBoolean())
    }

    return object: ServerResponse {
      override val status: Int get() = status.get()
      override val headers: Map<String, String> get() = headers.get()
      override val content: String get() = content.get()
      override val hasContent: Boolean get() = hasContent.get()
      override val fin: Boolean get() = fin.get()
    }
  }

  /**
   * TBD
   */
  public suspend fun prewarmScript(script: ExecutableScript) {
    runtime.resolve(
      script
    )
  }

  // Suspending submission.
  public suspend fun <R> execSuspend(runnable: CoroutineScope.(VMContext) -> R): R? {
    return withContext(runtime.dispatcher()) {
      runtime.acquire {
        runnable.invoke(this, it)
      }
    }
  }

  // Suspending submission.
  public suspend fun <R> execSuspendAsync(runnable: suspend CoroutineScope.(VMContext) -> R): Deferred<R?> {
    return withContext(runtime.dispatcher()) {
      async {
        runtime.acquire {
          runnable.invoke(this, it)
        }
      }
    }
  }

  /**
   * TBD
   */
  @VisibleForTesting public suspend fun executeStreaming(
    script: ExecutableScript,
    vararg arguments: Any?,
    receiver: (ServerResponse) -> Unit,
  ): Job = execSuspendAsync {
    val logging = LoggerFactory.getLogger(JsRuntime::class.java)

    // resolve the streaming entrypoint for the script -- if no entrypoint is available that yields a promise and takes
    // a content chunk callback, an error is thrown.
    val resolved: Value = runtime.resolve(
      script,
      streaming = true,
    )
    val wrappedReceiver = ProxyExecutable { args ->
      if (args.isEmpty()) {
        logging.warn("Ignoring callback from VM streaming entrypoint with empty arguments")
      } else {
        val chunk = args[0]
        val response = serverResponseFromRaw(chunk)
        receiver.invoke(response)
      }
    }

    // if we are handed back an executable, execute it, providing the input arguments. in this case, we expect a promise
    // to be returned which needs to be awaited. the promise does not return a value. at the conclusion of the promise,
    // the  callback is expected to have been invoked with all chunks of available content.
    //
    // by this time, a final chunk should have been emitted with the server's terminal response status and headers, as
    // applicable, from the runtime (no headers must be provided, they are appended to the response generated by the
    // Elide server layer). if no such condition is true, an error is thrown. if a timeout elapses while executing the
    // VM script, an error is thrown. if the VM script fails to execute, an error is thrown.
    resolved.execute(
      wrappedReceiver,
      *arguments
    )
  }

  /**
   * Asynchronously execute the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  public suspend fun <R> executeAsync(
    script: ExecutableScript,
    returnType: Class<R>,
    vararg arguments: Any?,
  ): Deferred<R?> = execSuspendAsync {
    val resolved: Value = runtime.resolve(
      script,
      streaming = false,
    )
    if (resolved.canExecute()) {
      val result = resolved.execute(*arguments)
      if (result != null && !result.isNull) {
        result.`as`(returnType)
      } else null
    } else error(
      "Cannot execute script entrypoint '$script'"
    )
  }

  /**
   * Suspension execution of the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  public suspend fun <R> execute(script: ExecutableScript, returnType: Class<R>, vararg arguments: Any?): R? {
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
  public fun <R> executeBlocking(script: ExecutableScript, returnType: Class<R>, vararg arguments: Any?): R? {
    // interpret the script
    return runBlocking {
      execute(script, returnType, *arguments)
    }
  }
}
