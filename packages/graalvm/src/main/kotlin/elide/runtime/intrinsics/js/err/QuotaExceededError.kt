package elide.runtime.intrinsics.js.err

import elide.annotations.core.Polyglot

/**
 * # JavaScript: `QuotaExceededError`
 *
 */
public class QuotaExceededError (@get:Polyglot override val message: String) : ValueError()
