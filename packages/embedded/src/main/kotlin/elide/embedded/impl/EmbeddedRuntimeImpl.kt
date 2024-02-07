package elide.embedded.impl

import tools.elide.meta.GuestLanguage.*
import java.util.concurrent.ConcurrentSkipListSet
import kotlinx.atomicfu.atomic
import elide.annotations.Singleton
import elide.embedded.api.Capability
import elide.embedded.api.EmbeddedRuntime
import elide.embedded.api.EmbeddedRuntime.EmbeddedDispatcher
import elide.embedded.api.InstanceConfiguration
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.js.JavaScript

@OptIn(DelicateElideApi::class) @Singleton internal class EmbeddedRuntimeImpl(
  private val hostConfig: InstanceConfiguration,
  ) : EmbeddedRuntime {
    /** Concurrency-safe flag for configuration state. */
  private val configured = atomic(false)

  /** Concurrency-safe flag for initialization state. */
  private val initialized = atomic(false)

  /** Concurrency-safe flag for running state. */
  private val running = atomic(false)

  /** Concurrency-safe set of enabled capabilities. */
  private val capabilities = ConcurrentSkipListSet<Capability>()

  /** A reference to the engine used to obtain dispatcher contexts. */
  private val engine = atomic<PolyglotEngine?>(null)

  override val isConfigured: Boolean by configured
  override val isRunning: Boolean by running

  private fun prepareEngine(): PolyglotEngine = PolyglotEngine {
    // TODO(@darvld): configure engine according to enabled capabilities

    // enable support for requested guest languages
    for (language in hostConfig.engine.languageList) when (language) {
      JAVASCRIPT -> install(JavaScript) {
      // nothing to configure
      }

      JVM, WASM, LLVM, PYTHON, RUBY -> error("Guest language is not yet supported: $language")
      NO_GUEST_ENABLED, UNRECOGNIZED, null -> error("Invalid guest language requested: $language")
    }
  }

  override fun initialize() {
    // nothing to do at init-time, the engine is configured during start
    check(initialized.compareAndSet(expect = false, update = true)) {
      "Embedded runtime may not be initialized more than once"
    }
  }

  override fun enable(capability: Capability) {
    check(!isRunning) { "Cannot enable capability $capability after runtime has started" }
    capabilities.add(capability)
  }

  override fun start() {
    check(initialized.compareAndSet(expect = false, update = true)) {
      "Embedded runtime may not be started more than once"
    }

    // create and configure the engine
    engine.value = prepareEngine()
  }

  override fun notify(capabilities: Set<Capability>) {
    // enable all requested capabilities and start
    capabilities.forEach(::enable)
    start()
  }

  override fun dispatcher(): EmbeddedDispatcher {
    // create a new dispatcher for every call
    return EmbeddedDispatcherImpl()
  }
  }
