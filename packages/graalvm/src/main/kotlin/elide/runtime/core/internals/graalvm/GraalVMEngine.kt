/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.core.internals.graalvm

import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.Platform
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Context.Builder
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.EnvironmentAccess
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.Proxy
import java.io.InputStream
import java.io.OutputStream
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.io.path.Path
import kotlin.math.max
import kotlin.math.min
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.core.*
import elide.runtime.core.EngineLifecycleEvent.*
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.*
import elide.runtime.core.extensions.disableOption
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.internals.MutableEngineLifecycle
import elide.runtime.core.internals.graalvm.GraalVMEngine.Companion.create
import elide.runtime.core.internals.graalvm.GraalVMRuntime.Companion.GVM_23
import elide.runtime.core.internals.graalvm.GraalVMRuntime.Companion.GVM_23_1
import org.graalvm.polyglot.HostAccess as PolyglotHostAccess

/**
 * A [PolyglotEngine] implementation built around a GraalVM [engine]. This class allows plugins to configure the
 * [Context] builders before they are returned by the [acquire] method.
 *
 * @see acquire
 * @see create
 */
@DelicateElideApi internal class GraalVMEngine private constructor(
  private val lifecycle: MutableEngineLifecycle,
  private val config: GraalVMConfiguration,
  private val engine: Engine,
) : PolyglotEngine {

  /** Select an [EnvironmentAccess] configuration from the [HostAccess] settings for this engine. */
  private fun HostAccess.toEnvAccess(): EnvironmentAccess? = when (this) {
    ALLOW_ENV, ALLOW_ALL -> EnvironmentAccess.INHERIT
    ALLOW_IO, ALLOW_NONE -> EnvironmentAccess.NONE
  }

  /**
   * Returns the underlying GraalVM [Engine] used by this instance.
   *
   * This method is considered delicate even for internal use within the Elide runtime, since it breaks the
   * encapsulation provided by the core API; it should be used only in select cases where accessing the GraalVM engine
   * directly is less complex than implementing a new abstraction for the desired feature.
   */
  override fun unwrap(): Engine {
    return engine
  }

  /** Create a new [GraalVMContext], triggering lifecycle events to allow customization. */
  private fun createContext(cfg: Builder.() -> Unit, finalizer: Builder.() -> Context = { build() }): GraalVMContext {
    val contextHostAccess = PolyglotHostAccess.newBuilder(PolyglotHostAccess.ALL)
      .allowImplementations(Proxy::class.java)
      .allowAccessAnnotatedBy(PolyglotHostAccess.Export::class.java)
      .allowArrayAccess(true)
      .allowBufferAccess(true)
      .allowAccessInheritance(true)
      .allowIterableAccess(true)
      .allowIteratorAccess(true)
      .allowListAccess(true)
      .allowMapAccess(true)
      .build()

    // build a new context using the shared engine
    val builder = Context.newBuilder()
      .allowExperimentalOptions(true)
      .allowEnvironmentAccess(config.hostAccess.toEnvAccess())
      .allowPolyglotAccess(PolyglotAccess.ALL)
      .allowValueSharing(true)
      .allowHostAccess(contextHostAccess)
      .allowInnerContextOptions(true)
      .allowCreateThread(true)
      .allowCreateProcess(true)
      .allowHostClassLoading(true)
      .allowNativeAccess(true)
      .engine(engine)

    // allow plugins to customize the context on creation
    lifecycle.emit(ContextCreated, builder)

    // mount application entrypoint arguments for each language
    config.arguments.let {
      if (it.isNotEmpty()) {
        config.languages.forEach { lang ->
          builder.arguments(lang.languageId, it)
        }
      }
    }

    // build the context, notify event listeners, and finalize
    return GraalVMContext(finalizer.invoke(builder.apply { cfg.invoke(builder) })).also { ctx->
      lifecycle.emit(ContextInitialized, ctx)
    }.finalizeContext()
  }

  // Finalize a suite of bindings for a given language (or the main polyglot bindings).
  private fun finalizeBindings(bindings: Value) {
    if (bindings.hasMembers()) {
      bindings.memberKeys.forEach {
        if (knownInternalMembers.contains(it)) {
          try {
            bindings.removeMember(it)
          } catch (uoe: UnsupportedOperationException) {
            // ignore
          }
        }
      }
    }
  }

  // Finalize a context before execution of guest code.
  private fun doFinalize(ctx: GraalVMContext) = ctx.apply {
    if (EXPERIMENTAL_DROP_INTERNALS && shouldDropInternals) {
      val polyglot = context.polyglotBindings
      finalizeBindings(polyglot)
      config.languages.forEach { lang ->
        val bindings = context.getBindings(lang.languageId)
        finalizeBindings(bindings)
      }
    }
  }

  // Prepare a context for final use by guest code; this involves revoking access to primordials, etc.
  private fun GraalVMContext.finalizeContext(): GraalVMContext = apply {
    try {
      context.enter()

      // emit context finalization
      lifecycle.emit(ContextFinalized, this)
      doFinalize(this)
    } finally {
      context.leave()
    }
  }

  override fun acquire(cfg: Builder.() -> Unit): PolyglotContext {
    return createContext(cfg)
  }

  internal companion object {
    @JvmStatic private val defaultAuxPath = System.getProperty("elide.natives")

    // Names of known-internal members which are yanked before guest code is executed.
    private val knownInternalMembers = sortedSetOf(
      "primordials",
    )

    /** Stubbed output stream. */
    private object StubbedOutputStream : OutputStream() {
      override fun write(b: Int): Unit = error("Cannot write to stubbed stream from inside a guest VM.")
    }

    /** Stubbed input stream. */
    private object StubbedInputStream : InputStream() {
      override fun read(): Int = error("Cannot read from stubbed stream from inside a guest VM.")
    }

    /** Simple proxy allowing the use of a [Logger] in GraalVM engines. */
    private class EngineLogHandler(private val logger: Logger) : Handler() {
      override fun publish(record: LogRecord?) {
        if (record == null) return
        val fmt = record.message

        when (record.level) {
          // no-op if off
          Level.OFF -> {}

          // FINEST becomes trace
          Level.FINEST -> logger.info(fmt)

          // FINE and FINER become debug
          Level.FINE,
          Level.FINER -> logger.debug(fmt)

          // INFO stays info
          Level.INFO -> logger.info(fmt)

          // WARN becomes warning, ERROR becomes SEVERE
          Level.WARNING -> logger.warn(fmt)
          Level.SEVERE -> logger.error(fmt)
        }
      }

      override fun flush() = Unit
      override fun close() = Unit
    }

    /** Whether to enable VM isolates. */
    private const val ENABLE_ISOLATES = false

    /** Whether to enable the auxiliary cache. */
    private const val ENABLE_AUX_CACHE = false

    /** Whether to drop internals from the polyglot context before finalization completes. */
    private const val EXPERIMENTAL_DROP_INTERNALS = true

    /** Whether internal symbols should be withheld from guest code. */
    private val shouldDropInternals = System.getProperty("elide.internals") != "true"

    /** Whether the runtime is built as a native image. */
    private val isNativeImage = ImageInfo.inImageCode()

    /** Logger used for engine instances */
    private val engineLogger = Logging.named("elide:engine")

    /** Whether the auxiliary cache is actually enabled. */
    private val useAuxCache = (
      ENABLE_AUX_CACHE &&
      isNativeImage &&
      System.getProperty("elide.test") != "true" &&
      System.getProperty("ELIDE_TEST") != "true" &&
      System.getProperty("elide.vm.engine.preinitialize") != "false" &&  // manual killswitch
      !ImageInfo.isSharedLibrary() &&
      !Platform.includedIn(Platform.LINUX_AMD64::class.java) &&  // disabled to prefer G1GC on linux AMD64
      !Platform.includedIn(Platform.WINDOWS::class.java)  // disabled on windows - not supported
    )

    /**
     * Creates a new [GraalVMEngine] using the provided [configuration]. This method triggers the [EngineCreated] event
     * for registered plugins.
     */
    fun create(configuration: GraalVMConfiguration, lifecycle: MutableEngineLifecycle): GraalVMEngine {
      val nativesPath = System.getProperty("elide.natives")?.ifBlank { null } ?: defaultAuxPath
      val languages = configuration.languages.map { it.languageId }.toTypedArray()

      val builder = Engine.newBuilder(*languages).apply {
        useSystemProperties(false)
        allowExperimentalOptions(true)

        // stub streams
        if (System.getProperty("elide.js.vm.enableStreams", "false") != "true") {
          `in`(StubbedInputStream)
          out(StubbedOutputStream)
          err(StubbedOutputStream)
        }

        // assign core log handler
        logHandler(EngineLogHandler(engineLogger))

        // base options enabled for every engine
        enableOptions(
          "engine.BackgroundCompilation",
          "engine.UsePreInitializedContext",
          "engine.Compilation",
          "engine.MultiTier",
          "engine.Splitting",
          "engine.OSR",
        )

        // set number of compiler threads
        option(
          "engine.CompilerThreads",
          min(max(Runtime.getRuntime().availableProcessors() / 4, 8), 2).toString(),
        )

        // jit compile on second root call
        option(
          "engine.Mode",
          "latency",
        )
        option(
          "engine.FirstTierMinInvokeThreshold",
          "2",
        )
        option(
          "engine.LastTierCompilationThreshold",
          "2000",
        )

        configuration.hostRuntime.on(GVM_23.andLower()) {
          enableOptions("engine.InlineAcrossTruffleBoundary")
        }

        configuration.hostRuntime.on(GVM_23.andLower()) {
          enableOptions(
            "engine.Inlining",
            "engine.InlineAcrossTruffleBoundary",
          )
        }

        configuration.hostRuntime.on(GVM_23_1.andHigher()) {
          enableOptions(
            "engine.Inlining",
            "engine.InlineAcrossTruffleBoundary",
            "compiler.Inlining",
            "compiler.EncodedGraphCache",
            "compiler.InlineAcrossTruffleBoundary",
          )

          disableOption("engine.WarnOptionDeprecation")
        }

        // isolate options
        if (ENABLE_ISOLATES) {
          option("engine.UntrustedCodeMitigation", "none")
          option("engine.SpawnIsolate", "js")
          option("engine.MaxIsolateMemory", "2GB")
        }

        if (useAuxCache && nativesPath != null) {
          // if we're running in a native image, enabled the code compile cache
          // option("engine.PreinitializeContexts", "js")
          // option("engine.CacheCompile", "hot")
          option("engine.TraceCache", "true")
          disableOption("engine.CachePreinitializeContext")

          option(
            "engine.Cache",
            Path(nativesPath).resolve("elide-img.bin").toAbsolutePath().toString(),
          )
        }
      }

      // allow plugins to customize the engine builder
      lifecycle.emit(EngineCreated, builder)

      // build the engine
      val engine = GraalVMEngine(lifecycle, configuration, builder.build())

      // one more event for initialization plugins
      lifecycle.emit(EngineInitialized, engine)

      return engine
    }
  }
}
