package elide.runtime.plugins.wasm

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.AbstractLanguageConfig

@DelicateElideApi public class WasmConfig internal constructor() : AbstractLanguageConfig() {
  /** Apply init-time settings to a new [context]. */
  internal fun applyTo(context: PolyglotContext) {
    // register intrinsics
    applyBindings(context, Wasm)
  }
}