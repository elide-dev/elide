package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

@DelicateElideApi internal class ThreadLocalHandlerRegistry(
  private val initializeForThread: (ThreadLocalHandlerRegistry) -> Unit
) : HandlerRegistry {
  /** Backing thread-local map, populated by [initializeForThread]. */
  private val backing: ThreadLocal<HandlerMap> = ThreadLocal()

  init {
    // TODO(@darvld): refactor this to be cleaner
    // pre-initialize with an empty map for the construction thread
    // this avoids incorrect re-evaluation of the entrypoint on start
    backing.set(mutableMapOf())
  }

  /** Shorthand for calling [ThreadLocal.get] on the [backing] map. */
  private inline val backingRegistry: HandlerMap
    get() {
      // map already exists for this thread, return it
      backing.get()?.let { return it }

      // prepare a new empty map
      val map: HandlerMap = mutableMapOf()
      backing.set(map)

      // initialize for this thread (populate the map)
      initializeForThread(this)

      return map
    }

  override fun register(key: String, handler: GuestHandler) {
    backingRegistry[key] = handler
  }

  override fun resolve(key: String): GuestHandler? {
    return backingRegistry[key]
  }
}