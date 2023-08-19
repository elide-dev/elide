package elide.runtime.plugins.python

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.AbstractLanguageConfig

@DelicateElideApi public class PythonConfig : AbstractLanguageConfig() {
  /** Apply init-time settings to a new [context]. */
  internal fun applyTo(context: PolyglotContext) {
    // register intrinsics
    applyBindings(context, Python)
  }
}