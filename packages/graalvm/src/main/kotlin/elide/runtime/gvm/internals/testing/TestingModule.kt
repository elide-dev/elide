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
@file:Suppress("DataClassPrivateConstructor", "MnInjectionPoints")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.testing

import com.google.errorprone.annotations.ThreadSafe
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.LinkedList
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import jakarta.inject.Provider
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.toImmutableList
import elide.annotations.Factory
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.AssertAPI
import elide.runtime.intrinsics.testing.TestingAPI
import elide.runtime.intrinsics.testing.TestingAPI.Expect
import elide.runtime.intrinsics.testing.TestingAPI.TestGraphNode.*
import elide.runtime.intrinsics.testing.TestingRegistrar
import elide.runtime.intrinsics.testing.TestingRegistrar.*
import elide.runtime.node.asserts.NodeAssertModule
import elide.runtime.node.asserts.assertionError
import elide.vm.annotations.Polyglot

// Constants.
private const val TEST = "test"
private const val DESCRIBE = "describe"
private const val SUITE = "suite"
private const val EXPECT = "expect"
private const val TEST_MODULE_NAME = "test"
private const val TEST_API_SYMBOL_NAME = "elide_test"
private val TEST_API_SYMBOL = TEST_API_SYMBOL_NAME.asJsSymbol()

// All test API properties.
private val testModuleProps = arrayOf(
  SUITE,
  TEST,
  DESCRIBE,
  EXPECT,
)

// Assertion methods.
private val testAssertionMethodsAndProps = arrayOf(
  "not",
  "isNull",
  "isNotNull",
  "toBe",
  "toBeTrue",
  "toBeFalse",
)

// Installs the Elide test runner and API bindings.
@Intrinsic
@Factory
internal class ElideTestingModule @Inject constructor (
  registrar: Provider<TestingRegistrar>,
) : AbstractNodeBuiltinModule() {
  private val singleton = TestingImpl.obtain(registrar)

  @Singleton fun provide(): TestingAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[TEST_API_SYMBOL] = ProxyExecutable { provide() }
  }

  init {
    ModuleRegistry.deferred(ModuleInfo.of(TEST_MODULE_NAME)) { provide() }
  }
}

