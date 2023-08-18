package elide.runtime.core.internals.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.GuestLanguage
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue

/**
 * An implementation of the [PolyglotContext] interface wrapping a GraalVM context.
 */
@DelicateElideApi internal class GraalVMContext(val context: Context) : PolyglotContext {
  override fun bindings(language: GuestLanguage?): PolyglotValue {
    return language?.let { context.getBindings(it.languageId) } ?: context.polyglotBindings
  }

  override fun execute(source: Source): PolyglotValue {
    return context.eval(source)
  }
}
