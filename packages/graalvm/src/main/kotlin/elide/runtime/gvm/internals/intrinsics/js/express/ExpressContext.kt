package elide.runtime.gvm.internals.intrinsics.js.express

import org.graalvm.polyglot.Context

/** Internal abstraction used to manage VM context operations and JVM lifecycle utilities. */
internal interface ExpressContext {
  /** Increase the number of background tasks being performed that should prevent the JVM from exiting. */
  fun pin()

  /** Decrease the number of background tasks being performed that should prevent the JVM from exiting. */
  fun unpin()

  /** Execute a [block] of code after safely entering a VM [Context]. The context will be exited automatically. */
  fun <T> useGuest(block: Context.() -> T): T
}
