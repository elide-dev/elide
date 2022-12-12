package elide.runtime.gvm.internals.js

import elide.annotations.Inject
import elide.annotations.core.Polyglot
import elide.runtime.gvm.internals.AbstractIntrinsicTest
import elide.runtime.gvm.internals.ContextManager
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.graalvm.polyglot.Context as VMContext

/** Abstract base for JS intrinsics. */
internal abstract class AbstractJsIntrinsicTest<T : GuestIntrinsic> : AbstractIntrinsicTest<T>() {
  @Inject lateinit var contextManager: ContextManager<VMContext, VMContext.Builder>

  // Binding for guest assertions: `assertTrue`.
  @Polyglot fun nativeAssertTrue(condition: Boolean, message: String? = null) =
    assertTrue(condition, message)

  // Binding for guest assertions: `assertFalse`.
  @Polyglot fun nativeAssertFalse(condition: Boolean, message: String? = null) =
    assertFalse(condition, message)

  // Binding for guest assertions: `assertNotNull`.
  @Polyglot fun nativeAssertNotNull(subject: Any?, message: String? = null) =
    assertNotNull(subject, message)

  // Binding for guest assertions: `assertNull`.
  @Polyglot fun nativeAssertNull(subject: Any?, message: String? = null) =
    assertNull(subject, message)

  // Binding for guest assertions: `assertEquals`.
  @Polyglot fun nativeAssertEquals(expected: Any?, subject: Any?, message: String? = null) =
    assertEquals(expected, subject, message)

  // Binding for guest assertions: `assertNotEquals`.
  @Polyglot fun nativeAssertNotEquals(expected: Any?, subject: Any?, message: String? = null) =
    assertNotEquals(expected, subject, message)

  // Run the provided `op` with an active (and exclusively owned) JS VM context.
  override fun <V: Any> withContext(op: VMContext.() -> V): V = runBlocking {
    contextManager {
      op.invoke(this)
    }
  }

  // Run the provided factory to produce a script, then run that test within a warmed `Context`.
  override fun executeGuest(bind: Boolean, op: VMContext.() -> String) = GuestTestExecution(::withContext) {
    // resolve the script
    val script = op.invoke(this)

    // prep intrinsic bindings under test
    val bindings = if (bind) {
      val target = HashMap<String, Any>()
      provide().install(GuestIntrinsic.MutableIntrinsicBindings.Factory.wrap(target))
      target
    } else {
      emptyMap()
    }

    // install bindings under test
    val target = getBindings("js")
    bindings.forEach {
      target.putMember(it.key, it.value)
    }
    val hasErr = AtomicBoolean(false)
    val subjectErr: AtomicReference<Throwable> = AtomicReference(null)

    // execute script
    try {
      enter()
      assertDoesNotThrow {
        eval("js", script)
      }
    } catch (err: Throwable) {
      hasErr.set(true)
      subjectErr.set(err)
    } finally {
      leave()
    }
    if (hasErr.get()) {
      throw subjectErr.get()
    }
  }
}
