package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi

/**
 * An implementation of [HandlerRegistry] that keeps a per-thread backing map.
 *
 * Whenever the registry is accessed for the first time on a given thread, the [initializeForThread] function is
 * called to obtain new thread-scoped references provided by guest code.
 *
 * By scoping handler references to the current thread, this registry guarantees that they can safely be used with a
 * simiarily scoped context, avoiding the limitations on concurrency imposed by GraalVM's JavaScript engine and other
 * single-threaded guest languages.
 *
 * @param preInitialized Whether to pre-initialize the backing map for the construction thread.
 */
@DelicateElideApi internal class ThreadLocalHandlerRegistry(
  preInitialized: Boolean = true,
  private val initializeForThread: (ThreadLocalHandlerRegistry) -> Unit
) : HandlerRegistry() {
  /** Backing thread-local map, populated by [initializeForThread]. */
  private val backing: ThreadLocal<HandlerStack> = ThreadLocal()

  init {
    // pre-initialize with an empty list for the construction thread
    // this avoids incorrect re-evaluation of the entrypoint on start
    if (preInitialized) backing.set(mutableListOf())
  }

  /** Shorthand for calling [ThreadLocal.get] on the [backing] list. */
  private inline val backingRegistry: HandlerStack
    get() {
      // value already exists for this thread, return it
      backing.get()?.let { return it }

      // prepare a new empty list
      val map: HandlerStack = mutableListOf()
      backing.set(map)

      // initialize for this thread (populate the list)
      initializeForThread(this)

      return map
    }

  override fun register(handler: GuestHandler): Int {
    backingRegistry.add(handler)
    return backingRegistry.size - 1
  }

  override fun resolve(stage: Int): GuestHandler? {
    return backingRegistry.getOrNull(stage)
  }
}