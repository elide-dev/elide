package elide.runtime.plugins.ruby

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.core.execute

/**
 * Execute the given Ruby [source] code, returning the result. This is equivalent to calling
 * [PolyglotContext.execute] and selecting [Ruby] as source language.
 *
 * @param source The source code to be executed.
 * @return The result of the invocation.
 */
@DelicateElideApi public fun PolyglotContext.ruby(source: String): PolyglotValue {
  return execute(Ruby, source)
}
