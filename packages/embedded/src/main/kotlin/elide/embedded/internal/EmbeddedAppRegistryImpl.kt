package elide.embedded.internal

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import elide.annotations.Singleton
import elide.embedded.*

/**
 * An [EmbeddedAppRegistry] using structured concurrency to create a tree of guest apps which will safely stop once the
 * registry is [cancelled][cancel].
 */
@Singleton internal class EmbeddedAppRegistryImpl(private val config: EmbeddedConfiguration) : EmbeddedAppRegistry {
  /** A [CoroutineScope] in which application state transitions are executed. */
  private val context = Dispatchers.IO + SupervisorJob()

  /** A concurrency-safe map holding the app records for the registry. */
  private val records = ConcurrentHashMap<EmbeddedAppId, EmbeddedAppImpl>()

  private fun validateAppConfiguration(appConfig: EmbeddedAppConfiguration) {
    check(appConfig.language in config.guestLanguages) {
      "The requested language (${appConfig.language}) is not enabled. Enabled languages: ${config.guestLanguages}"
    }
  }

  override fun register(id: EmbeddedAppId, config: EmbeddedAppConfiguration): EmbeddedApp {
    check(context.isActive) { "Registry is closed, cannot register new applications." }

    val app = records.compute(id) { _, existing ->
      check(existing == null) { "An application with ID '$id' is already registered" }

      validateAppConfiguration(config)
      EmbeddedAppImpl.launch(id, config, context)
    }

    // the result of the computation should never be null
    return app ?: error("Internal error: new application instance is null (using id '$id')")
  }

  override fun remove(id: EmbeddedAppId): Boolean {
    // delete the app from the registry and cancel its scope
    // return true only if both operations were executed
    return records.remove(id)?.cancel() == true
  }

  override fun resolve(id: EmbeddedAppId): EmbeddedApp? {
    return records[id]
  }

  override fun cancel(): Boolean {
    if (!context.isActive) return false

    // cancelling the root context will cascade through all registered apps,
    // causing them to stop first, and then cancel their own coroutine scopes
    context.cancel()
    records.clear()

    return true
  }
}
