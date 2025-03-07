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
@file:Suppress(
  "ThrowsCount",
  "TooGenericExceptionCaught",
  "TooManyFunctions",
  "CyclomaticComplexMethod",
  "LargeClass",
  "LongMethod",
)

package elide.runtime.node.asserts

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.Value.asValue
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import java.math.BigInteger
import java.util.*
import java.util.Optional.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import kotlin.jvm.optionals.getOrNull
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.err.JsException
import elide.runtime.intrinsics.js.node.AssertAPI
import elide.runtime.intrinsics.js.node.asserts.AssertionError
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.lang.javascript.SyntheticJSModule
import elide.runtime.lang.javascript.SyntheticJSModule.ExportedSymbol
import elide.vm.annotations.Polyglot

// Symbol where the internal module implementation is installed.
private const val ASSERT_MODULE_SYMBOL: String = "node_${NodeModuleName.ASSERT}"

// Symbol where the assertion error type is installed.
private const val ASSERTION_ERROR_SYMBOL: String = "AssertionError"

private const val METHOD_OK = "ok"
private const val METHOD_NOT_OK = "notOk"
private const val METHOD_FAIL = "fail"
private const val METHOD_ASSERT = "assert"
private const val METHOD_EQUAL = "equal"
private const val METHOD_STRICT = "strict"
private const val METHOD_NOT_EQUAL = "notEqual"
private const val METHOD_DEEP_EQUAL = "deepEqual"
private const val METHOD_NOT_DEEP_EQUAL = "notDeepEqual"
private const val METHOD_DEEP_STRICT_EQUAL = "deepStrictEqual"
private const val METHOD_NOT_DEEP_STRICT_EQUAL = "notDeepStrictEqual"
private const val METHOD_MATCH = "match"
private const val METHOD_DOES_NOT_MATCH = "doesNotMatch"
private const val METHOD_THROWS = "throws"
private const val METHOD_DOES_NOT_THROW = "doesNotThrow"
private const val METHOD_REJECTS = "rejects"
private const val METHOD_DOES_NOT_REJECT = "doesNotReject"

// Methods provided by the Node assert module.
private val assertionModuleMethods = arrayOf(
  METHOD_OK,
  METHOD_NOT_OK,
  METHOD_FAIL,
  METHOD_ASSERT,
  METHOD_EQUAL,
  METHOD_STRICT,
  METHOD_NOT_EQUAL,
  METHOD_DEEP_EQUAL,
  METHOD_NOT_DEEP_EQUAL,
  METHOD_DEEP_STRICT_EQUAL,
  METHOD_NOT_DEEP_STRICT_EQUAL,
  METHOD_MATCH,
  METHOD_DOES_NOT_MATCH,
  METHOD_THROWS,
  METHOD_DOES_NOT_THROW,
  METHOD_REJECTS,
  METHOD_DOES_NOT_REJECT,
)

// Installs the Node assert module into the intrinsic bindings.
@Intrinsic internal class NodeAssertModule : SyntheticJSModule<AssertAPI>, AbstractNodeBuiltinModule() {
  private val instance by lazy { NodeAssert.create() }
  override fun provide(): AssertAPI = instance

