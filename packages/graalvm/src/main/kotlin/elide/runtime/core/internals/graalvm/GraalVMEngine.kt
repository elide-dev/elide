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

package elide.runtime.core.internals.graalvm

import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.Platform
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.EnvironmentAccess
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.proxy.Proxy
import java.io.InputStream
import java.io.OutputStream
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.io.path.Path
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent
import elide.runtime.core.EngineLifecycleEvent.EngineCreated
import elide.runtime.core.EngineLifecycleEvent.EngineInitialized
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.*
import elide.runtime.core.extensions.disableOption
import elide.runtime.core.extensions.enableOption
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.internals.MutableEngineLifecycle
import elide.runtime.core.internals.graalvm.GraalVMEngine.Companion.create
import elide.runtime.gvm.internals.VMStaticProperty
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
  internal fun unwrap(): Engine {
    return engine
  }

  /** Create a new [GraalVMContext], triggering lifecycle events to allow customization. */
  private fun createContext(): GraalVMContext {
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
      .allowInnerContextOptions(false)
      .allowCreateThread(true)
      .allowCreateProcess(false)
      .allowHostClassLoading(true)
      .allowNativeAccess(true)
      .engine(engine)

    // allow plugins to customize the context on creation
    lifecycle.emit(EngineLifecycleEvent.ContextCreated, builder)

    // build the context and notify event listeners
    val context = GraalVMContext(builder.build())
    lifecycle.emit(EngineLifecycleEvent.ContextInitialized, context)

    return context
  }

  override fun acquire(): PolyglotContext {
    return createContext()
  }

  internal companion object {
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
          "engine.InlineAcrossTruffleBoundary",
          "engine.OSR",
        )

        // TODO(@darvld): swap for throughput in server mode
        option("engine.Mode", "latency")

        // TODO(@darvld): translate this to an API in the core package
        listOfNotNull(
          VMStaticProperty.activeWhenAtMost("23.0", "engine.Inlining"),
          VMStaticProperty.activeWhenAtMost("23.0", "engine.InlineAcrossTruffleBoundary"),
          VMStaticProperty.activeWhenAtLeast("23.1", "compiler.Inlining"),
          VMStaticProperty.activeWhenAtLeast("23.1", "compiler.EncodedGraphCache"),
          VMStaticProperty.activeWhenAtLeast("23.1", "compiler.InlineAcrossTruffleBoundary"),
          VMStaticProperty.inactiveWhenAtLeast("23.1", "engine.WarnOptionDeprecation"),
        ).forEach {
          option(it.symbol, it.value())
        }

        // isolate options
        if (ENABLE_ISOLATES) {
          disableOption("engine.SpawnIsolate")
          option("engine.UntrustedCodeMitigation", "none")
          option("engine.MaxIsolateMemory", "2GB")
        }

        // if we're running in a native image, enabled the code compile cache
        if (useAuxCache) {
          enableOption("engine.CachePreinitializeContext")
          option("engine.PreinitializeContexts", "js")
          option("engine.CacheCompile", "hot")
          option(
            "engine.Cache",
            Path("/", "tmp", "elide-${ProcessHandle.current().pid()}.vmcache").toAbsolutePath().toString(),
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
