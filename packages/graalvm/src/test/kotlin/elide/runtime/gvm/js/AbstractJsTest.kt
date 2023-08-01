@file:Suppress("SameParameterValue", "MemberVisibilityCanBePrivate")

package elide.runtime.gvm.js

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.FileSystem
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import elide.annotations.Inject
import elide.runtime.gvm.internals.AbstractDualTest
import elide.runtime.gvm.internals.GraalVMGuest
import elide.runtime.gvm.internals.IntrinsicsManager
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.gvm.internals.js.JsRuntime
import elide.runtime.intrinsics.GuestIntrinsic

/** Abstract dual-execution test which expects a JavaScript snippet in addition to a regular test. */
@Suppress("unused")
internal abstract class AbstractJsTest : AbstractDualTest() {
  private val initialized: AtomicBoolean = AtomicBoolean(false)

  // Guest context manager.
  @Inject lateinit var contextManager: ContextManager<Context, Context.Builder>

  // JS runtime.
  @Inject lateinit var jsvm: JsRuntime

  // Default intrinsics manager.
  @Inject lateinit var defaultIntrinsicsManager: IntrinsicsManager

  /** @return Intrinsics manager for this test. */
  protected open fun intrinsicsManager(): IntrinsicsManager = defaultIntrinsicsManager

  // Logic to execute a guest-side test.
  private inline fun executeGuestInternal(
    ctx: Context,
    bind: Boolean,
    bindUtils: Boolean,
    esm: Boolean,
    op: Context.() -> String,
  ): Value {
    // resolve the script
    val script = op.invoke(ctx)

    // install bindings under test, if directed
    val target = ctx.getBindings("js")

    // prep intrinsic bindings under test
    if (bind) {
      val manager = intrinsicsManager()
      val intrinsicsMap = mutableMapOf<JsSymbol, Any>()
      val bindings = GuestIntrinsic.MutableIntrinsicBindings.Factory.wrap(intrinsicsMap)

      manager.resolver().resolve(GraalVMGuest.JAVASCRIPT).forEach { intrinsic ->
        intrinsic.install(bindings)
      }

      bindings.forEach {
        target.putMember(it.key.symbol, it.value)
      }
    }

    // install utility bindings, if directed
    if (bindUtils) {
      target.putMember("test", AbstractJsIntrinsicTest.CaptureAssertion())
    }

    // prep for execution
    val hasErr = AtomicBoolean(false)
    val subjectErr: AtomicReference<Throwable> = AtomicReference(null)

    // build a source chunk for the test script
    val src = Source.newBuilder(
      "js",
      script,
      if (esm) "test.mjs" else "test.js",
    ).interactive(
      false,
    ).cached(
      false,
    ).internal(
      false,
    ).mimeType(
      if (esm) "application/javascript+module" else "application/javascript",
    ).encoding(
      StandardCharsets.UTF_8,
    ).build()

    // execute script
    val returnValue = try {
      ctx.enter()
      ctx.eval(src)
    } catch (err: Throwable) {
      hasErr.set(true)
      subjectErr.set(err)
      throw subjectErr.get()
    } finally {
      ctx.leave()
    }
    return returnValue
  }

  // Build an entirely custom context.
  override fun buildContext(engine: Engine, conf: (Context.Builder.() -> Unit)?): Context.Builder {
    val builder = jsvm.builder(engine)
    conf?.invoke(builder)
    return builder
  }

  // Run the provided `op` with an active (and exclusively owned) JS VM context.
  override fun <V : Any> withContext(op: Context.() -> V): V = runBlocking {
    if (!initialized.get()) {
      contextManager.installContextFactory {
        buildContext(it, null)
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

  // Run the provided `op` with an active (and exclusively owned) JS VM context, and configured with `conf`.
  override fun <V : Any> withContext(op: Context.() -> V, conf: (Context.Builder.() -> Unit)?): V {
    if (!initialized.get()) {
      contextManager.installContextFactory {
        buildContext(it, conf)
      }
      contextManager.installContextSpawn {
        jsvm.spawn(it)
      }
      contextManager.activate(start = false)
    }
    return contextManager {
      op.invoke(this)
    }
  }

  // Run the provided factory to produce an ESM script, then run that test within a warmed `Context`.
  fun executeESM(bind: Boolean = true, op: Context.() -> String) = GuestTestExecution(::withContext) {
    executeGuestInternal(
      this,
      bind,
      bindUtils = true,
      esm = true,
      op,
    )
  }

  // Run the provided factory to produce a script, then run that test within a warmed `Context`.
  override fun executeGuest(bind: Boolean, op: Context.() -> String) = GuestTestExecution(::withContext) {
    executeGuestInternal(
      this,
      bind,
      bindUtils = true,
      esm = false,
      op,
    )
  }

  // Configure a context and then return a guest test execution bound to it.
  protected fun configureContext(
    conf: Context.Builder.() -> Unit,
    op: Context.() -> String,
  ) = GuestTestExecution(conf, { withContext(op, conf) }) {
    executeGuestInternal(
      this,
      bind = true,
      bindUtils = true,
      esm = false,
      op,
    )
  }

  // Configure a context and then return a guest test execution bound to it.
  @Suppress("DEPRECATION")
  protected fun withVFS(fs: FileSystem, op: Context.() -> String): GuestTestExecution {
    val conf: (Context.Builder.() -> Unit) = { fileSystem(fs).build() }
    return GuestTestExecution(conf, { withContext(op, conf) }) {
      executeGuestInternal(
        this,
        bind = true,
        bindUtils = true,
        esm = false,
        op,
      )
    }
  }

  /** Proxy which wires together a dual-test execution (in the guest and on the host). */
  internal abstract inner class DualTestExecutionProxy {
    /**
     * Wire together the guest-side of a dual test.
     *
     * @param esm Whether to execute as an ESM module.
     * @param guestOperation Operation to run on the guest.
     */
    abstract fun guest(esm: Boolean = false, guestOperation: Context.() -> String)

    /**
     * Wire together the guest-side of a dual test, but defer for additional assertions.
     *
     * @param guestOperation Operation to run on the guest.
     * @return Guest test execution control proxy.
     */
    abstract fun thenRun(guestOperation: Context.() -> String): GuestTestExecution
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun dual(op: () -> Unit): DualTestExecutionProxy =
    dual(true, op)

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun dual(bind: Boolean, op: () -> Unit): DualTestExecutionProxy {
    op.invoke()

    return object : DualTestExecutionProxy() {
      /** @inheritDoc */
      override fun guest(esm: Boolean, guestOperation: Context.() -> String) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          esm = esm,
          guestOperation,
        )
      }.doesNotFail()

      /** @inheritDoc */
      override fun thenRun(guestOperation: Context.() -> String) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          esm = false,
          guestOperation,
        )
      }
    }
  }
}