  override fun install(bindings: MutableIntrinsicBindings) {
    // @TODO: fully support `ProxyObject` so this module can be synthetic
    bindings[ASSERT_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { provide() }
    bindings[ASSERTION_ERROR_SYMBOL.asJsSymbol()] = ProxyInstantiable {
      val messageOrErr = it.getOrNull(0)
      when {
        messageOrErr != null -> NodeAssertionError(messageOrErr)
        else -> NodeAssertionError()
      }
    }
  }

  override fun exports(): Array<ExportedSymbol> = assertionModuleMethods.map {
    ExportedSymbol.of(it)
  }.plus(
    ExportedSymbol.default(METHOD_ASSERT),
  ).toTypedArray()
}

// Implements Node's `AssertionError` type, which is thrown for assertion failures.
public class NodeAssertionError internal constructor(
  private val messageOrErr: Any? = null,
  private val isGenerated: Boolean = false,
  private val actualValue: Optional<Any> = empty(),
  private val expectedValue: Optional<Any> = empty(),
  private val operatorValue: String? = null
) : Throwable(messageOrErr as? String), AssertionError, JsException {
  override val name: String get() = super<AssertionError>.name
  override val generatedMessage: Boolean get() = isGenerated
  override val actual: Any? get() = actualValue.getOrNull()
  override val expected: Any? get() = expectedValue.getOrNull()
  override val operator: String? get() = operatorValue

  public companion object {
    private const val DEFAULT_MESSAGE: String = "Failed assertion"
    private const val DEFAULT_OPERATOR: String = "=="

    @JvmStatic internal fun renderMessage(
      operation: String,
      actualValue: Optional<Any>,
      expectedValue: Optional<Any>,
    ): String = StringBuilder().apply {
      append("Expected $operation value")
      append(" '")
      append(expectedValue.orElse(null) ?: "undefined")
      append("', but got: ")
      val value = actualValue.orElse(null)
      if (value == null) append("null")
      else {
        append("'")
        append(value)
        append("'")
      }
    }.toString()

    @JvmStatic @JvmOverloads public fun of(
      message: Any? = null,
      isGenerated: Boolean = false,
      actualValue: Optional<Any> = empty(),
      expectedValue: Optional<Any> = empty(),
      operatorValue: String? = null
    ): NodeAssertionError = NodeAssertionError(
      message ?: DEFAULT_MESSAGE,
      isGenerated,
      actualValue,
      expectedValue,
      operatorValue ?: DEFAULT_OPERATOR,
    )
  }

  override val message: String?
    get() = when (messageOrErr) {
      null -> null
      is String -> messageOrErr
      else -> messageOrErr.toString()
    }
}

// Expectation message for a failure from `ok` or `assert`.
private const val OK_EXPECTATION = "Expected value to be truthy"

@Suppress("LongParameterList")
public fun assertionError(
  message: Any?,
  isGenerated: Boolean = false,
  actualValue: Optional<Any> = empty(),
  expectedValue: Optional<Any> = empty(),
  operatorValue: String? = null,
  operation: String = "check",
): NodeAssertionError = NodeAssertionError.of(
  message ?: NodeAssertionError.renderMessage(operation, actualValue, expectedValue),
  isGenerated,
  actualValue,
  expectedValue,
  operatorValue,
)

internal class NodeAssert private constructor () : AssertAPI {
  companion object {
    @JvmStatic fun create(): AssertAPI = NodeAssert()
  }

  // Assert that a given `value` is truthy (or falsy, if `reverse` is true).
  private fun checkTruthy(reverse: Boolean, value: Any?, message: Any?): Boolean {
    return when (value) {
      null -> false
      is Boolean -> value
      is String -> value.isNotEmpty()
      is Value -> when {
        value.isNull -> false
        value.isBoolean -> value.asBoolean()
        value.isString -> value.asString().isNotEmpty()

        value.isNumber -> when {
          value.fitsInShort() -> value.asShort() > 0
          value.fitsInInt() -> value.asInt() > 0
          value.fitsInLong() -> value.asLong() > 0L
          value.fitsInBigInteger() -> value.asBigInteger() > BigInteger.ZERO
          value.fitsInFloat() -> value.asFloat() > 0.0f
          value.fitsInDouble() -> value.asDouble() > 0.0
          else -> false
        }

        // Non-empty objects should pass
        value.isProxyObject -> value.memberKeys.isNotEmpty()

        // All of these types are automatically truthy.
        value.hasBufferElements() ||
                value.isDate ||
                value.isTime ||
                value.isTimeZone ||
                value.isInstant ||
                value.isDuration ||
                value.isNativePointer ||
                value.isIterator ||
                value.isMetaObject -> true

        value.isHostObject -> checkTruthy(
          reverse,
          value.asHostObject<Any>(),
          message,
        )

        else -> error("Unrecognized polyglot value type: $value (${value.metaObject} / ${value::class.java.name})")
      }

      is Array<*> -> value.isNotEmpty()
      is Collection<*> -> value.isNotEmpty()
      is Map<*, *> -> value.isNotEmpty()
      is Iterator<*> -> value.hasNext()
      is Unit -> false
      is Short -> value > 0
      is Int -> value > 0
      is Long -> value > 0L
      is BigInteger -> value > BigInteger.ZERO
      is Float -> value > 0.0f
      is Double -> value > 0.0

      is ProxyArray -> value.size > 0L
      is ProxyHashMap -> value.hashSize > 0L

      is ProxyObject -> value.memberKeys.let { keys ->
        when (keys) {
          is ProxyArray -> keys.size > 0L
          is Array<*> -> keys.size > 0
          is Collection<*> -> keys.size > 0
          else -> false
        }
      }

      // Otherwise, this is an exotic type, which is assumed not to be falsy.
      else -> true

    }.also { condition ->
      if (condition == reverse) throw assertionError(message ?: OK_EXPECTATION)
    }
  }

