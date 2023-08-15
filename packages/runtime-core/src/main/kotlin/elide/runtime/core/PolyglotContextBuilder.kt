package elide.runtime.core

import org.graalvm.polyglot.Context

/**
 * A builder allowing configuration of [PolyglotContext] instances. Plugins can intercept builders to apply custom
 * options using this class.
 *
 * Under the current implementation, this is an alias over GraalVM's [Context.Builder].
 */
@DelicateElideApi public typealias PolyglotContextBuilder = Context.Builder