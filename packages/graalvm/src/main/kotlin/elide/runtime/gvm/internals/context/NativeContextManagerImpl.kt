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

import com.lmax.disruptor.*
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.dsl.ProducerType
import com.lmax.disruptor.util.DaemonThreadFactory
import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.Engine
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream
import kotlin.io.path.Path
import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.cfg.GuestVMConfiguration
import elide.runtime.gvm.internals.VMProperty
import elide.util.RuntimeFlag
import org.graalvm.polyglot.Context as VMContext
import elide.runtime.gvm.internals.VMStaticProperty as StaticProperty

/** TBD. */
@Singleton
internal class NativeContextManagerImpl(
  private val config: GuestVMConfiguration
) : ContextManager<VMContext, VMContext.Builder> {
  private class ContextRequest(var continuation: ((VMContext) -> Unit)? = null)

  private class ContextDispatcher(
    poolSize: Int,
    private val ordinal: Long,
    private val context: VMContext,
  ) : EventHandler<ContextRequest> {
    private val poolSizeMask: Long = poolSize - 1L

    override fun onEvent(event: ContextRequest, sequence: Long, endOfBatch: Boolean) {
      // evenly distribute events between handlers
      // see: https://github.com/LMAX-Exchange/disruptor/wiki/Frequently-Asked-Questions
      // use a bitmask to calculate modulo, assuming power of two pool sizes (4x faster than %)
      if(sequence and poolSizeMask != ordinal) return

      event.continuation?.let { request ->
        request(context)
      }
    }
  }

  /**
   * Work queue used to manage context requests. [ContextRequest] messages are sent by the [acquire] method,
   * and are handled by one of the [ContextDispatcher] instances in the pool.
   *
   * In order for this [Disruptor] to work properly, [activate] must be called before any other operation.
   */
  private val disruptor = Disruptor(
    /*eventFactory=*/ ::ContextRequest,
    /*ringBufferSize=*/ DEFAULT_RING_BUFFER_SIZE,
    /*threadFactory=*/ Thread.ofPlatform().daemon(true).factory(),
    /*producerType=*/ ProducerType.MULTI,
    BusySpinWaitStrategy(),
//    /*waitStrategy=*/when(config.waitStrategy) {
//      "busySpin" -> BusySpinWaitStrategy()
//      "liteBlocking" -> LiteBlockingWaitStrategy()
//      "liteTimeout" -> LiteTimeoutBlockingWaitStrategy(100, TimeUnit.NANOSECONDS)
//      "sleep" -> SleepingWaitStrategy()
//      "timeout" -> TimeoutBlockingWaitStrategy(100, TimeUnit.NANOSECONDS)
//      "yield" -> YieldingWaitStrategy()
//      else -> BlockingWaitStrategy()
//    }
  )

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

    // configure and start the disruptor
    val handlers = Array(config.poolSize ?: DEFAULT_POOL_SIZE) { number ->
      ContextDispatcher(
        poolSize = config.poolSize ?: DEFAULT_POOL_SIZE,
        ordinal = number.toLong(),
        context = allocateContext()
      )
    }

    // configure and start the disruptor
    disruptor.handleEventsWith(*handlers)
//    disruptor.start()
  }

  /** @inheritDoc */
  override fun engine(): Engine = engine

  override fun <R> executeAsync(operation: VMContext.() -> R): CompletableFuture<R> {
    TODO("not yet implemented")
  }

  /** Thread-confined VM executor. */
  private class VMContainer {
    private val local = ThreadLocal<VMContext>()

    fun obtain(allocate: () -> VMContext): VMContext {
      val existing = local.get()
      if (existing == null) {
        val ctx = allocate()
        local.set(ctx)
      }
      return local.get()
    }
  }

  /** VM container. */
  private val vm = VMContainer()

  override fun <R> acquire(builder: ((VMContext.Builder) -> Unit)?, operation: VMContext.() -> R): R {
    // TODO(@darvld): migrate the builder out of this function, the contexts are constructed at init time
    // submit the operation and wait for it to complete
    return vm.obtain {
      allocateContext(builder)
    }.let { context ->
      operation(context)
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
    // Number of VM context instances used to process tasks
    const val DEFAULT_POOL_SIZE = 4

    // Size of the Disruptor's ring buffer
    const val DEFAULT_RING_BUFFER_SIZE = 2048

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
