package elide.runtime.core

import org.graalvm.polyglot.Value

/**
 * A value managed by the polyglot engine. It is common for instances to be obtained after evaluating guest code in a
 * [PolyglotContext].
 */
@DelicateElideApi public typealias PolyglotValue = Value