@file:Suppress("RedundantVisibilityModifier")

package elide.proto.api

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import elide.proto.ElideProtocol

/**
 * TBD.
 */
public object Protocol : ServiceLoader.Provider<ElideProtocol> {
  // Active default protocol implementation.
  private lateinit var activeDefaultProtocol: ElideProtocol

  // Active implementation class.
  private lateinit var activeImplType: Class<out ElideProtocol>

  // All installed protocol implementations.
  private val allProtocols: MutableList<ServiceLoader.Provider<out ElideProtocol>> = ArrayList()

  // Cached access to each instantiated protocol implementation.
  private val implMap: TreeMap<ElideProtocol.ImplementationLibrary, ElideProtocol> = TreeMap()

  // Whether the default protocol has loaded.
  private val initialized = AtomicBoolean(false)

  /**
   * TBD.
   */
  public fun acquire(impl: ElideProtocol.ImplementationLibrary? = null): ElideProtocol {
    if (!initialized.get()) {
      // gather installed protocol implementations (one is required at invocation time)
      allProtocols.addAll(ServiceLoader.load(ElideProtocol::class.java)
        .stream()
        .collect(Collectors.toList()))

      // throw if not available
      if (allProtocols.isEmpty()) error("No installed protocol implementations found.")

      // default is the first installed
      val selected = allProtocols.first()
      activeDefaultProtocol = selected.get()
      activeImplType = selected.type()
      initialized.compareAndSet(false, true)
    }
    if (impl == null)
      return activeDefaultProtocol
    return implMap.getOrPut(impl) {
      allProtocols.firstOrNull { it.get().engine() == impl }
        ?.get()
        ?: error("No installed protocol implementation found for $impl.")
    }
  }

  /** @inheritDoc */
  override fun get(): ElideProtocol = acquire(null)

  /** @inheritDoc */
  override fun type(): Class<out ElideProtocol> = activeImplType
}