public abstract class GuestAssertionStack (
  protected val assertApi: AssertAPI,
  invert: Boolean = false,
) : GuestAssertion {
  protected val stack: MutableList<Pair<Expectation, String?>> = mutableListOf()
  private val inverted = atomic(invert)

  internal abstract fun copy(invert: Boolean = false): GuestAssertionStack

  private fun runAssertion(block: () -> Boolean): Boolean {
    // @TODO logging? instrumentation for assertions?
    return block.invoke()
  }

  override fun expect(expectation: Expectation, message: String?): AssertionSuite = apply {
    stack.add(expectation to message)
  }

  private fun collapse(assert: AssertAPI, other: Value? = null): AssertionSuite = apply {
    val (last, msg) = stack.last()
    val result = when (last) {
      is Expect.PrimitiveExpectation -> runAssertion { last.satisfy(value) }
      is Expect.ComplexExpectation -> runAssertion { last.satisfy(assert, value, other) }
      else -> error("Unable to enforce expectation: $last")
    }

    when (if (inverted.value) !result else result) {
      // the condition was satisfied
      true -> {}

      // the condition was not satisfied
      false -> throw assertionError(msg ?: "assertion failed: '$last'")
    }
  }

  private fun expectAndCollapse(
    expect: Expectation,
    assert: AssertAPI,
    message: String? = null,
    other: Value? = null,
  ): AssertionSuite = apply {
    expect(expect, message)
    collapse(assert, other)
  }

  @Polyglot override fun isNull(message: String?): AssertionSuite = apply {
    expectAndCollapse(
      Expect.IsNull,
      assertApi,
      message,
    )
  }

  @Polyglot override fun isNotNull(message: String?): AssertionSuite = apply {
    expectAndCollapse(
      Expect.IsNotNull,
      assertApi,
      message,
    )
  }

  @Polyglot override fun toBe(other: Value?, message: String?): AssertionSuite = apply {
    expectAndCollapse(
      Expect.IsEqual,
      assertApi,
      message,
      other,
    )
  }

  @Polyglot override fun notToBe(other: Value?, message: String?): AssertionSuite = apply {
    expectAndCollapse(
      Expect.IsNotEqual,
      assertApi,
      message,
      other,
    )
  }

  @Polyglot override fun toBeTrue(message: String?): AssertionSuite = apply {
    expectAndCollapse(
      Expect.IsTrue,
      assertApi,
      message,
    )
  }

  @Polyglot override fun toBeFalse(message: String?): AssertionSuite = apply {
    expectAndCollapse(
      Expect.IsFalse,
      assertApi,
      message,
    )
  }

  override fun getMemberKeys(): Array<String> = testAssertionMethodsAndProps

  override fun getMember(key: String): Any? = when (key) {
    "isNull" -> ProxyExecutable { isNull(it.firstOrNull()?.asString()) }
    "isNotNull" -> ProxyExecutable { isNotNull(it.firstOrNull()?.asString()) }
    "toBeTrue" -> ProxyExecutable { toBeTrue(it.firstOrNull()?.asString()) }
    "toBeFalse" -> ProxyExecutable { toBeFalse(it.firstOrNull()?.asString()) }
    "toBe" -> ProxyExecutable {
      when (it.size) {
        0 -> throw JsError.typeError("`toBe` requires at least one argument")
        1 -> toBe(it.first(), null)
        else -> toBe(it.first(), it[1].asString())
      }
    }

    // inverted form
    "not" -> object: ReadOnlyProxyObject {
      override fun getMemberKeys(): Array<String> = testAssertionMethodsAndProps
      override fun getMember(inner: String): Any? = when (inner) {
        "not" -> this@GuestAssertionStack.getMember(inner)
        "isNull" -> ProxyExecutable { isNotNull(it.firstOrNull()?.asString()) }
        "isNotNull" -> ProxyExecutable { isNull(it.firstOrNull()?.asString()) }
        "toBeTrue" -> ProxyExecutable { toBeFalse(it.firstOrNull()?.asString()) }
        "toBeFalse" -> ProxyExecutable { toBeTrue(it.firstOrNull()?.asString()) }
        "toBe" -> ProxyExecutable {
          when (it.size) {
            0 -> throw JsError.typeError("`toBe` requires at least one argument")
            1 -> notToBe(it.first(), null)
            else -> notToBe(it.first(), it[1].asString())
          }
        }
        else -> null
      }
    }
    else -> null
  }
}

public class GuestValueAssertion(assert: AssertAPI, private val wrapped: Value?): GuestAssertionStack(assert) {
  override val value: Value? get() = wrapped
  override val expectations: Collection<Expectation> get() = stack.toImmutableList().map { it.first }

  override fun copy(invert: Boolean): GuestAssertionStack {
    return GuestValueAssertion(
      assertApi,
      wrapped,
    )
  }
}

// Implements a thread-safe testing registrar.
@Singleton @ThreadSafe internal class TestingRegistrarImpl : TestingRegistrar {
  private val witnessOrder = LinkedList<Pair<TestScope<*>, RegisteredTest>>()
  private val scopes = ConcurrentSkipListSet<RegisteredScope>()
  private val registry = ConcurrentSkipListMap<String, RegisteredTest>()
  private val byScope = ConcurrentSkipListMap<RegisteredScope, MutableSet<RegisteredTest>>()
  private val activeScope = atomic<RegisteredScope?>(null)
  private val isMutable = atomic(true)

  private inline fun <T> withMutable(crossinline block: () -> T): T {
    return if (isMutable.value) {
      block.invoke()
    } else error(
      "cannot modify test registry after it has been frozen",
    )
  }

  internal fun activeScope(or: RegisteredScope? = null): TestScope<*> {
    return activeScope.value ?: or ?: GlobalTestScope
  }

  override fun freeze(): TestingRegistrar = apply {
    isMutable.value = false
  }

  override fun all(): Sequence<RegisteredTest> = witnessOrder.asSequence().map { it.second }

  override fun grouped(): Sequence<Pair<TestScope<*>, RegisteredTest>> = witnessOrder.asSequence()

  override fun resetScope() = withMutable {
    activeScope.value = null
  }

  override fun register(test: RegisteredTest) = withMutable {
    val scope = activeScope()
    registry[test.qualifiedName] = test
    witnessOrder.addLast(scope to test)
  }

  override fun register(scope: RegisteredScope): Unit = withMutable {
    scopes.add(scope)
  }

