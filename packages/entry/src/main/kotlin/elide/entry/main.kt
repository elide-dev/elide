@file:Suppress(
    "UNUSED",
    "NOTHING_TO_INLINE",
)

package elide.entry

import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.io.IOAccess
import java.lang.Runnable
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

private val languages = arrayOf(
    "js",
)

private val contextBuilder = Context.newBuilder(*languages)
  .allowIO(IOAccess.ALL)
  .allowEnvironmentAccess(EnvironmentAccess.INHERIT)
  .allowPolyglotAccess(PolyglotAccess.ALL)
  .allowValueSharing(true)
  .allowNativeAccess(true)
  .allowInnerContextOptions(true)
  .allowHostClassLoading(true)
  .allowExperimentalOptions(true)

//

private const val ENGINE_CORE_POOL_SIZE = 1u
private val ENGINE_THREAD_GROUP = ThreadGroup("engine")

private object EngineThreadFactory : ThreadFactory {
    override fun newThread(r: Runnable): Thread = Thread(ENGINE_THREAD_GROUP, r).apply {
        // nothing at this time
    }
}

private val engineThreadpool by lazy {
    Executors.newScheduledThreadPool(ENGINE_CORE_POOL_SIZE.toInt(), EngineThreadFactory)
}

//

class JavaScript (private val ctx: AppContext) {
    fun invoke(args: Array<String>) {
        val script = args.getOrNull(1) ?: error("No script provided")
        val src = Paths.get(script)
        val url = src.toUri()
        val source = Source.newBuilder("js", url.toURL()).build()
        ctx.useGuestContext {
            eval(source).also {
                System.out.println(it.toString())
            }
        }
    }
}

class Hello (private val ctx: AppContext) {
    fun invoke(args: Array<String>) {
      repeat(3) {
        System.out.println("args (${args.size}): ${args.joinToString(" ")}")
      }
      if (args.contains("info")) {
        System.out.println("Binary path: ${Statics.binaryPath}")
        System.out.println("Binary home: ${Statics.binaryHome}")
        System.out.println("Resources path: ${Statics.resourcesPath}")
      }
    }
}

///

// Flags to enable on the engine.
private val enableEngineFlags: List<String> = listOf()

// Whether to enable the auxiliary cache.
private val enableAuxCache = System.getProperty("elide.auxcache") != "false"
private val enableAuxCacheTracing = java.lang.Boolean.getBoolean("elide.auxcache.trace")

// Languages to pre-initialize.
private val preinitLangs = System.getProperty("elide.preinit", "js")

// Flags mapped to values on the engine.
private fun engineFlagValues(forceDisableAuxCache: Boolean = false): Map<String, String> {
    return mapOf(
        "engine.WarnOptionDeprecation" to "false",
        "engine.WarnVirtualThreadSupport" to "false",
    ).plus(
        if (
            !forceDisableAuxCache &&
            enableAuxCache &&
            ImageInfo.inImageCode() &&
            Statics.auxCacheEnabled()
        ) mapOf(
            "engine.PreinitializeContexts" to preinitLangs,
            "engine.Cache" to "/tmp/elide-auxcache-2.bin",
            "engine.TraceCache" to enableAuxCacheTracing.toString(),
        ) else emptyMap()
    )
}

object Statics {
    private var allArgs: Array<String>? = null
    private var initialized = false
    @Volatile private var ctx: AppContext? = null
    @Volatile private var auxCache = true

    const val graalPyVersion = "24.1"

    val binaryPath: Path by lazy {
        if (ImageInfo.inImageCode() && ImageInfo.isExecutable()) {
            Paths.get(ProcessHandle.current().info().command().orElse("labs"))
        } else {
            Paths.get(requireNotNull(System.getProperty("elide.binpath.override")))
        }
    }

    val binaryHome: Path by lazy {
        binaryPath.parent
    }

    val resourcesPath: Path by lazy {
        binaryPath.parent.resolve("resources")
    }

    fun requireInitialized() {
        check(initialized) { "Statics not initialized" }
    }

    fun initialize(args: Array<String>, activeCtx: AppContext) {
        if (initialized == false) {
            initialized = true
            allArgs = args
            ctx = activeCtx
        }
    }

    @Synchronized fun disableEngineAuxiliaryCache() {
        auxCache = false
    }

    @Synchronized fun auxCacheEnabled() = auxCache
}

// Flags to enable on the context.
private val enableContextFlags by lazy {
    listOf(
        // ---- JavaScript ----------------
        "js.allow-eval",
        "js.foreign-object-prototype",
        "js.intl-402",
        "js.strict",
        // Experimental:
        "js.iterator-helpers",
        "js.import-attributes",
        "js.performance",
        "js.lazy-translation",
        "js.json-modules",
        "js.shared-array-buffer",
        "js.error-cause",
        "js.new-set-methods",
        "js.foreign-hash-properties",
        "js.temporal",
        "js.atomics",
        "js.class-fields",
        "js.top-level-await",
        "js.async-context",
        "js.async-iterator-helpers",
        "js.async-stack-traces",
        "js.atomics-wait-async",
        "js.bind-member-functions",
        "js.esm-eval-returns-exports",
        "js.string-lazy-substrings",
        "js.zone-rules-based-time-zones",
        "js.direct-byte-buffer",
        // Enabled for use by polyfills or for experimental features:
        "js.java-package-globals",
        "js.graal-builtin",
        "js.polyglot-evalfile",
        "js.load",
        "js.polyglot-builtin",
        "js.global-property",
        "js.shadow-realm",
        "js.scope-optimization",
        "js.annex-b",
    )
}

