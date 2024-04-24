package elide.runtime.plugins.bindings

import elide.runtime.core.DelicateElideApi

/** A component that dynamically resolves language binding installers at configuration time. */
@DelicateElideApi public fun interface BindingsResolver {
  /** Resolve and return a sequence of applicable [BindingsInstaller] implementations. */
  public fun resolveBindings(): Sequence<BindingsInstaller>

  /** An empty resolver implementation which always returns an empty sequence. */
  public object Empty : BindingsResolver {
    override fun resolveBindings(): Sequence<BindingsInstaller> = emptySequence()
  }
}
