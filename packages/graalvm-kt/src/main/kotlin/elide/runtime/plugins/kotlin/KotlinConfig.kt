package elide.runtime.plugins.kotlin

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.AbstractLanguageConfig

@DelicateElideApi public class KotlinConfig internal constructor() : AbstractLanguageConfig() {
  /** Apply init-time settings to a new [context]. */
  internal fun applyTo(context: PolyglotContext) {
    // register intrinsics
    applyBindings(context, Kotlin)
  }
}