  @Polyglot override fun ok(vararg values: Any?) {
    checkTruthy(false, values.firstOrNull(), values.getOrNull(2))
  }

  @Polyglot override fun notOk(value: Any?, message: Any?) {
    checkTruthy(true, value, message)
  }

  @Polyglot override fun fail(message: String?) {
    throw assertionError(message)
  }

  @Polyglot override fun fail(actual: Any?, expected: Any?, message: String?, operator: String?, stackStartFn: Any?) {
    throw assertionError(
      message,
      actualValue = ofNullable(actual),
      expectedValue = ofNullable(expected),
      operatorValue = operator,
    )
  }

  @Polyglot override fun ifError(value: Any?) = when (value) {
    null -> {}
    is Value -> when {
      value.isNull -> {}
      value.isString -> value.asString().isEmpty().let {
        if (!it) throw assertionError("ifError got unwanted exception: $value", actualValue = of(value))
      }

      else -> throw assertionError("ifError got unwanted exception: $value", actualValue = of(value))
    }

    else -> throw assertionError("ifError got unwanted exception: $value", actualValue = of(value))
  }

  @Polyglot override fun assert(value: Any?, message: String?) {
    checkTruthy(false, value, message)
  }

  private fun checkEqual(condition: Boolean, actual: Any?, expected: Any?, message: String?): Boolean {
    return when {
      // if they are the same object, they are also equal
      actual === expected -> true
      actual == null && expected == null -> true
      else -> when {
        // comparison algorithm for two guest values
        actual is Value && expected is Value -> when {
          actual.isNull && expected.isNull -> true
          actual.isNull || expected.isNull -> false
          actual.isNumber && expected.isNumber -> when {
            expected.fitsInShort() && actual.fitsInShort() -> actual.asShort() == expected.asShort()
            expected.fitsInInt() && actual.fitsInInt() -> actual.asInt() == expected.asInt()
            expected.fitsInLong() && actual.fitsInLong() -> actual.asLong() == expected.asLong()
            expected.fitsInFloat() && actual.fitsInFloat() -> actual.asFloat() == expected.asFloat()
            expected.fitsInDouble() && actual.fitsInDouble() -> actual.asDouble() == expected.asDouble()
            expected.fitsInBigInteger() && actual.fitsInBigInteger() -> actual.asBigInteger() == expected.asBigInteger()
            else -> actual == expected
          }

          actual.isHostObject && expected.isHostObject -> actual == expected
          else -> actual == expected
        }

        // guest and host nulls are equivalent
        (actual == null) && (expected is Value && expected.isNull) -> true
        (expected is Value && expected.isNull) && actual == null -> true

        // either side being null with the other being non-null means they are not equal
        actual == null || expected == null -> false

        // comparison algorithm for two host values
        actual !is Value && expected !is Value -> when (actual) {
          // String == ...
          is String -> when (expected) {
            is String -> actual == expected  // == String
            else -> actual == expected.toString()  // == <non-string>
          }

          // Int == ...
          is Int -> when (expected) {
            is Int -> actual == expected  // == Int
            is UInt -> actual == expected.toInt()  // == UInt
            is Short -> actual == expected.toInt()  // == Short
            is Long -> actual.toLong() == expected.toLong()  // == Long
            is Float -> actual.toFloat() == expected.toFloat()  // == Float
            is Double -> actual.toDouble() == expected.toDouble()  // == Double
            is BigInteger -> BigInteger.valueOf(actual.toLong()) == expected  // == BigInteger
            is String -> actual.toString() == expected  // == String
            else -> false
          }

          // UInt == ...
          is UInt -> when (expected) {
            is UInt -> actual == expected  // == UInt
            is Int -> actual.toInt() == expected  // == Int
            is Short -> actual.toInt() == expected.toInt()  // == Short
            is Long -> actual.toLong() == expected.toLong()  // == Long
            is Float -> actual.toFloat() == expected.toFloat()  // == Float
            is Double -> actual.toDouble() == expected.toDouble()  // == Double
            is BigInteger -> BigInteger.valueOf(actual.toLong()) == expected  // == BigInteger
            is String -> actual.toString() == expected  // == String
            else -> actual == expected
          }

          // Long == ...
          is Long -> when (expected) {
            is Long -> actual == expected  // == Long
            is Short -> actual == expected.toLong()  // == Short
            is UInt -> actual == expected.toLong()  // == Int
            is Int -> actual == expected.toLong()  // == UInt
            is Float -> actual.toFloat() == expected  // == Float
            is Double -> actual.toDouble() == expected  // == Double
            is BigInteger -> actual.toBigInteger() == expected  // == BigInteger
            is String -> actual.toString() == expected  // == String
            else -> false
          }

          // Short == ...
          is Short -> when (expected) {
            is Short -> actual == expected  // == Short
            is Int -> actual == expected.toShort()  // == Int
            is UInt -> actual.toInt() == expected.toInt()  // == UInt
            is Long -> actual.toLong() == expected  // == Long
            is Float -> actual.toFloat() == expected  // == Float
            is Double -> actual.toDouble() == expected  // == Double
            is BigInteger -> BigInteger.valueOf(actual.toLong()) == expected  // == BigInteger
            is String -> actual.toString() == expected  // == String
            else -> false
          }

          // Float == ...
          is Float -> when (expected) {
            is Float -> actual == expected  // == Float
            is Double -> actual == expected.toFloat()  // == Double
            is Short -> actual == expected.toFloat() // == Short
            is Int -> actual == expected.toFloat()  // == Int
            is UInt -> actual == expected.toFloat()  // == UInt
            is Long -> actual == expected.toFloat()  // == Long
            is BigInteger -> actual == expected.toFloat()  // == BigInteger
            is String -> actual.toString() == expected  // == String
            else -> false
          }

          // Double == ...
          is Double -> when (expected) {
            is Double -> actual == expected  // == Double
            is Short -> actual == expected.toDouble()  // == Short
            is Int -> actual == expected.toDouble()  // == Int
            is UInt -> actual == expected.toDouble()  // == UInt
            is Long -> actual == expected.toDouble()  // == Long
            is Float -> actual.toFloat() == expected  // == Float
            is BigInteger -> actual == expected.toDouble()  // == BigInteger
            is String -> actual.toString() == expected  // == String
            else -> false
          }

          // BigInteger == ...
          is BigInteger -> when (expected) {
            is BigInteger -> actual == expected  // == BigInteger
            is Int -> actual == BigInteger.valueOf(expected.toLong())  // == Int
            is UInt -> actual == BigInteger.valueOf(expected.toLong())  // == UInt
            is Short -> actual == BigInteger.valueOf(expected.toLong())  // == Short
            is Long -> actual == BigInteger.valueOf(expected)  // == Long
            is Float -> actual.toFloat() == expected.toFloat()  // == Float
            is Double -> actual.toDouble() == expected.toDouble()  // == Double
            is String -> actual.toString() == expected  // == String
            else -> actual == expected
          }

          // (fallback, simple comparison)
          else -> actual == expected
        }

        // if only one side is a guest value, wrap the other side as a guest value for comparison
        else -> checkEqual(
          condition,
          actual as? Value ?: asValue(actual),
          expected as? Value ?: asValue(expected),
          message,
        )
      }
    }.also {
      if (it != condition)
        throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
    }
  }

