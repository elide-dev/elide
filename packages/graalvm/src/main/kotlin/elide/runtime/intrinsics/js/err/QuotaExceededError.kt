package elide.runtime.intrinsics.js.err

import elide.annotations.core.Polyglot

/**
 * # JavaScript: `QuotaExceededError`
 *
 */
public class QuotaExceededError : ValueError() {
  @get:Polyglot override val message: String
    get() = TODO("Not yet implemented")
}
