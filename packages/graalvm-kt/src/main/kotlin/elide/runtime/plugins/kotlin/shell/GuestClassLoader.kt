package elide.runtime.plugins.kotlin.shell

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.plugins.jvm.interop.loadGuestClass

@DelicateElideApi @JvmInline internal value class GuestClassLoader(
  private val delegate: PolyglotValue,
) {
  /** Create a new instance by resolving the guest class from a [context]. */
  constructor(context: PolyglotContext) : this(context.loadGuestClass(DYNAMIC_LOADER_CLASS).newInstance())

  fun defineClass(name: String, bytecode: PolyglotValue) {
    delegate.invokeMember("define", name, bytecode)
  }

  fun loadClass(name: String): PolyglotValue {
    return delegate.invokeMember("loadClass", name)
  }

  private companion object {
    /** Fully qualified class name used to resolve the dynamic loader class in a guest context. */
    private const val DYNAMIC_LOADER_CLASS = "elide.runtime.plugins.kotlin.shell.DynamicClassLoader"
  }
}