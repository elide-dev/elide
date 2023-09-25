/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("MemberVisibilityCanBePrivate")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals

import com.fasterxml.jackson.databind.ObjectMapper
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration
import elide.vm.annotations.Polyglot
import kotlinx.coroutines.test.runTest
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Value
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

/** Base implementation of a test which can spawn VM contexts, and execute tests within them. */
abstract class AbstractDualTest {
  companion object {
    @JvmStatic
    fun loadResource(path: String): String {
      return requireNotNull(AbstractDualTest::class.java.getResource(path)) {
        "failed to locate resource at $path"
      }.readText(
        StandardCharsets.UTF_8,
      )
    }

    @JvmStatic
    inline fun <reified T> loadJSON(path: String): T {
      return requireNotNull(ObjectMapper().readValue(loadResource(path), T::class.java)) {
        "failed to parse JSON at $path"
      }
    }

    @JvmStatic
    inline fun <reified T, R> withJSON(path: String, noinline op: suspend (T) -> R): R {
      val data = loadJSON<T>(path)
      val result: AtomicReference<R> = AtomicReference(null)
      runTest {
        result.set(op.invoke(data))
      }
      return result.get()
    }
  }

  /**
   * Typed and highlighted guest executor context.
   */
  internal interface GuestTestContext {
    /**
     * Prepares guest code in a test.
     *
     * @param code Code to use for the test.
     */
    fun code(code: String)
  }

  /** Interface returned by assertions for further customization. */
  interface TestContext {
    /** Perform a truth assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot
    fun shouldBeTrue(message: String? = null)

    /** Perform a truth assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot
    fun shouldBeTrue() = shouldBeTrue(null)

    /** Perform a false assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot
    fun shouldBeFalse(message: String? = null)

    /** Perform a false assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot
    fun shouldBeFalse() = shouldBeFalse(null)

    /** Perform a not-null assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot
    fun isNotNull() = isNotNull(null)

    /** Perform a not-null assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot
    fun isNotNull(message: String?)

    /** Perform a null assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot
    fun isNull() = isNull(null)

    /** Perform a null assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot
    fun isNull(message: String?)

    /** Perform an equality assertion between the held value and the [other] value. */
    @Polyglot
    fun isEqualTo(other: Any?) = isEqualTo(other, null)

    /** Perform an equality assertion between the held value and the [other] value. */
    @Polyglot
    fun isEqualTo(other: Any?, message: String?)

    /** Perform an in-equality assertion between the held value and the [other] value. */
    @Polyglot
    fun isNotEqualTo(other: Any?) = isNotEqualTo(other, null)

    /** Perform an in-equality assertion between the held value and the [other] value. */
    @Polyglot
    fun isNotEqualTo(other: Any?, message: String? = null)

    /** Perform a failure assertion on the held value, which is expected to be a function. */
    @Polyglot
    fun fails() = fails(null)

    /** Perform a failure assertion on the held value, which is expected to be a function. */
    @Polyglot
    fun fails(message: String? = null)

    /** Execute a [shouldBeTrue] assertion on the held value (syntactic sugar). */
    @Polyglot
    fun message(message: String) = shouldBeTrue(message)
  }

  /** Base interface for a guest assertion type. */
  interface TestAssertion {
    /** Value associated with this assertion. */
    val value: Any?
  }

