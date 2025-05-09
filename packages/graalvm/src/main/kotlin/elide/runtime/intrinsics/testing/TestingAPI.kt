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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.intrinsics.testing

import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Value
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import elide.annotations.API
import elide.runtime.core.DelicateElideApi
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.js.node.AssertAPI
import elide.runtime.node.asserts.assertionError
import elide.vm.annotations.Polyglot

// Timeout to wait for async tasks (in seconds).
private const val ASYNC_TIMEOUT = 30L

/**
 * # Testing API
 */
@API @ReflectiveAccess public interface TestingAPI {
  public sealed interface TestGraphNode {
    public sealed interface TestAddressable : TestGraphNode {
      public val simpleName: String
      public val qualifiedName: String
    }

    public interface Suite : TestAddressable
    public interface Test : TestAddressable

    public sealed interface Assertion : TestGraphNode
    public sealed interface Expectation : Assertion

    public sealed interface SimpleAssertion : Expectation {
      public fun satisfy(value: Value?): Boolean
    }

    public sealed interface ParameterizedAssertion : Expectation {
      public fun satisfy(assert: AssertAPI, left: Value?, right: Value?): Boolean
    }

    public sealed interface AssertionSuite : Assertion, ReadOnlyProxyObject {
      public val expectations: Collection<Expectation>
      public fun expect(expectation: Expectation, message: String? = null): AssertionSuite
      @Polyglot public fun isNull(message: String?): AssertionSuite
      @Polyglot public fun isNotNull(message: String?): AssertionSuite
      @Polyglot public fun toBe(other: Value?, message: String?): AssertionSuite
      @Polyglot public fun toBeTrue(message: String?): AssertionSuite
      @Polyglot public fun toBeFalse(message: String?): AssertionSuite
    }

    public interface GuestAssertion : AssertionSuite {
      public val value: Value?
    }
  }

  /**
   * ## Expectations
   */
  public data object Expect {
    public sealed interface PrimitiveExpectation : TestGraphNode.SimpleAssertion
    public sealed interface ComplexExpectation : TestGraphNode.ParameterizedAssertion
    public sealed interface FunctionExpectation : TestGraphNode.SimpleAssertion
    public sealed interface AsyncExpectation : FunctionExpectation

    /** Expect: a value is not null. */
    public data object IsNotNull: PrimitiveExpectation {
      override fun satisfy(value: Value?): Boolean {
        return value != null && !value.isNull
      }
    }

    /** Expect: a value is null. */
    public data object IsNull: PrimitiveExpectation {
      override fun satisfy(value: Value?): Boolean {
        return value == null || value.isNull
      }
    }

    /** Expect: a value is true. */
    public data object IsTrue: PrimitiveExpectation {
      override fun satisfy(value: Value?): Boolean {
        return value != null && value.isBoolean && value.asBoolean()
      }
    }

    /** Expect: a value is false. */
    public data object IsFalse: PrimitiveExpectation {
      override fun satisfy(value: Value?): Boolean {
        return value != null && value.isBoolean && !value.asBoolean()
      }
    }

    /** Expect: a value is the same exact instance as another value. */
    public data object IsSame: ComplexExpectation {
      override fun satisfy(assert: AssertAPI, left: Value?, right: Value?): Boolean {
        return left === right
      }
    }

    /** Expect: a value is equal to another value. */
    public data object IsEqual: ComplexExpectation {
      override fun satisfy(assert: AssertAPI, left: Value?, right: Value?): Boolean {
        return runCatching { assert.equal(left, right) }.isSuccess
      }
    }

    /** Expect: a value is not equal to another value. */
    public data object IsNotEqual: ComplexExpectation {
      override fun satisfy(assert: AssertAPI, left: Value?, right: Value?): Boolean {
        return runCatching { assert.notEqual(left, right) }.isSuccess
      }
    }

    /** Expect: a function runs without error. */
    public data object DoesNotThrow: FunctionExpectation {
      override fun satisfy(value: Value?): Boolean {
        if (value == null) throw assertionError("Cannot run null function")
        return runCatching { value.executeVoid() }.isSuccess
      }
    }

    /** Expect: a function throws an error. */
    public data object Throws: FunctionExpectation {
      override fun satisfy(value: Value?): Boolean {
        if (value == null) throw assertionError("Cannot run null function")
        return runCatching { value.executeVoid() }.isFailure
      }
    }

    @JvmStatic private fun assertFutOrAsyncMethod(value: Value, negate: Boolean = false): Boolean {
      return when {
        // it might be a function which returns a future
        value.canExecute() -> runCatching { assertFutOrAsyncMethod(value.execute()) }.let {
          if (negate) it.isFailure else it.isSuccess
        }

        // or it might be a future itself
        else -> if (!value.isProxyObject) {
          throw assertionError("Cannot convert object to future or function which produces future")
        } else try {
          val promise = value.`as`(Future::class.java)
          runCatching { promise.get(ASYNC_TIMEOUT, TimeUnit.SECONDS) }.let {
            if (negate) it.isFailure else it.isSuccess
          }
        } catch (_: ClassCastException) {
          throw assertionError("Not a future and not a function that produces a future")
        } catch (_: PolyglotException) {
          throw assertionError("Error while testing rejecting function")
        }
      }
    }

    /** Expect: an async function concludes without error. */
    public data object DoesNotReject: AsyncExpectation {
      override fun satisfy(value: Value?): Boolean {
        if (value == null) throw assertionError("Cannot run null function")
        return assertFutOrAsyncMethod(value)
      }
    }

    /** Expect: an async function rejects with an error. */
    public data object Rejects: AsyncExpectation {
      override fun satisfy(value: Value?): Boolean {
        if (value == null) throw assertionError("Cannot run null function")
        return assertFutOrAsyncMethod(value, true)
      }
    }
  }

  /**
   * ## Suite
   */
  @Polyglot public fun suite(block: Value): TestGraphNode.Suite = suite(label = null, block)

  /**
   * ## Suite
   */
  @Polyglot public fun suite(label: String?, block: Value): TestGraphNode.Suite

  /**
   * ## Describe
   */
  @Polyglot public fun describe(block: Value): TestGraphNode.Suite = describe(label = null, block)

  /**
   * ## Describe
   */
  @Polyglot public fun describe(label: String?, block: Value): TestGraphNode.Suite = suite(label, block)

  /**
   * ## Test
   */
  @Polyglot public fun test(value: Value): TestGraphNode.Test = test(label = null, value, scope = null)

  /**
   * ## Test
   */
  @Polyglot public fun test(value: Value, scope: TestGraphNode.Suite): TestGraphNode.Test =
    test(label = null, value, scope = scope)

  /**
   * ## Test
   */
  @Polyglot public fun test(label: String?, block: Value, scope: TestGraphNode.Suite?): TestGraphNode.Test

  /**
   * ## Expectation
   */
  @Polyglot public fun expect(value: Value?): TestGraphNode.Assertion
}
