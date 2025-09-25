/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
@file:Suppress("WildcardImport")

package elide.runtime.core.internals.graalvm

import com.oracle.truffle.js.lang.JavaScriptLanguage
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.Platform
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Context.Builder
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.EnvironmentAccess
import org.graalvm.polyglot.Value
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlinx.io.IOException
import kotlin.io.path.Path
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.core.*
import elide.runtime.core.EngineLifecycleEvent.*
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.*
import elide.runtime.core.internals.MutableEngineLifecycle
import elide.runtime.core.internals.graalvm.GraalVMEngine.Companion.create
import elide.runtime.lang.typescript.TypeScriptLanguage
import elide.runtime.plugins.defaultPolyglotContextBuilder
import elide.runtime.plugins.defaultPolyglotEngine
import elide.runtime.plugins.defaultPolyglotEngineBuilder
import elide.runtime.plugins.initializeDefaultContext

/**
 * A [PolyglotEngine] implementation built around a GraalVM [engine]. This class allows plugins to configure the
 * [Context] builders before they are returned by the [acquire] method.
 *
 * @see acquire
 * @see create
 */
@DelicateElideApi public class GraalVMEngine private constructor(
  private val lifecycle: MutableEngineLifecycle,
  private val config: GraalVMConfiguration,
  private val engine: Engine = defaultPolyglotEngine(),
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
  override fun unwrap(): Engine = engine

  /** Create a new [GraalVMContext], triggering lifecycle events to allow customization. */
  private fun createContext(
    shared: Boolean,
    cfg: Builder.(Engine) -> Unit,
    finalizer: Builder.(Engine) -> Context = { build() },
  ): GraalVMContext {
    // build a new context using the shared engine
    val builder: Builder = (if (shared) {
      defaultPolyglotContextBuilder()
    } else {
      Context.newBuilder()
        .initializeDefaultContext(defaults = true)
    })

    builder.allowEnvironmentAccess(config.hostAccess.toEnvAccess())

    if (shared) {
      builder.engine(engine)
    }

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
    return GraalVMContext(finalizer.invoke(builder.apply { cfg.invoke(builder, engine) }, engine)).also { ctx->
      lifecycle.emit(ContextInitialized, ctx)
    }.finalizeContext()
  }

  // Finalize a suite of bindings for a given language (or the main polyglot bindings).
  @Suppress("KotlinConstantConditions")
  private fun finalizeBindings(bindings: Value) {
    if (EXPERIMENTAL_DROP_INTERNALS && bindings.hasMembers()) {
      knownInternalMembers.forEach {
        try {
          bindings.removeMember(it)
        } catch (_: UnsupportedOperationException) {
          // ignore
        }
      }
    }
  }

  // Finalize a context before execution of guest code.
  @Suppress("KotlinConstantConditions")
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

  override fun acquire(shared: Boolean, cfg: Builder.(Engine) -> Unit): PolyglotContext {
    return createContext(shared, cfg)
  }

  public companion object {
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
    private const val ENABLE_AUX_CACHE = true

    /** Whether to drop internals from the polyglot context before finalization completes. */
    private const val EXPERIMENTAL_DROP_INTERNALS = false

    /** Whether internal symbols should be withheld from guest code. */
    @JvmStatic private val shouldDropInternals = System.getProperty("elide.internals") != "true"

    /** Whether to enable output/input stream access by guest languages (by default). */
    @JvmStatic private val enableStreams = System.getProperty("elide.js.vm.enableStreams", "false")

    /** Logger used for engine instances */
    @JvmStatic private val engineLogger by lazy { Logging.named("elide:engine") }

    /** Whether to emit trace messages for aux cache usage. */
    @JvmStatic private val traceCache = System.getProperty("elide.traceCache", "false") == "true"

    /** Whether to use the auxiliary engine cache. */
    @JvmStatic private val enableAuxCache = System.getProperty("elide.auxCache", "false") == "true"

    /** Whether the auxiliary cache is actually enabled. */
    @JvmStatic private val useAuxCache = (
      ENABLE_AUX_CACHE &&
      enableAuxCache &&
      System.getProperty("elide.test") != "true" &&
      System.getProperty("ELIDE_TEST") != "true" &&
      System.getProperty("elide.vm.engine.preinitialize") != "false" &&  // manual kill-switch
      ImageInfo.inImageCode() &&
      !ImageInfo.isSharedLibrary() &&
      !Platform.includedIn(Platform.WINDOWS::class.java)  // disabled on windows - not supported
    )

    // JIT compilation enabled by default in GraalVM.
    @JvmStatic private val truffleJitEnabled = (
      System.getProperty("truffle.TruffleRuntime") != "com.oracle.truffle.api.impl.DefaultTruffleRuntime"
    )

    /**
     * Creates a new [GraalVMEngine] using the provided [configuration]. This method triggers the [EngineCreated] event
     * for registered plugins.
     */
    @Suppress("SpreadOperator", "LongMethod")
    @JvmStatic public fun create(
      configuration: GraalVMConfiguration,
      lifecycle: MutableEngineLifecycle,
    ): GraalVMEngine {
      val auxCachePath = System.getProperty("elide.cachePath")?.ifBlank { null }

      val languages = configuration.languages.flatMap {
        when (it.languageId) {
          JavaScriptLanguage.ID,
          TypeScriptLanguage.ID -> listOf(
            JavaScriptLanguage.ID,
            TypeScriptLanguage.ID,
            "wasm",  // @TODO better reference
          )

          else -> listOf(it.languageId)
        }
      }.distinct().toTypedArray()

      engineLogger.debug { "Creating GraalVM engine with languages: ${languages.joinToString(", ")}" }
      val builder = defaultPolyglotEngineBuilder().apply {
        // stub streams
        if (enableStreams != "true") {
          `in`(StubbedInputStream)
          out(StubbedOutputStream)
          err(StubbedOutputStream)
        }

        // assign core log handler
        logHandler(EngineLogHandler(engineLogger))

        // jit compile on second root call
        if (truffleJitEnabled) {
          option(
            "engine.FirstTierMinInvokeThreshold",
            "2",
          )
          option(
            "engine.LastTierCompilationThreshold",
            "2000",
          )

          // isolate options
          if (ENABLE_ISOLATES) {
            engineLogger.debug { "Isolates are active (lang: 'js')" }
            option("engine.UntrustedCodeMitigation", "none")
            option("engine.SpawnIsolate", "js")
            option("engine.MaxIsolateMemory", "2GB")
          }
        }

        if (truffleJitEnabled && useAuxCache) {
          var auxCacheReady = true
          var creatingAuxCache = System.getProperty("elide.writeAuxCache") == "true"
          val auxCacheParent: Path =
            auxCachePath?.let { Path(it) }
              // if no path is specified for the cache, put it next to the binary
              ?: ProcessHandle.current().info().command().orElse(null)?.let { Path(it).parent }
              // if we somehow can't resolve the binary, default to /tmp
              ?: Path("/tmp")

          if (!Files.exists(auxCacheParent)) {
            creatingAuxCache = true
            try {
              Files.createDirectories(auxCacheParent)
            } catch (e: IOException) {
              engineLogger.warn { "Failed to create aux cache directory: $auxCachePath (err: '${e.message}')" }
              auxCacheReady = false
            }
          }
          if (!Files.isWritable(auxCacheParent)) {
            engineLogger.warn { "Aux cache directory is not writable: $auxCachePath" }
            auxCacheReady = false
          }
          val auxCacheActual = auxCacheParent.resolve("elide-img.bin")
          if (!Files.exists(auxCacheActual)) {
            creatingAuxCache = true
          }
          if (auxCacheReady) {
            engineLogger.debug { "Aux cache is active (path: '$auxCacheActual')" }
            option("engine.CachePreinitializeContext", "true")
            option("engine.CacheCompile", "hot")

            if (traceCache) {
              engineLogger.debug { "Aux cache tracing is active" }
              option("engine.TraceCache", "true")
            } else {
              engineLogger.debug { "Aux cache tracing is inactive" }
            }

            option(
              if (creatingAuxCache) "engine.Cache" else "engine.CacheLoad",
              auxCacheActual.toAbsolutePath().toString(),
            )
          }
        } else {
          if (!ImageInfo.inImageCode()) {
            engineLogger.debug { "Running on JVM; aux cache is not supported" }
          } else {
            engineLogger.debug { "Aux cache is not enabled" }
          }
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