  // Default test result context.
  @Suppress("UNCHECKED_CAST")
  class TestResultContext(
    private val assertion: TestAssertion,
  ) : TestContext {
    private val expectFail: AtomicBoolean = AtomicBoolean(false)
    private val executed: AtomicBoolean = AtomicBoolean(false)
    private val pass: AtomicBoolean = AtomicBoolean(false)
    private val exc: AtomicReference<Throwable?> = AtomicReference(null)
    private val message: AtomicReference<String?> = AtomicReference(null)

    // Execute the provided test while recording the outcome.
    private fun executeTest(test: () -> Unit) = try {
      executed.compareAndSet(false, true)
      test.invoke()
    } catch (err: Throwable) {
      exc.set(err)
      pass.set(!expectFail.get())
    } finally {
      if (!expectFail.get() && exc.get() != null) {
        throw exc.get()!!
      } else if (expectFail.get() && exc.get() == null) {
        throw AssertionError("Expected failure, but test passed.")
      }
    }

    @Polyglot
    override fun shouldBeTrue(message: String?) = executeTest {
      val boolValue = when (val value = assertion.value) {
        is Boolean -> value
        is Value -> if (value.isBoolean) {
          value.asBoolean()
        } else error(
          "Guest value '$value' is not a boolean",
        )

        else -> error(
          "Value '$value' is not a boolean",
        )
      }
      Assertions.assertTrue(
        boolValue,
        message ?: this.message.get() ?: "expected guest value to be `true`",
      )
    }

    @Polyglot
    override fun shouldBeFalse(message: String?) = executeTest {
      val boolValue = when (val value = assertion.value) {
        is Boolean -> value
        is Value -> if (value.isBoolean) {
          value.asBoolean()
        } else error(
          "Guest value '$value' is not a boolean",
        )

        else -> error(
          "Value '$value' is not a boolean",
        )
      }
      Assertions.assertFalse(
        boolValue,
        message ?: this.message.get() ?: "expected guest value to be `false`",
      )
    }

    @Polyglot
    override fun isNotNull(message: String?) = executeTest {
      val possiblyNull = when (val value = assertion.value) {
        null -> null
        is Value -> if (value.isNull) {
          null
        } else value

        else -> value
      }
      Assertions.assertNotNull(possiblyNull, message ?: "expected guest value to be non-null (got `$possiblyNull`)")
    }

    @Polyglot
    override fun isNull(message: String?) = executeTest {
      val possiblyNull = when (val value = assertion.value) {
        null -> null
        is Value -> if (value.isNull) {
          null
        } else value

        else -> value
      }
      Assertions.assertNull(possiblyNull, message ?: "expected guest value to be null (got `$possiblyNull`)")
    }

    @Polyglot
    override fun isEqualTo(other: Any?, message: String?) = executeTest {
      Assertions.assertEquals(
        other,
        assertion.value,
        message ?: "expected guest value equality mismatch (got `${assertion.value}`, but expected `$other`)",
      )
    }

    @Polyglot
    override fun isNotEqualTo(other: Any?, message: String?) = executeTest {
      Assertions.assertNotEquals(
        assertion.value,
        other,
        message ?: "expected guest value inequality mismatch (got `${assertion.value}``)",
      )
    }

    @Polyglot
    override fun fails(message: String?) {
      val executable = when (val value = assertion.value) {
        is Value -> if (value.canExecute()) {
          { value.executeVoid() }
        } else error(
          "Guest value is not executable, cannot execute failure test",
        )

        is Runnable -> {
          { value.run() }
        }

        is Callable<*> -> {
          { value.call() }
        }

        is Function<*, *> -> {
          val args: Array<Any> = emptyArray()
          val callable = value as Function<Array<Any>, Any>
          { callable.apply(args) }
        }

        else -> error("Expected guest value for failure test, but got: '$value'")
      }
      expectFail.set(true)
      if (!message.isNullOrBlank()) this.message.set(message)
      executeTest {
        executable.invoke()
      }
    }
  }

  /** A [PolyglotEngine] used to acquire context instances for testing, configurable trough [configureEngine]. */
  protected val engine: PolyglotEngine by lazy { PolyglotEngine(::configureEngine) }

  /** Configure the [engine] used to acquire contexts passed to [withContext]. */
  protected open fun configureEngine(config: PolyglotEngineConfiguration) {
    // nothing by default
  }

  /** Acquire an exclusive [PolyglotContext] instance from the [engine] and use it with a given [block] of code. */
  protected open fun <T> withContext(block: PolyglotContext.() -> T): T {
    return block(engine.acquire())
  }

  /** @return Execute a guest script with the subject intrinsics bound. */
  protected abstract fun executeGuest(bind: Boolean = true, op: PolyglotContext.() -> String): GuestTestExecution

  /** Single test execution within the scope of a guest VM. */
  inner class GuestTestExecution(
    val factory: (PolyglotContext.() -> Unit) -> Unit,
    val test: PolyglotContext.() -> Value?
  ) {
    // Return value, if any.
    private val returnValue: AtomicReference<Value?> = AtomicReference(null)

    // Set the return value from a test execution.
    fun setReturnValue(value: Value): Unit = returnValue.set(value)

    // Retrieve the return value from a test execution.
    fun returnValue(): Value? = returnValue.get()

    /** After guest execution concludes, execute the provided [assertions] against the test context. */
    fun thenAssert(
      allowFailure: Boolean = false,
      assertions: (PolyglotContext.(GuestTestExecution) -> Unit)? = null,
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
    inline fun <reified X : Throwable> failsWith(
      noinline assertions: (PolyglotContext.(GuestTestExecution) -> Unit)? = null
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
                  "(expected '${X::class.java.simpleName}')",
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

  /** Proxy which wires together a dual-test execution (in the guest and on the host). */
  abstract inner class DualTestExecutionProxy {
    /**
     * Wire together the guest-side of a dual test.
     *
     * @param guestOperation Operation to run on the guest.
     */
    abstract fun guest(guestOperation: PolyglotContext.() -> String)

    /**
     * Wire together the guest-side of a dual test, but defer for additional assertions.
     *
     * @param guestOperation Operation to run on the guest.
     * @return Guest test execution control proxy.
     */
    abstract fun thenRun(guestOperation: PolyglotContext.() -> String): GuestTestExecution
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun dual(op: () -> Unit): DualTestExecutionProxy =
    dual(true, op)

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected abstract fun dual(bind: Boolean, op: () -> Unit): DualTestExecutionProxy
}
