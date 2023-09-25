package elide.runtime.core

import org.graalvm.polyglot.Engine

/**
 * A builder allowing configuration of [PolyglotEngine] instances. Plugins can intercept builders to apply custom
 * options using this class.
 *
 * Under the current implementation, this is an alias over GraalVM's [Engine.Builder].
 */
@DelicateElideApi public typealias PolyglotEngineBuilder = Engine.Builder