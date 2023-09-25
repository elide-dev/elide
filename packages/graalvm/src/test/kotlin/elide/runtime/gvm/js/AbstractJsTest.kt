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

@file:Suppress("SameParameterValue", "MemberVisibilityCanBePrivate")

package elide.runtime.gvm.js

import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.FileSystem
import org.intellij.lang.annotations.Language
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.gvm.internals.AbstractDualTest
import elide.runtime.gvm.internals.GraalVMGuest
import elide.runtime.gvm.internals.IntrinsicsManager
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.Symbol
import elide.runtime.plugins.js.JavaScript

/** Abstract dual-execution test which expects a JavaScript snippet in addition to a regular test. */
@OptIn(DelicateElideApi::class)
@Suppress("unused")
internal abstract class AbstractJsTest : AbstractDualTest() {
  /**
   * Typed and highlighted JavaScript test executor context.
   */
  internal interface JsTestContext : GuestTestContext {
    /**
     * Prepares guest code in a test for the JavaScript VM.
     *
     * @param code Code to use for the test.
     */
    override fun code(@Language("javascript") code: String)
  }

  private val initialized: AtomicBoolean = AtomicBoolean(false)

  // Default intrinsics manager.
  @Inject lateinit var defaultIntrinsicsManager: IntrinsicsManager

  /** @return Intrinsics manager for this test. */
  protected open fun intrinsicsManager(): IntrinsicsManager = defaultIntrinsicsManager

  // Logic to execute a guest-side test.
  private inline fun executeGuestInternal(
    ctx: PolyglotContext,
    bind: Boolean,
    bindUtils: Boolean,
    esm: Boolean,
    op: PolyglotContext.() -> String,
  ): Value {
    // resolve the script
    val script = op.invoke(ctx)

    // install bindings under test, if directed
    val target = ctx.bindings(JavaScript)

    // prep intrinsic bindings under test
    if (bind) {
      val manager = intrinsicsManager()
      val intrinsicsMap = mutableMapOf<Symbol, Any>()
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
      ctx.evaluate(src)
    } catch (err: Throwable) {
      hasErr.set(true)
      subjectErr.set(err)
      throw subjectErr.get()
    } finally {
      ctx.leave()
    }
    return returnValue
  }

  override fun configureEngine(config: PolyglotEngineConfiguration) {
    config.install(JavaScript)
  }

  // Build an entirely custom context.
  //override fun buildContext(engine: Engine, conf: (Context.Builder.() -> Unit)?): Context.Builder {
  //  val builder = jsvm.builder(engine)
  //  conf?.invoke(builder)
  //  return builder
  //}

  // Run the provided `op` with an active (and exclusively owned) JS VM context.
  //override fun <V : Any> withContext(op: Context.() -> V): V = runBlocking {
  //  if (!initialized.get()) {
  //    contextManager.installContextFactory {
  //      buildContext(it, null)
  //    }
  //    contextManager.installContextSpawn {
  //      jsvm.spawn(it)
  //    }
  //    contextManager.activate(start = false)
  //  }
  //  contextManager {
  //    op.invoke(this)
  //  }
  //}

  // Run the provided `op` with an active (and exclusively owned) JS VM context, and configured with `conf`.
  //override fun <V : Any> withContext(op: Context.() -> V, conf: (Context.Builder.() -> Unit)?): V {
  //  if (!initialized.get()) {
  //    contextManager.installContextFactory {
  //      buildContext(it, conf)
  //    }
  //    contextManager.installContextSpawn {
  //      jsvm.spawn(it)
  //    }
  //    contextManager.activate(start = false)
  //  }
  //  return contextManager {
  //    op.invoke(this)
  //  }
  //}

  // Run the provided factory to produce an ESM script, then run that test within a warmed `Context`.
  fun executeESM(bind: Boolean = true, op: PolyglotContext.() -> String) = GuestTestExecution(::withContext) {
    executeGuestInternal(
      this,
      bind,
      bindUtils = true,
      esm = true,
      op,
    )
  }

  fun test(op: context(PolyglotContext, JsTestContext) () -> Unit) = test(
    bind = true,
    op = op,
  )

  fun test(bind: Boolean, op: context(PolyglotContext, JsTestContext) () -> Unit) = GuestTestExecution(::withContext) {
    executeGuestInternal(
      this,
      bind,
      bindUtils = true,
      esm = false,
    ) {
      val codeToRun = AtomicReference<String>(null)
      val wrapped = object : JsTestContext {
        override fun code(code: String) {
          codeToRun.set(code)
        }
      }
      op.invoke(this, wrapped)
      codeToRun.get() ?: error("Failed to resolve guest code for test")
    }
  }

  // Run the provided factory to produce a script, then run that test within a warmed `Context`.
  override fun executeGuest(bind: Boolean, op: PolyglotContext.() -> String) = GuestTestExecution(::withContext) {
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
    op: PolyglotContext.() -> String
  ) = GuestTestExecution({ withContext(op) }) {
    executeGuestInternal(
      this,
      bind = true,
      bindUtils = true,
      esm = false,
      op,
    )
  }

  // TODO(@darlvd): rewrite tests using this to use a common FS
  // Configure a context and then return a guest test execution bound to it.
  protected fun withVFS(fs: FileSystem, op: PolyglotContext.() -> String): GuestTestExecution {
    // val conf: (Context.Builder.() -> Unit) = { fileSystem(fs).build() }
    //return GuestTestExecution(conf, { withContext(op, conf) }) {
    //  executeGuestInternal(
    //    this,
    //    bind = true,
    //    bindUtils = true,
    //    esm = false,
    //    op,
    //  )
    //}
    error("not supported")
  }

  /** Proxy which wires together a dual-test execution (in the guest and on the host). */
  internal abstract inner class JsDualTestExecutionProxy : AbstractDualTest.DualTestExecutionProxy() {
    /**
     * Wire together the guest-side of a dual test.
     *
     * @param esm Whether to execute as an ESM module.
     * @param guestOperation Operation to run on the guest.
     */
    abstract fun guest(esm: Boolean = false, guestOperation: PolyglotContext.() -> String)
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  override fun dual(bind: Boolean, op: () -> Unit): DualTestExecutionProxy {
    op.invoke()

    return object : JsDualTestExecutionProxy() {
      override fun guest(esm: Boolean, guestOperation: PolyglotContext.() -> String) =
        GuestTestExecution(::withContext) {
          executeGuestInternal(
            this,
            bind,
            bindUtils = true,
            esm = esm,
            guestOperation,
          )
        }.doesNotFail()

      override fun guest(guestOperation: PolyglotContext.() -> String) {
        return guest(false, guestOperation)
      }

      override fun thenRun(guestOperation: PolyglotContext.() -> String) = GuestTestExecution(::withContext) {
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