  override fun register(test: RegisteredTest, scope: RegisteredScope): Unit = withMutable {
    scopes.add(scope)
    registry[test.qualifiedName] = test
    byScope.computeIfAbsent(scope) { ConcurrentSkipListSet<RegisteredTest>() }.also {
      byScope[scope]!!.add(test)
    }
    witnessOrder.addLast(scope to test)
  }

  override fun withinScope(scope: RegisteredScope): Scoped = object: Scoped, TestingRegistrar by this {
    override val scope: RegisteredScope get() = scope
    override val parent: TestingRegistrar get() = this@TestingRegistrarImpl

    override fun register(test: RegisteredTest): Unit = withMutable {
      register(test, scope)
    }
  }.also {
    activeScope.value = scope
  }
}

// Implements Elide's guest-exposed testing APIs.
internal class TestingImpl private constructor (
  private val registrar: Provider<TestingRegistrar>,
  private val assertApi: AssertAPI,
) : TestingAPI, ReadOnlyProxyObject {
  companion object {
    private lateinit var SINGLETON: TestingImpl

    fun obtain(registrar: Provider<TestingRegistrar>): TestingAPI {
      if (!::SINGLETON.isInitialized) {
        SINGLETON = TestingImpl(registrar, NodeAssertModule().provide())
      }
      return SINGLETON
    }
  }

  // If a suite definition returns a promise, await it.
  @Suppress("TooGenericExceptionCaught")
  private fun handleSuiteRegistrationResult(result: Value) {
    when {
      result.isNull -> {}
      else -> runCatching { result.`as`<Future<*>>(Future::class.java) }.getOrNull()?.let { promise ->
        try {
          promise.get(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
          // Handle promise exception
          throw JsError.typeError("test suite registration timed out or failed: ${e.message}", e)
        }
      }
    }
  }

  // Creates and registers a named testing scope/suite; implements `suite` and `describe`.
  @Polyglot override fun suite(label: String?, block: Value): Suite = TestingRegistrar.namedScope(block, label).also {
    if (!block.canExecute()) throw JsError.typeError("defining a test suite or group requires a function")
    registrar.get().withinScope(it).use {
      handleSuiteRegistrationResult(block.execute())
    }
  }

  // Creates and registers a named test; implements `test`.
  @Polyglot override fun test(label: String?, block: Value, scope: Suite?): Test {
    if (!block.canExecute()) throw JsError.typeError("defining a test requires a function")
    val registry = registrar.get() as TestingRegistrarImpl
    val active = registry.activeScope()
    val test = TestingRegistrar.guest(block, label, active)

    // register built test
    if (scope as? RegisteredScope != null) {
      registry.register(test, scope)
    } else {
      registry.register(test)
    }
    return test
  }

  // Creates and registers a test expectation; implements `expect`.
  @Polyglot override fun expect(value: Value?): Assertion {
    return GuestValueAssertion(assertApi, value ?: Value.asValue(null))
  }

  override fun getMemberKeys(): Array<String> = testModuleProps

  override fun getMember(key: String): Any? = when (key) {
    SUITE, DESCRIBE -> ProxyExecutable { args ->
      when (args.size) {
        0 -> throw JsError.typeError("`$key` requires at least one argument")
        1 -> suite(null, args.first())
        else -> args.first().let { presumablyName ->
          if (!presumablyName.isString) throw JsError.typeError("`$key` requires a string as the first argument")
          suite(presumablyName.asString(), args[1])
        }
      }
    }

    TEST -> ProxyExecutable { args ->
      when (args.size) {
        0 -> throw JsError.typeError("`$key` requires at least one argument")

        // `test(() => ...)` (with no suite)
        1 -> test(null, args.first(), null)

        // `test('name', () => ...)` and `test(() => ..., suite)`
        else -> args.first().let { presumablyName ->
          if (!presumablyName.isString) throw JsError.typeError("`$key` requires a string as the first argument")

          when (args.size) {
            2 -> test(presumablyName.asString(), args[1], null)
            else -> test(presumablyName.asString(), args[1], args[2].asProxyObject<RegisteredScope>())
          }
        }
      }
    }

    EXPECT -> ProxyExecutable { args ->
      when (args.size) {
        0 -> throw JsError.typeError("`$key` requires at least one argument")

        // `expect(value)`
        else -> expect(args.first())
      }
    }

    else -> null
  }
}
