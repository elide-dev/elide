package elide.runtime.plugins.python

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.core.evaluate

/**
 * Execute the given Python [source] code, returning the result. This is equivalent to calling
 * [PolyglotContext.evaluate] and selecting [Python] as source language.
 *
 * @param source The Python source code to be executed.
 * @return The result of the invocation.
 */
@DelicateElideApi public fun PolyglotContext.python(
  /*language=python*/ source: String,
  name: String? = null,
): PolyglotValue {
  return evaluate(Python, source, name = name)
}