// Flags mapped to values on the context.
private val contextFlagValues by lazy {
    mapOf(
        "js.timer-resolution" to "1",
        "js.commonjs-require-cwd" to ".",
        "js.debug-property-name" to "Debug",
        "js.ecmascript-version" to "2024",
        "js.unhandled-rejections" to "throw",
    )
}

private inline fun doSafeOption(name: String, value: String, setter: (String, String) -> Unit) {
    try {
        setter(name, value)
    } catch (iae: IllegalArgumentException) {
        System.out.println("warn: option '${name}' is not valid or unavailable")
    }
}

private inline fun Engine.Builder.safeOption(name: String, value: String) =
    doSafeOption(name, value, this::option)

private inline fun Context.Builder.safeOption(name: String, value: String) =
    doSafeOption(name, value, this::option)

// Configure an engine instance.
private inline fun configureEngine(langs: Array<String>): Engine.Builder {
    Statics.requireInitialized()

    return (if (langs.isEmpty()) Engine.newBuilder() else Engine.newBuilder(*langs))
        .allowExperimentalOptions(true)
        .sandbox(SandboxPolicy.TRUSTED)
        .apply {
            enableEngineFlags.forEach { safeOption(it, "true") }
            engineFlagValues().forEach { safeOption(it.key, it.value) }
        }
}

// Singleton engine.
private inline fun createEngine(langs: Array<String>): Engine = configureEngine(langs).build()

fun createEmptyContextBuilder(
    lang: Array<String> = languages,
    engineGetter: (Array<String>) -> Engine = { _ -> globalEngine },
    engine: Engine? = null,
    attachEngine: Boolean = true,
): Context.Builder =
    contextBuilder
        .apply {
            enableContextFlags.forEach { safeOption(it, "true") }
            contextFlagValues.filter {
                when {
                    it.key.startsWith("js.") -> lang.contains("js")
                    else -> true
                }
            }.forEach {
                safeOption(it.key, it.value)
            }
        }
        .apply {
            if (attachEngine) {
                engine(engine ?: engineGetter.invoke(lang))
            }
        }

// Singleton engine.
val globalEngine by lazy {
    Statics.requireInitialized()
    createEngine(languages)
}

// Singleton context.
val globalContext by lazy {
    Statics.requireInitialized()
    createEmptyContextBuilder(attachEngine = false).build()
}

sealed interface AppContextAPI {
    val engine: (Array<String>) -> Engine
    val context: (Context.Builder) -> Context
}

typealias ContextBuilder = Context.Builder.() -> Unit

class AppContext (
    override val engine: (Array<String>) -> Engine = ::createEngine,
    override val context: (Context.Builder?) -> Context = { _: Context.Builder? -> globalContext },
) : AppContextAPI {
    inline fun <R> useGuestContext(crossinline block: Context.() -> R): R = withGuestContext {
        use { block() }
    }

    inline fun <R> useGuestContext(
        isolatedEngine: Boolean,
        crossinline block: Context.() -> R
    ): R = withGuestContext(isolatedEngine = isolatedEngine) {
        use { block() }
    }

    inline fun <R> useGuestContext(
        crossinline builder: ContextBuilder,
        crossinline block: Context.() -> R,
    ): R = withGuestContext(builder) {
        use { block() }
    }

    inline fun <R> useGuestContext(
        isolatedEngine: Boolean,
        crossinline builder: ContextBuilder,
        crossinline block: Context.() -> R,
    ): R = withGuestContext(builder, isolatedEngine) {
        use { block() }
    }

    inline fun <R> withGuestContext(
        isolatedEngine: Boolean = true,
        crossinline block: Context.() -> R
    ): R = withGuestContext({ /* no-op */ }, isolatedEngine, block)

    inline fun <R> withGuestContext(
        crossinline builder: ContextBuilder,
        isolatedEngine: Boolean = true,
        crossinline block: Context.() -> R,
    ): R {
        val engine = when (isolatedEngine) {
            true -> engine(emptyArray())
            false -> globalEngine
        }
        val ctxBuilder = createEmptyContextBuilder(
            attachEngine = true,
            engine = engine,
        )
        try {
            context.invoke(ctxBuilder.apply(builder)).use { ctx ->
                ctx.enter()

                try {
                    return block.invoke(ctx)
                } finally {
                    ctx.leave()
                }
            }
        } finally {
            engine.close()
        }
    }
}

fun entry(args: Array<String> = emptyArray()): Int {
    System.setProperty(
        "polyglot.engine.WarnVirtualThreadSupport",
        "false",
    )

    val ctx = AppContext()

    Statics.initialize(
        args,
        ctx,
    )

    try {
        if (args.getOrNull(0) == "js") {
            JavaScript(ctx).invoke(args)
        } else {
            Hello(ctx).invoke(args)
        }
        return 0
    } catch (iae: IllegalArgumentException) {
        if (iae.message?.contains("is experimental and must be enabled with") == true) {
            iae.printStackTrace()
            System.out.println("warn: invalid option prevented image persist; message: ${iae.message}")
        } else {
            throw iae
        }
        return 1
    }
}

fun main(args: Array<String>) {
    entry(args)
}
