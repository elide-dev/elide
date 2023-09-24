package elide.runtime.plugins.ruby

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.core.evaluate

/**
 * Execute the given Ruby [source] code, returning the result. This is equivalent to calling
 * [PolyglotContext.evaluate] and selecting [Ruby] as source language.
 *
 * @param source The source code to be executed.
 * @param name Name to provide for this source fragment.
 * @return The result of the invocation.
 */
@DelicateElideApi public fun PolyglotContext.ruby(source: String, name: String? = null): PolyglotValue {
  return evaluate(Ruby, source, name = name)
}
