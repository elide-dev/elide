package elide.runtime.plugins.bindings

import elide.runtime.core.DelicateElideApi

/** Simple alias over an untyped map, representing free-form intrinsic language bindings. */
@DelicateElideApi public typealias BindingsRegistrar = MutableMap<String, Any>
