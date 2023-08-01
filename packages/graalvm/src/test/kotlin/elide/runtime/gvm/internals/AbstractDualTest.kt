@file:Suppress("MemberVisibilityCanBePrivate")

package elide.runtime.gvm.internals

import com.fasterxml.jackson.databind.ObjectMapper
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Value
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import org.graalvm.polyglot.Context as VMContext

/** Base implementation of a test which can spawn VM contexts, and execute tests within them. */
internal abstract class AbstractDualTest {
  companion object {
    @JvmStatic fun loadResource(path: String): String {
      return requireNotNull(AbstractDualTest::class.java.getResource(path)) {
        "failed to locate resource at $path"
      }.readText(
        StandardCharsets.UTF_8
      )
    }

    @JvmStatic inline fun <reified T> loadJSON(path: String): T {
      return requireNotNull(ObjectMapper().readValue(loadResource(path), T::class.java)) {
        "failed to parse JSON at $path"
      }
    }

    @JvmStatic inline fun <reified T, R> withJSON(path: String, noinline op: suspend (T) -> R): R {
      val data = loadJSON<T>(path)
      val result: AtomicReference<R> = AtomicReference(null)
      runTest {
        result.set(op.invoke(data))
      }
      return result.get()
    }
  }

  /** @return Initialized and exclusively-owned context for use with this test. */
  protected abstract fun buildContext(engine: Engine, conf: (VMContext.Builder.() -> Unit)?): VMContext.Builder

  /** @return Initialized and exclusively-owned context for use with this test. */
  protected abstract fun <V: Any> withContext(op: VMContext.() -> V): V

  /** @return Initialized and exclusively-owned context for use with this test. */
  protected abstract fun <V: Any> withContext(op: VMContext.() -> V, conf: (VMContext.Builder.() -> Unit)?): V

  /** @return Execute a guest script with the subject intrinsics bound. */
  protected abstract fun executeGuest(bind: Boolean = true, op: VMContext.() -> String): GuestTestExecution

  /** Single test execution within the scope of a guest VM. */
  internal inner class GuestTestExecution (
    private val builder: (VMContext.Builder.() -> Unit)?,
    private val factory: (VMContext.() -> Unit) -> Unit,
    private val test: VMContext.() -> Value?
  ) {
    constructor (
      factory: (VMContext.() -> Unit) -> Unit,
      test: VMContext.() -> Value?
    ) : this (
      null,
      factory,
      test = test
    )

    // Return value, if any.
    private val returnValue: AtomicReference<Value?> = AtomicReference(null)

    // Set the return value from a test execution.
    private fun setReturnValue(value: Value): Unit = returnValue.set(value)

    // Use the provided `builder` or generate one that just builds the context without changing it.
    internal fun builder(): VMContext.Builder.() -> Unit {
      return builder ?: { /* no-op */ }
    }

    // Retrieve the return value from a test execution.
    internal fun returnValue(): Value? = returnValue.get()

    /** After guest execution concludes, execute the provided [assertions] against the test context. */
    fun thenAssert(
      allowFailure: Boolean = false,
      assertions: (VMContext.(GuestTestExecution) -> Unit)? = null,
    ) = factory {
      if (allowFailure) {
        failsWith<Throwable> {
          if (assertions != null) assertions.invoke(this@factory, this@GuestTestExecution) else {
            test.invoke(this)
          }
        }
      } else {
        val result = test.invoke(this)
        if (result != null) setReturnValue(result)
        assertions?.invoke(this@factory, this@GuestTestExecution)
      }
    }

    /** After guest execution concludes, execute the provided [assertions] against the test context. */
    inline fun <reified X: Throwable> failsWith(
      noinline assertions: (VMContext.(GuestTestExecution) -> Unit)? = null
    ) = factory {
      val exc = assertThrows<Throwable> {
        val result = test.invoke(this)
        if (result != null) setReturnValue(result)
        assertions?.invoke(this@factory, this@GuestTestExecution)
      }
      when (exc) {
        is X -> {
          // expected
        }

        is PolyglotException -> if (exc.isHostException) {
          assert(exc.asHostException() is X) {
            "Invalid exception type '${exc::class.simpleName}' raised " +
            "(expected '${X::class.java.simpleName}')"
          }
        }

        else -> throw AssertionError(
          "Invalid exception type '${exc::class.simpleName}' raised " +
            "(expected '${X::class.java.simpleName}')"
        )
      }
    }

    /** Stubbed `Unit` returning function which ties off the execution. */
    fun doesNotFail(): Unit = thenAssert {
      // nothing: stubbed
    }

    /** Stubbed `Unit` returning function which expects failure. */
    fun fails(): Unit = thenAssert(allowFailure = true) {
      // nothing: stubbed (should raise)
    }
  }
}
