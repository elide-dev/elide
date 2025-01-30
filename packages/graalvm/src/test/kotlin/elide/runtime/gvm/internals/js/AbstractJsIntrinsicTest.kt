/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
@file:Suppress("MemberVisibilityCanBePrivate", "SameParameterValue")

package elide.runtime.gvm.internals.js

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyObject
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import kotlinx.coroutines.test.runTest
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.gvm.internals.AbstractDualTest.JavaScript
import elide.runtime.gvm.internals.AbstractIntrinsicTest
import elide.runtime.gvm.internals.intrinsics.js.base64.Base64Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.console.ConsoleIntrinsic
import elide.runtime.node.asserts.NodeAssertModule
import elide.runtime.node.asserts.NodeAssertStrictModule
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.Symbol
import elide.runtime.plugins.js.JavaScript as JavaScriptPlugin
import elide.runtime.plugins.js.javascript
import elide.runtime.plugins.vfs.vfs
import elide.vm.annotations.Polyglot
import org.graalvm.polyglot.Value as GuestValue

/** Abstract base for JS intrinsics. */
@OptIn(DelicateElideApi::class)
internal abstract class AbstractJsIntrinsicTest<T : GuestIntrinsic>(
  private val testInject: Boolean = true,
) : AbstractIntrinsicTest<T, JavaScript>() {
  companion object {
    init {
      System.setProperty("elide.js.vm.enableStreams", "true")
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
  private fun executeGuestInternal(
      ctx: PolyglotContext,
      bind: Boolean,
      bindUtils: Boolean,
      bindPrimordials: Boolean,
      bindAssert: Boolean,
      bindConsole: Boolean,
      bindBase64: Boolean,
      op: JavaScript,
  ): GuestValue {
    // resolve the script
    val script = op.invoke(ctx)

    // prep intrinsic bindings under test
    val langBindings = if (bind) {
      val target = HashMap<Symbol, Any>()
      val binding = GuestIntrinsic.MutableIntrinsicBindings.Factory.wrap(target)
      provide().install(binding)
      if (bindAssert && !target.any { it.key.symbol.contains("assert") }) {
        NodeAssertModule().install(binding)
        NodeAssertStrictModule().install(binding)
      }
      if (bindConsole && !target.any { it.key.symbol.contains("console") }) {
        ConsoleIntrinsic().install(binding)
      }
      if (bindBase64 && !target.any { it.key.symbol.contains("Base64") }) {
        Base64Intrinsic().install(binding)
      }
      target
    } else {
      emptyMap()
    }

    // prep internal bindings
    val internalBindings: MutableMap<String, Any?> = mutableMapOf()
    langBindings.forEach {
      if (it.key.isInternal) {
        internalBindings[it.key.internalSymbol] = it.value
      }
    }

    // install bindings under test (public only so far)
    val target = ctx.bindings(JavaScriptPlugin)
    for (binding in langBindings) {
      if (binding.key.isInternal) {
        continue
      }
      target.putMember(binding.key.symbol, binding.value)
    }

    if (bindPrimordials) {
      // shim primordials
      val primordialsProxy = object: ProxyObject, ProxyHashMap {
        override fun getMemberKeys(): Array<String> = internalBindings.keys.toTypedArray()
        override fun hasMember(key: String?): Boolean = key != null && key in internalBindings
        override fun hasHashEntry(key: GuestValue?): Boolean = key != null && key.asString() in internalBindings
        override fun getHashSize(): Long = internalBindings.size.toLong()
        override fun getHashEntriesIterator(): Any = internalBindings.entries.iterator()
        override fun putMember(key: String?, value: GuestValue?) {
          // no-op
        }
        override fun putHashEntry(key: Value?, value: Value?) {
          // no-op
        }

        override fun getMember(key: String?): Any? = when (key) {
          null -> null
          else -> internalBindings[key]
        }

        override fun getHashValue(key: Value?): Any? = getMember(key?.asString())
      }

      target.putMember("primordials", primordialsProxy)
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
      ctx.javascript(script)
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
    config.apply {
      install(JavaScriptPlugin)
      vfs { deferred = false }
    }
  }

  // Run the provided factory to produce a script, then run that test within a warmed `Context`.
  override fun executeGuest(bind: Boolean, op: JavaScript) = GuestTestExecution(::withContext) {
    executeGuestInternal(
      this,
      bind,
      bindUtils = true,
      bindPrimordials = true,
      bindAssert = true,
      bindConsole = true,
      bindBase64 = true,
      op,
    )
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun executeDual(op: () -> Unit, guest: JavaScript) = executeDual(true, op, guest)

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  protected fun executeDual(
      bind: Boolean,
      op: () -> Unit,
      guest: JavaScript,
  ) = GuestTestExecution(::withContext) {
    op.invoke()
    executeGuestInternal(
      this,
      bind,
      bindUtils = true,
      bindPrimordials = true,
      bindAssert = true,
      bindConsole = true,
      bindBase64 = true,
      guest,
    )
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  override fun dual(bind: Boolean, op: suspend () -> Unit): DualTestExecutionProxy<JavaScript> {
    runTest {
      assertDoesNotThrow {
        op.invoke()
      }
    }
    return object : DualTestExecutionProxy<JavaScript>() {
      override fun guest(guestOperation: JavaScript) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          bindPrimordials = true,
          bindAssert = true,
          bindConsole = true,
          bindBase64 = true,
          guestOperation,
        )
      }.doesNotFail()

      override fun thenRun(guestOperation: JavaScript) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          bindPrimordials = true,
          bindAssert = true,
          bindConsole = true,
          bindBase64 = true,
          guestOperation,
        )
      }
    }
  }

  suspend inline fun SequenceScope<DynamicTest>.dynamicGuestTest(
    name: String,
    crossinline cbk: () -> String,
  ) {
    yield(
      dynamicTest(name) {
        dual {}.guest { cbk.invoke() }
      }
    )
  }
}
