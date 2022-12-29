@file:Suppress("MemberVisibilityCanBePrivate", "SameParameterValue")

package elide.runtime.gvm.internals.js

import elide.annotations.Inject
import elide.annotations.core.Polyglot
import elide.runtime.gvm.internals.AbstractIntrinsicTest
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import org.graalvm.polyglot.Context as VMContext
import org.graalvm.polyglot.Value as GuestValue

/** Abstract base for JS intrinsics. */
internal abstract class AbstractJsIntrinsicTest<T : GuestIntrinsic>(
  private val testInject: Boolean = true,
) : AbstractIntrinsicTest<T>() {
  companion object {
    init {
      System.setProperty("elide.js.vm.enableStreams", "true")
    }
  }

  private val initialized: AtomicBoolean = AtomicBoolean(false)

  // Guest context manager.
  @Inject lateinit var contextManager: ContextManager<VMContext, VMContext.Builder>

  // JS runtime.
  @Inject lateinit var jsvm: JsRuntime

  // Run the provided `op` with an active (and exclusively owned) JS VM context.
  override fun <V: Any> withContext(op: VMContext.() -> V): V = runBlocking {
    if (!initialized.get()) {
      contextManager.installContextFactory {
        jsvm.builder(it)
      }
      contextManager.installContextSpawn {
        jsvm.spawn(it)
      }
      contextManager.activate(start = false)
    }
    contextManager {
      op.invoke(this)
    }
  }

  /** Interface returned by assertions for further customization. */
  interface TestContext {
    /** Perform a truth assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot fun shouldBeTrue(message: String? = null)

    /** Perform a truth assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot fun shouldBeTrue() = shouldBeTrue(null)

    /** Perform a false assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot fun shouldBeFalse(message: String? = null)

    /** Perform a false assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot fun shouldBeFalse() = shouldBeFalse(null)

    /** Perform a not-null assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot fun isNotNull() = isNotNull(null)

    /** Perform a not-null assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot fun isNotNull(message: String?)

    /** Perform a null assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot fun isNull() = isNull(null)

    /** Perform a null assertion on the held value, or display [message] as an error (or a default message). */
    @Polyglot fun isNull(message: String?)

    /** Perform an equality assertion between the held value and the [other] value. */
    @Polyglot fun isEqualTo(other: Any?) = isEqualTo(other, null)

    /** Perform an equality assertion between the held value and the [other] value. */
    @Polyglot fun isEqualTo(other: Any?, message: String?)

    /** Perform an in-equality assertion between the held value and the [other] value. */
    @Polyglot fun isNotEqualTo(other: Any?) = isNotEqualTo(other, null)

    /** Perform an in-equality assertion between the held value and the [other] value. */
    @Polyglot fun isNotEqualTo(other: Any?, message: String? = null)

    /** Perform a failure assertion on the held value, which is expected to be a function. */
    @Polyglot fun fails() = fails(null)

    /** Perform a failure assertion on the held value, which is expected to be a function. */
    @Polyglot fun fails(message: String? = null)

    /** Execute a [shouldBeTrue] assertion on the held value (syntactic sugar). */
    @Polyglot fun message(message: String) = shouldBeTrue(message)
  }

  /** Base interface for a guest assertion type. */
  internal interface TestAssertion {
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

    /** @inheritDoc */
    @Polyglot override fun shouldBeTrue(message: String?) = executeTest {
      val boolValue = when (val value = assertion.value) {
        is Boolean -> value
        is GuestValue -> if (value.isBoolean) {
          value.asBoolean()
        } else error(
          "Guest value '$value' is not a boolean"
        )
        else -> error(
          "Value '$value' is not a boolean"
        )
      }
      assertTrue(
        boolValue,
        message ?: this.message.get() ?: "expected guest value to be `true`",
      )
    }

    /** @inheritDoc */
    @Polyglot override fun shouldBeFalse(message: String?) = executeTest {
      val boolValue = when (val value = assertion.value) {
        is Boolean -> value
        is GuestValue -> if (value.isBoolean) {
          value.asBoolean()
        } else error(
          "Guest value '$value' is not a boolean"
        )
        else -> error(
          "Value '$value' is not a boolean"
        )
      }
      assertFalse(
        boolValue,
        message ?: this.message.get() ?: "expected guest value to be `false`",
      )
    }

    /** @inheritDoc */
    @Polyglot override fun isNotNull(message: String?) = executeTest {
      val possiblyNull = when (val value = assertion.value) {
        null -> null
        is GuestValue -> if (value.isNull) {
          null
        } else value
        else -> value
      }
      assertNotNull(possiblyNull, message ?: "expected guest value to be non-null (got `$possiblyNull`)")
    }

    /** @inheritDoc */
    @Polyglot override fun isNull(message: String?) = executeTest {
      val possiblyNull = when (val value = assertion.value) {
        null -> null
        is GuestValue -> if (value.isNull) {
          null
        } else value
        else -> value
      }
      assertNull(possiblyNull, message ?: "expected guest value to be null (got `$possiblyNull`)")
    }

    /** @inheritDoc */
    @Polyglot override fun isEqualTo(other: Any?, message: String?) = executeTest {
      assertEquals(
        other,
        assertion.value,
        message ?: "expected guest value equality mismatch (got `${assertion.value}`, but expected `$other`)"
      )
    }

    /** @inheritDoc */
    @Polyglot override fun isNotEqualTo(other: Any?, message: String?) = executeTest {
      assertNotEquals(
        assertion.value,
        other,
        message ?: "expected guest value inequality mismatch (got `${assertion.value}``)"
      )
    }

    /** @inheritDoc */
    @Polyglot override fun fails(message: String?) {
      val executable = when (val value = assertion.value) {
        is GuestValue -> if (value.canExecute()) {
          {  value.executeVoid() }
        } else error(
          "Guest value is not executable, cannot execute failure test"
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

  /** Assertion capture interface. */
  @FunctionalInterface internal interface JsAssertion : TestAssertion, Function<Any?, TestContext> {
    /** Invoke a null-check-based assertion. */
    @Polyglot override fun apply(value: Any?): TestContext
  }

  /** Default top-level assertion implementation. */
  internal class CaptureAssertion : JsAssertion {
    private val heldValue: AtomicReference<Any?> = AtomicReference(null)
    override val value: Any? get() = heldValue.get()
    @Polyglot override fun apply(value: Any?): TestContext {
      heldValue.set(value)
      return TestResultContext(this)
    }
  }

  // Logic to execute a guest-side test.
  private inline fun executeGuestInternal(
    ctx: VMContext,
    bind: Boolean,
    bindUtils: Boolean,
    op: VMContext.() -> String,
  ): GuestValue {
    // resolve the script
    val script = op.invoke(ctx)

    // prep intrinsic bindings under test
    val bindings = if (bind) {
      val target = HashMap<JsSymbol, Any>()
      provide().install(GuestIntrinsic.MutableIntrinsicBindings.Factory.wrap(target))
      target
    } else {
      emptyMap()
    }

    // install bindings under test, if directed
    val target = ctx.getBindings("js")
    bindings.forEach {
      target.putMember(it.key.symbol, it.value)
    }

    // install utility bindings, if directed
    if (bindUtils) {
      target.putMember("test", CaptureAssertion())
    }

    // prep for execution
    val hasErr = AtomicBoolean(false)
    val subjectErr: AtomicReference<Throwable> = AtomicReference(null)

    // execute script
    val returnValue = try {
      ctx.enter()
      ctx.eval("js", script)
    } catch (err: Throwable) {
      hasErr.set(true)
      subjectErr.set(err)
      throw subjectErr.get()
    } finally {
      ctx.leave()
    }
    return returnValue
  }

  // Run the provided factory to produce a script, then run that test within a warmed `Context`.
  override fun executeGuest(bind: Boolean, op: VMContext.() -> String) = GuestTestExecution(::withContext) {
    executeGuestInternal(
      this,
      bind,
      bindUtils = true,
      op,
    )
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun executeDual(op: () -> Unit, guest: VMContext.() -> String) = executeDual(true, op, guest)

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun executeDual(
    bind: Boolean,
    op: () -> Unit,
    guest: VMContext.() -> String,
  ) = GuestTestExecution(::withContext) {
    op.invoke()
    executeGuestInternal(
      this,
      bind,
      bindUtils = true,
      guest,
    )
  }

  /** Proxy which wires together a dual-test execution (in the guest and on the host). */
  internal abstract inner class DualTestExecutionProxy {
    /**
     * Wire together the guest-side of a dual test.
     *
     * @param defer Whether to defer for additional assertions.
     * @param guestOperation Operation to run on the guest.
     */
    abstract fun guest(guestOperation: VMContext.() -> String)

    /**
     * Wire together the guest-side of a dual test, but defer for additional assertions.
     *
     * @param guestOperation Operation to run on the guest.
     * @return Guest test execution control proxy.
     */
    abstract fun thenRun(guestOperation: VMContext.() -> String): GuestTestExecution
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun dual(op: () -> Unit): DualTestExecutionProxy =
    dual(true, op)

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun dual(bind: Boolean, op: () -> Unit): DualTestExecutionProxy {
    op.invoke()

    return object : DualTestExecutionProxy() {
      /** @inheritDoc */
      override fun guest(guestOperation: Context.() -> String) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          guestOperation,
        )
      }.doesNotFail()

      /** @inheritDoc */
      override fun thenRun(guestOperation: Context.() -> String) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          guestOperation,
        )
      }
    }
  }
}
