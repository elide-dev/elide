package elide.runtime.core.extensions

import org.graalvm.polyglot.management.ExecutionListener
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.internals.graalvm.GraalVMEngine

/**
 * Attaches this [builder][ExecutionListener.Builder] to a GraalVM-backed [PolyglotEngine].
 *
 * @param engine A [PolyglotEngine] to attach this listener to.
 * @throws IllegalArgumentException If the provided [engine] is not backed by a GraalVM implementation and does not
 * otherwise support execution listeners.
 */
@DelicateElideApi public fun ExecutionListener.Builder.attach(engine: PolyglotEngine): ExecutionListener {
  require(engine is GraalVMEngine) { "The provided engine does not support GraalVM execution listeners." }
  return attach(engine.unwrap())
}