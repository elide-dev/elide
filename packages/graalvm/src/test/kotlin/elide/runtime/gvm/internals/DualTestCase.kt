package elide.runtime.gvm.internals

import org.graalvm.polyglot.Context

/** Base implementation of a guest VM-compatible test case. */
internal abstract class DualTestCase : AbstractDualTest() {
  /** @return Spawned context for use with a guest test case. */
  override fun <V : Any> withContext(op: Context.() -> V): V {
    TODO("Not yet implemented")
  }

  /** @return Guest test execution which further action can be taken on. */
  override fun executeGuest(bind: Boolean, op: Context.() -> String): GuestTestExecution {
    TODO("Not yet implemented")
  }
}
