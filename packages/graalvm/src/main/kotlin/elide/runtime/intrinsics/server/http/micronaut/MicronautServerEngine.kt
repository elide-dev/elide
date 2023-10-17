package elide.runtime.intrinsics.server.http.micronaut

import io.micronaut.runtime.Micronaut
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpServerConfig
import elide.runtime.intrinsics.server.http.HttpServerEngine
import elide.vm.annotations.Polyglot

@DelicateElideApi internal class MicronautServerEngine(
  @Polyglot override val config: HttpServerConfig,
  @Polyglot override val router: MicronautGuestRouter,
) : HttpServerEngine {
  /** Thread-safe flag to signal  */
  private val serverRunning = AtomicBoolean(false)
  
  /** Private logger instance. */
  private val logging by lazy { Logging.of(MicronautServerEngine::class) }
  
  @get:Polyglot override val running: Boolean get() = serverRunning.get()

  @Polyglot override fun start() {
    logging.debug("Starting server")
    
    // allow this call only once
    if (!serverRunning.compareAndSet(false, true)) {
      logging.debug("Server already running, ignoring start() call")
      return
    }
    
    Micronaut.build()
      .singletons(router)
      .properties(mapOf("elide.embedded" to "true"))
      .start()
  }
}