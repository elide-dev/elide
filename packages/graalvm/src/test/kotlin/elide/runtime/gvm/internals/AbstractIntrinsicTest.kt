package elide.runtime.gvm.internals

import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.testing.annotations.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.graalvm.polyglot.Context as VMContext

/** Abstract base for intrinsic-bound tests. */
internal abstract class AbstractIntrinsicTest<T : GuestIntrinsic> {
  /** @return Intrinsic implementation under test. */
  protected abstract fun provide(): T

  /** @return Initialized and exclusively-owned context for use with this test. */
  protected abstract fun <V: Any> withContext(op: VMContext.() -> V): V

  /** @return Execute a guest script with the subject intrinsics bound. */
  protected abstract fun executeGuest(bind: Boolean = true, op: VMContext.() -> String): GuestTestExecution

  /** Test injection of an intrinsic implementation. */
  @Test abstract fun testInjectable()

  /** Test installation of the intrinsic. */
  @Test fun testInstall() {
    val target = HashMap<String, Any>()
    val subject = provide()
    assertNotNull(subject, "should not get `null` subject from `provide` for intrinsic under test")
    assertDoesNotThrow {
      subject.install(MutableIntrinsicBindings.Factory.wrap(target))
    }
    assertTrue(target.isNotEmpty(), "should have at least one intrinsic binding installed")
  }

  /** Single test execution within the scope of a guest VM. */
  internal inner class GuestTestExecution (
    private val factory: (VMContext.() -> Unit) -> Unit,
    private val test: VMContext.() -> Unit
  ) {
    /** After guest execution concludes, execute the provided [assertions] against the test context. */
    fun thenAssert(assertions: VMContext.() -> Unit) = factory {
      test.invoke(this)
      assertions.invoke(this)
    }
  }
}
