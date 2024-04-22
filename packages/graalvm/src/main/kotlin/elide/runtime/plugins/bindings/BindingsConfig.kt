package elide.runtime.plugins.bindings

import elide.runtime.core.DelicateElideApi

/** Configuration for the [Bindings] plugin. */
@DelicateElideApi public class BindingsConfig internal constructor() {
  /**
   * Selects the [BindingsResolver] to be used at configuration time to collect all applicable bindings. Defaults to
   * [BindingsResolver.Empty].
   */
  public var resolver: BindingsResolver = BindingsResolver.Empty
}