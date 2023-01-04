package elide.runtime.gvm.internals

import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicReference
import org.graalvm.polyglot.Context as VMContext

/** Base implementation of a test which can spawn VM contexts, and execute tests within them. */
internal abstract class AbstractDualTest {
  /** @return Initialized and exclusively-owned context for use with this test. */
  protected abstract fun <V: Any> withContext(op: VMContext.() -> V): V

  /** @return Execute a guest script with the subject intrinsics bound. */
  protected abstract fun executeGuest(bind: Boolean = true, op: VMContext.() -> String): GuestTestExecution

  /** Single test execution within the scope of a guest VM. */
  internal inner class GuestTestExecution (
    private val factory: (VMContext.() -> Unit) -> Unit,
    private val test: VMContext.() -> Value?
  ) {
    // Return value, if any.
    private val returnValue: AtomicReference<Value?> = AtomicReference(null)

    // Set the return value from a test execution.
    private fun setReturnValue(value: Value): Unit = returnValue.set(value)

    // Retrieve the return value from a test execution.
    internal fun returnValue(): Value? = returnValue.get()

    /** After guest execution concludes, execute the provided [assertions] against the test context. */
    fun thenAssert(assertions: VMContext.(GuestTestExecution) -> Unit) = factory {
      val result = test.invoke(this)
      if (result != null) {
        setReturnValue(result)
      }
      assertions.invoke(this@factory, this@GuestTestExecution)
    }

    /** Stubbed `Unit` returning function which ties off the execution. */
    fun doesNotFail(): Unit = thenAssert {
      // nothing: stubbed (should raise)
    }
  }
}