  @Polyglot override fun equal(actual: Any?, expected: Any?, message: String?) {
    checkEqual(true, actual, expected, message)
  }

  @Polyglot override fun strict(actual: Any?, expected: Any?, message: String?) {
    when {
      actual == null && expected == null -> {}
      actual == null || expected == null ->
        throw assertionError(message, actualValue = empty(), expectedValue = ofNullable(expected))

      // two guest objects
      actual is Value && expected is Value -> when {
        actual.isNull && expected.isNull -> {}
        actual.isNull || expected.isNull ->
          throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))

        actual.isBoolean && expected.isBoolean -> if (actual.asBoolean() != expected.asBoolean()) {
          throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }

        (actual.isBoolean && !expected.isBoolean) || (expected.isBoolean && !actual.isBoolean) ->
          throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))

        actual.isString && expected.isString -> if (actual.asString() != expected.asString()) {
          throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }

        (actual.isString && !expected.isString) || (expected.isString && !actual.isString) -> {
          throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }

        actual.isNumber -> when {
          actual.fitsInShort() && expected.fitsInShort() -> {
            if (actual.asShort() != expected.asShort()) {
              throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
            }
          }

          actual.fitsInInt() && expected.fitsInInt() -> {
            if (actual.asInt() != expected.asInt()) {
              throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
            }
          }

          actual.fitsInLong() && expected.fitsInLong() -> {
            if (actual.asLong() != expected.asLong()) {
              throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
            }
          }

          actual.fitsInFloat() && expected.fitsInFloat() -> {
            if (actual.asFloat() != expected.asFloat()) {
              throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
            }
          }

          actual.fitsInDouble() && expected.fitsInDouble() -> {
            if (actual.asDouble() != expected.asDouble()) {
              throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
            }
          }

          actual.fitsInBigInteger() && expected.fitsInBigInteger() -> {
            if (actual.asBigInteger() != expected.asBigInteger()) {
              throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
            }
          }

          else -> if (actual != expected) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }
        }

        else -> if (actual != expected) {
          throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }
      }

      // two host objects
      actual !is Value && expected !is Value -> when {
        actual is Short -> when (expected) {
          is Short -> if (actual != expected) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Int -> if (actual.toInt() != expected.toInt()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Long -> if (actual.toLong() != expected.toLong()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Float -> if (actual.toFloat() != expected.toFloat()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Double -> if (actual.toDouble() != expected.toDouble()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          else -> throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }

        actual is Int -> when (expected) {
          is Int -> if (actual != expected) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Short -> if (actual != expected.toInt()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Long -> if (actual != expected.toInt()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Float -> if (actual != expected.toInt()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Double -> if (actual != expected.toInt()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          else -> throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }

        actual is Long -> when (expected) {
          is Long -> if (actual != expected) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Short -> if (actual != expected.toLong()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Int -> if (actual != expected.toLong()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Float -> if (actual != expected.toLong()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Double -> if (actual != expected.toLong()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          else -> throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }

        actual is Float -> when (expected) {
          is Float -> if (actual != expected) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Short -> if (actual != expected.toFloat()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Int -> if (actual != expected.toFloat()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Long -> if (actual != expected.toFloat()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Double -> if (actual != expected.toFloat()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          else -> throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }

        actual is Double -> when (expected) {
          is Double -> if (actual != expected) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Short -> if (actual != expected.toDouble()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Int -> if (actual != expected.toDouble()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Long -> if (actual != expected.toDouble()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          is Float -> if (actual != expected.toDouble()) {
            throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
          }

          else -> throw assertionError(message, actualValue = ofNullable(actual), expectedValue = ofNullable(expected))
        }

        else -> (expected == actual).let {
          if (!it) throw assertionError(
            message,
            actualValue = ofNullable(actual),
            expectedValue = ofNullable(expected),
          )
        }
      }

      // otherwise, guest-ify if needed
      else -> strict(
        actual = if (actual is Value) actual else asValue(actual),
        expected = if (expected is Value) expected else asValue(expected),
        message = message,
      )
    }
  }

  @Polyglot override fun notEqual(actual: Any?, expected: Any?, message: String?) {
    checkEqual(false, actual, expected, message)
  }

  @Polyglot override fun deepEqual(actual: Any?, expected: Any?, message: String?) {
    TODO("Not yet implemented: `assert.deepEqual`")
  }

  @Polyglot override fun notDeepEqual(actual: Any?, expected: Any?, message: String?) {
    TODO("Not yet implemented: `assert.notDeepEqual`")
  }

  @Polyglot override fun deepStrictEqual(actual: Any?, expected: Any?, message: String?) {
    TODO("Not yet implemented: `assert.deepStrictEqual`")
  }

  @Polyglot override fun notDeepStrictEqual(actual: Any?, expected: Any?, message: String?) {
    TODO("Not yet implemented: `assert.notDeepStrictEqual`")
  }

  @Polyglot override fun match(string: String, regexp: Regex, message: String?) {
    if (!regexp.matches(string)) {
      throw assertionError(message, actualValue = of(string), expectedValue = of(regexp))
    }
  }

  private fun polyglotRegexMatch(shouldMatch: Boolean, string: String, regexp: Value, message: String?) {
    if (!regexp.hasMember("exec")) throw assertionError("Expected `RegExp` with `exec`")
    val match = regexp.getMember("exec")
    if (match.isNull) error("Unexpected missing `exec` function")
    if (!match.canExecute()) throw assertionError("Cannot execute `RegExp.exec`")
    val doesMatch = !match.execute(string).isNull
    if (shouldMatch != doesMatch) {
      throw assertionError(message, actualValue = of(string), expectedValue = of(regexp))
    }
  }

  @Polyglot override fun match(string: String, regexp: Value, message: String?) {
    polyglotRegexMatch(true, string, regexp, message)
  }

  @Polyglot override fun doesNotMatch(string: String, regexp: Regex, message: String?) {
    if (regexp.matches(string)) {
      throw assertionError(message, actualValue = of(string), expectedValue = of(regexp))
    }
  }

  @Polyglot override fun doesNotMatch(string: String, regexp: Value, message: String?) {
    polyglotRegexMatch(false, string, regexp, message)
  }

  private fun runWithCapturedExceptions(expectThrows: Boolean, fn: Any?, error: Any?, message: String?) {
    val didThrow = AtomicBoolean(false)
    val exc = AtomicReference<Throwable>(null)

    when (fn) {
      is Function<*, *> -> try {
        @Suppress("UNCHECKED_CAST")
        (fn as Function<Any?, Any?>).apply(null)
      } catch (e: Throwable) {
        didThrow.set(true)
        exc.set(e)
      }

      is Value -> {
        if (!fn.canExecute()) {
          throw assertionError("Expected a function, but got: $fn (not executable)")
        }
        try {
          fn.execute()
        } catch (e: Throwable) {
          didThrow.set(true)
          exc.set(e)
        }
      }

      else -> throw assertionError("Expected a function, but got: $fn")
    }

    if (didThrow.get() && !expectThrows) {
      if (error != null) {
        val err = exc.get()
        if (err::class.java == error) {
          throw assertionError(message, actualValue = of(err), expectedValue = of(err))
        }
      }

      throw assertionError(message, actualValue = of(exc.get()))
    } else if (!didThrow.get() && expectThrows) {
      throw assertionError(message, actualValue = ofNullable(null), expectedValue = of("an error"))
    }
  }

  private fun runAsyncWithCapturedExceptions(expectRejects: Boolean, fn: Any?, error: Any?, message: String?) {
    val didReject = AtomicBoolean(false)
    val exc = AtomicReference<Throwable>(null)

    val out = when (fn) {
      is Function<*, *> -> try {
        @Suppress("UNCHECKED_CAST")
        (fn as Function<Any?, Any?>).apply(null)
      } catch (e: Throwable) {
        throw assertionError(
          "Expected async function to execute and produce a Promise, but it threw instead",
          actualValue = of(e),
        )
      }

      is Value -> {
        if (!fn.canExecute()) {
          throw assertionError("Expected a function, but got: $fn (not executable)")
        }
        try {
          fn.execute()
        } catch (e: Throwable) {
          throw assertionError(
            "Expected async foreign function to execute and produce a Promise, but it threw instead",
            actualValue = of(e),
          )
        }
      }

      else -> throw assertionError("Expected a function, but got: $fn")
    }

    // `out` should be a promise
    when {
      out is JsPromise<*> -> {
        out.then({}, { didReject.set(true) })
      }

      out is Value -> {
        if (!out.hasMember("then")) {
          throw assertionError("Expected a Promise, but got: $out")
        }
        val then = out.getMember("then")
        if (!then.canExecute()) {
          throw assertionError("Expected a Promise, but got: $out")
        }
        try {
          then.execute()
        } catch (e: Throwable) {
          didReject.set(true)
          exc.set(e)
        }
      }

      else -> throw assertionError("Expected a Promise, but got: $out")
    }

    if (didReject.get() && !expectRejects) {
      if (error != null) {
        throw assertionError(message, actualValue = ofNullable(null), expectedValue = of("an error"))
      }
    } else if (!didReject.get() && expectRejects) {
      throw assertionError(message, actualValue = ofNullable(null), expectedValue = of("an error"))
    }
  }

  @Polyglot override fun throws(error: Any?, message: String?, fn: () -> Unit) {
    try {
      fn()
    } catch (e: Throwable) {
      if (error != null) {
        if (e::class.java != error) {
          throw assertionError(message, actualValue = of(e), expectedValue = of(e))
        }
      }
      return
    }
    throw assertionError(
      "Expected fn to throw, but no error was thrown",
      actualValue = ofNullable(null),
      expectedValue = of("an error"),
    )
  }

  @Polyglot override fun throws(fn: Any, error: Any?, message: String?) {
    runWithCapturedExceptions(
      true,
      fn,
      error,
      message ?: "Expected fn to throw, but no error was thrown",
    )
  }

  @Polyglot override fun doesNotThrow(fn: Any, error: Any?, message: String?) {
    runWithCapturedExceptions(
      false,
      fn,
      error,
      message ?: "Expected fn not to throw, but an error was thrown",
    )
  }

  @Polyglot override fun doesNotThrow(error: Any?, message: String?, fn: () -> Unit) {
    try {
      fn()
    } catch (e: Throwable) {
      if (error != null) {
        if (e::class.java == error) {
          throw assertionError(message, actualValue = of(e), expectedValue = of(e))
        }
      }
      throw assertionError(message, actualValue = of(e))
    }
  }

  @Polyglot override fun rejects(asyncFn: Any, error: Any?, message: String?) {
    runAsyncWithCapturedExceptions(true, asyncFn, error, message)
  }

  @Polyglot override fun doesNotReject(asyncFn: Any, error: Any?, message: String?) {
    runAsyncWithCapturedExceptions(false, asyncFn, error, message)
  }
}
