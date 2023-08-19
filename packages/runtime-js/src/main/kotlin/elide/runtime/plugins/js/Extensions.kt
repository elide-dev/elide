package elide.runtime.plugins.js

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.core.execute

/**
 * Execute the given JavaScript [source] code, returning the result. This is equivalent to calling
 * [PolyglotContext.execute] and selecting [JavaScript] as source language.
 *
 * @param source The JavaScript source code to be executed.
 * @return The result of the invocation.
 */
@DelicateElideApi public fun PolyglotContext.javaScript(/*language=javascript*/ source: String): PolyglotValue {
  return execute(JavaScript, source)
}
