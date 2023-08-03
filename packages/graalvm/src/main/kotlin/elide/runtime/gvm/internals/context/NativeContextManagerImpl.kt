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

package elide.runtime.gvm.internals.context

import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.cfg.GuestVMConfiguration
import elide.runtime.gvm.internals.VMProperty
import elide.util.RuntimeFlag
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.Platform
import org.graalvm.polyglot.Engine
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream
import kotlin.io.path.Path
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.cfg.GuestVMConfiguration
import elide.runtime.gvm.internals.VMProperty
import elide.util.RuntimeFlag
import org.graalvm.polyglot.Context as VMContext
import elide.runtime.gvm.internals.VMStaticProperty as StaticProperty

/** TBD. */
@Singleton internal class NativeContextManagerImpl(
  config: GuestVMConfiguration
) : ContextManager<VMContext, VMContext.Builder> {
  // Private logger.
  private val logging: Logger = Logging.of(NativeContextManagerImpl::class)

  // Atomic reference to the globally-active Engine.
  private val engine: Engine = buildEngine(config)

  // Whether the engine has initialized.
  private val initialized: AtomicReference<Boolean> = AtomicReference(false)

  // Context factory function.
  private val contextFactory: AtomicReference<(Engine) -> VMContext.Builder> = AtomicReference(null)

  // Context configuration function.
  private val contextConfigure: AtomicReference<(VMContext.Builder) -> VMContext> = AtomicReference(null)

  // Additional properties to apply to created contexts.
  private val additionalProperties: MutableSet<VMProperty> = TreeSet()

  private fun buildEngine(config: GuestVMConfiguration): Engine {
    return Engine.newBuilder(*(config.languages ?: GuestVMConfiguration.DEFAULT_LANGUAGES).toTypedArray()).apply {
      // stub streams
      if (System.getProperty("elide.js.vm.enableStreams", "false") != "true") {
        `in`(StubbedInputStream)
        out(StubbedOutputStream)
        err(StubbedOutputStream)
      }

      // forbid system property overrides
      useSystemProperties(false)

      // allow experimental options
      allowExperimentalOptions(true).let {
        // Apply static engine options.
        staticEngineOptions.fold(it) { builder, property ->
          builder.option(property.symbol, property.value())
        }
      }
    }.build()
  }

  // Allocate a new thread-confined VM execution context.
  private fun allocateContext(builder: ((VMContext.Builder) -> Unit)? = null): VMContext {
    check(initialized.get()) { "Cannot allocate VM context: Engine is not initialized" }
    val fresh = contextFactory.get().invoke(engine())

    // apply properties installed via `configureVM`
    if (additionalProperties.isNotEmpty()) {
      logging.debug("Applying ${additionalProperties.size} additional VM properties")
      additionalProperties.mapNotNull { property ->
        property.value()?.let { property to it }
      }.forEach {
        fresh.option(it.first.symbol, it.second)
      }
    } else {
      logging.trace("No additional VM properties to apply")
    }

    // let the call-level builder have a chance to configure things
    builder?.invoke(fresh)

    // finalize the new context and return
    return contextConfigure.get().invoke(fresh)
  }

  override fun configureVM(props: Stream<VMProperty>) {
    check(!initialized.get()) {
      "Cannot configure VM context properties after initialization"
    }
    props.forEach {
      additionalProperties.add(it)
    }
  }

  override fun installContextFactory(factory: (Engine) -> VMContext.Builder) {
    logging.trace("VM installed context factory")
    contextFactory.set(factory)
  }

  override fun installContextSpawn(factory: (VMContext.Builder) -> VMContext) {
    logging.trace("VM installed context spawn")
    contextConfigure.set(factory)
  }

  override fun activate(start: Boolean) {
    if (!initialized.compareAndSet(false, true)) return
    logging.trace("Activating native VM context manager")

    // TODO(@darvld): configure and start the disruptor
    // ...
  }

  /** @inheritDoc */
  override fun engine(): Engine = engine

  override fun <R> executeAsync(operation: VMContext.() -> R): CompletableFuture<R> {
    TODO("not yet implemented")
  }

  override fun <R> acquire(builder: ((VMContext.Builder) -> Unit)?, operation: VMContext.() -> R): R {
    // safe to call even if already activated
    activate(start = true)

    val ctx = allocateContext(builder)
    try {
      ctx.enter()
      return operation.invoke(ctx)
    } finally {
      ctx.leave()
    }
  }

  /** Stubbed output stream. */
  private object StubbedOutputStream : OutputStream() {
    override fun write(b: Int): Unit = error("Cannot write to stubbed stream from inside the JS VM.")
  }

  /** Stubbed input stream. */
  private object StubbedInputStream : InputStream() {
    override fun read(): Int = error("Cannot read from stubbed stream from inside the JS VM.")
  }

  private companion object {
    // Whether to enable Isolates.
    const val ENABLE_ISOLATES = false

    // Whether to enable the auxiliary cache.
    const val ENABLE_AUX_CACHE = true

    // Flipped if we're building or running a native image.
    private val isNativeImage = ImageInfo.inImageCode()

    // Whether the auxiliary cache is effectively enabled.
    private val auxCache = ENABLE_AUX_CACHE && isNativeImage

    // Static options which are supplied to the engine.
    private val staticEngineOptions = listOfNotNull(
      StaticProperty.active("engine.BackgroundCompilation"),
      StaticProperty.active("engine.UsePreInitializedContext"),
      StaticProperty.active("engine.Compilation"),
      StaticProperty.active("engine.Inlining"),
      StaticProperty.active("engine.MultiTier"),
      StaticProperty.active("engine.Splitting"),
      StaticProperty.active("engine.InlineAcrossTruffleBoundary"),
      StaticProperty.of("engine.PreinitializeContexts", "js"),

      // isolate options
      if (!ENABLE_ISOLATES) null else StaticProperty.inactive("engine.SpawnIsolate"),
      if (!ENABLE_ISOLATES) null else StaticProperty.of("engine.UntrustedCodeMitigation", "none"),
      if (!ENABLE_ISOLATES) null else StaticProperty.of("engine.MaxIsolateMemory", "2GB"),

      // if we're running in a native image, enabled the code compile cache
      if (!auxCache) null else StaticProperty.active("engine.CachePreinitializeContext"),
      if (!auxCache) null else StaticProperty.of("engine.CacheCompile", "hot"),
      if (!auxCache) null else StaticProperty.of(
        "engine.Cache",
        Path("/", "tmp", "elide-${ProcessHandle.current().pid()}.vmcache").toAbsolutePath().toString()
      ),

      // enable debug features if so instructed
      if (!RuntimeFlag.inspectSuspend) null else StaticProperty.active("inspect.Suspend"),
      if (!RuntimeFlag.inspectWait) null else StaticProperty.active("inspect.WaitAttached"),
      if (!RuntimeFlag.inspectInternal) null else StaticProperty.active("inspect.Internal"),

      when {
        RuntimeFlag.inspect && RuntimeFlag.inspectHost.isNotBlank() && RuntimeFlag.inspectPort > 0 -> {
          StaticProperty.of("inspect", "${RuntimeFlag.inspectHost}:${RuntimeFlag.inspectPort}")
        }

        RuntimeFlag.inspect && RuntimeFlag.inspectHost.isNotBlank() -> {
          StaticProperty.of("inspect", "localhost:${RuntimeFlag.inspectPort}:4200")
        }

        RuntimeFlag.inspect && RuntimeFlag.inspectPort > 0 -> {
          StaticProperty.of("inspect", "localhost:${RuntimeFlag.inspectPort}")
        }

        else -> if (!RuntimeFlag.inspect) null else StaticProperty.active("inspect")
      },
    )
  }
}
