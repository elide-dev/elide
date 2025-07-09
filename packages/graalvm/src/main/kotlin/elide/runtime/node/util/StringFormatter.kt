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
@file:Suppress("NOTHING_TO_INLINE")

package elide.runtime.node.util

import org.graalvm.polyglot.Value
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.UtilAPI
import elide.runtime.intrinsics.js.node.util.InspectOptionsAPI

// Implements string formatting for `util.format`, based on simple logic and `util.inspect`.
internal object StringFormatter {
  // Constants used in formatting.
  private const val FORMAT_SIGNAL = '%'
  private const val FORMAT_NUMBER = 'd'
  private const val FORMAT_INT = 'i'
  private const val FORMAT_FLOAT = 'f'
  private const val FORMAT_STRING = 's'
  private const val FORMAT_JSON = 'j'
  private const val FORMAT_OBJECT_DEFAULTS = 'o'
  private const val FORMAT_OBJECT = 'O'
  private const val FORMAT_CSS = 'c'

  // Rendering context for inspection.
  private interface FormatContext : Appendable, CharSequence {
    fun utils(): UtilAPI
    fun options(): InspectOptionsAPI
    fun styles(): InspectStyling = options().styles
  }

  // Component renderers based on char identity.
  private sealed class ComponentFormatter<T: Any> (
    val typeAndPosition: Pair<Char, UInt>,
    val value: Any?,
  ) {
    fun format(builder: StringBuilder): Unit = format(builder, cast())
    abstract fun cast(): T?
    abstract fun format(builder: StringBuilder, value: T?)
  }

  // Implements `%s` (formatting as a string).
  private class StringComponentFormatter(info: Pair<Char, UInt>, value: Any?): ComponentFormatter<String>(info, value) {
    override fun cast(): String? = value.toString()

    override fun format(builder: StringBuilder, value: String?) {
      builder.append(value ?: "null")
    }
  }

  // Implements `%i` (formatting as an integer).
  private class IntComponentFormatter(info: Pair<Char, UInt>, value: Any?): ComponentFormatter<Int>(info, value) {
    override fun cast(): Int? = when (val inner = value) {
      null -> null
      is Int -> value
      is Float -> value.toInt()
      is Double -> value.toInt()
      is Long -> value.toInt()
      is Number -> value.toInt()
      is Value -> when {
        inner.isNumber -> when {
          inner.fitsInInt() -> inner.asInt()
          inner.fitsInLong() -> inner.asLong().toInt()
          inner.fitsInDouble() -> inner.asDouble().toInt()
          inner.fitsInFloat() -> inner.asFloat().toInt()
          else -> null
        }
        else -> null
      }
      else -> null
    }.also {
      it ?: error("Cannot cast value to Int: $value")
    }

    override fun format(builder: StringBuilder, value: Int?) {
      builder.append(value.toString())
    }
  }

  // Implements `%f` (formatting as a float).
  private class FloatComponentFormatter(info: Pair<Char, UInt>, value: Any?): ComponentFormatter<Float>(info, value) {
    override fun cast(): Float? = when (val inner = value) {
      null -> null
      is Float -> value
      is Int -> value.toFloat()
      is Double -> value.toFloat()
      is Long -> value.toFloat()
      is Number -> value.toFloat()
      is Value -> when {
        inner.isNumber -> when {
          inner.fitsInInt() -> inner.asInt().toFloat()
          inner.fitsInLong() -> inner.asLong().toFloat()
          inner.fitsInDouble() -> inner.asDouble().toFloat()
          inner.fitsInFloat() -> inner.asFloat().toFloat()
          else -> null
        }
        else -> null
      }
      else -> null
    }.also {
      it ?: error("Cannot cast value to Float: $value")
    }

    override fun format(builder: StringBuilder, value: Float?) {
      builder.append(value.toString())
    }
  }

  // Implements `%d` (formatting as a float or int based on identity).
  private sealed interface IntOrDouble {
    companion object {
      fun from(value: Any?): IntOrDouble = when (val inner = value) {
        null -> null
        is Int -> IntValue(inner)
        is Double -> DoubleValue(inner)
        is Float -> DoubleValue(inner.toDouble())
        is Long -> IntValue(inner.toInt())
        is Value -> when {
          inner.isNumber -> when {
            inner.fitsInInt() -> IntValue(inner.asInt())
            inner.fitsInLong() -> IntValue(inner.asLong().toInt())
            inner.fitsInDouble() -> DoubleValue(inner.asDouble())
            inner.fitsInFloat() -> DoubleValue(inner.asFloat().toDouble())
            else -> null
          }
          else -> null
        }
        else -> null
      } ?: error("Cannot cast value to IntOrDouble: $value")
    }
  }

  @JvmInline private value class IntValue(val value: Int) : IntOrDouble {
    override fun toString(): String = value.toString()
  }

  @JvmInline private value class DoubleValue(val value: Double) : IntOrDouble {
    override fun toString(): String = value.toString()
  }

  private class NumberComponentFormatter(info: Pair<Char, UInt>, value: Any?): ComponentFormatter<IntOrDouble>(
    info,
    value,
  ) {
    override fun cast(): IntOrDouble? = IntOrDouble.from(value)

    override fun format(builder: StringBuilder, value: IntOrDouble?) {
      builder.append(value.toString())
    }
  }

  // Implements stateful rendering for `util.format`.
  private class StringFormatterImpl(
    private val format: String,
    private val utils: UtilAPI,
    private val builder: StringBuilder,
    private val options: InspectOptionsAPI,
  ) : FormatContext, Appendable by builder, CharSequence by builder {
    override fun utils(): UtilAPI = utils
    override fun options(): InspectOptionsAPI = options
    override fun styles(): InspectStyling = options.styles

    private inline fun fail(message: String): Nothing {
      throw JsError.of("Format error: $message")
    }

    // Consume an argument from `args` at the specified position, or fail.
    private fun consumeArgAt(args: List<Any?>, position: UInt): Any? {
      if (position.toInt() >= args.size) fail(
        "Not enough format arguments provided: expected at least ${position + 1u}"
      )
      return args[position.toInt()]
    }

    // Format a primitive value based on the specified type.
    private fun formatPrimitive(typeAndPosition: Pair<Char, UInt>, argValue: Any?) {
      when (typeAndPosition.first) {
        FORMAT_STRING -> StringComponentFormatter(typeAndPosition, argValue).format(builder)
        FORMAT_INT -> IntComponentFormatter(typeAndPosition, argValue).format(builder)
        FORMAT_FLOAT -> FloatComponentFormatter(typeAndPosition, argValue).format(builder)
        FORMAT_NUMBER -> NumberComponentFormatter(typeAndPosition, argValue).format(builder)
        else -> error("Unrecognized primitive format specifier: '${typeAndPosition.first}'")
      }
    }

    // Format a structured value based on the specified type, with options for JSON and defaults.
    private fun formatStructured(typeAndPosition: Pair<Char, UInt>, argValue: Any?, json: Boolean, defaults: Boolean) {
      TODO("Not yet implemented: Format structured value as string")
    }

    operator fun invoke(args: List<Any?>): StringBuilder = builder.apply {
      var lastChar: Char? = null
      var maybeSpecial = false
      var charIdx = 0
      var nextConsumedArg = 0u

      fun reset(char: Char) {
        lastChar = char
        maybeSpecial = false
      }
      fun StringBuilder.formatPrimitiveChunk(type: Char, args: List<Any?>) {
        val thisArgPosition = nextConsumedArg
        val argValue = consumeArgAt(args, thisArgPosition)
        nextConsumedArg += 1u
        formatPrimitive(type to thisArgPosition, argValue)
      }
      fun StringBuilder.formatObject(type: Char, args: List<Any?>, json: Boolean, defaults: Boolean) {
        val thisArgPosition = nextConsumedArg
        val argValue = consumeArgAt(args, thisArgPosition)
        nextConsumedArg += 1u
        formatStructured(type to thisArgPosition, argValue, json, defaults)
      }

      while (charIdx < format.length) {
        val thisChar = format[charIdx]
        charIdx += 1

        when {
          thisChar == FORMAT_SIGNAL -> when {
            // is the user emitting an escaped '%'?
            maybeSpecial && lastChar == FORMAT_SIGNAL -> {
              append(FORMAT_SIGNAL)
              reset(FORMAT_SIGNAL)
              continue
            }

            // otherwise, this is probably the beginning of a format specifier.
            else -> {
              reset(FORMAT_SIGNAL)
              maybeSpecial = true
              continue
            }
          }

          // if we saw a format signal, we need to check this character for special meaning.
          maybeSpecial -> when (thisChar) {
            // primitive formatting
            FORMAT_STRING,
            FORMAT_NUMBER,
            FORMAT_INT,
            FORMAT_CSS,
            FORMAT_FLOAT -> builder.formatPrimitiveChunk(thisChar, args)

            // object formatting
            FORMAT_JSON -> builder.formatObject(thisChar, args, json = true, defaults = false)
            FORMAT_OBJECT_DEFAULTS -> builder.formatObject(thisChar, args, json = false, defaults = true)
            FORMAT_OBJECT -> builder.formatObject(thisChar, args, json = false, defaults = false)
            else -> error("Unrecognized format specifier: '$thisChar'")
          }.also {
            reset(thisChar)
          }

          // otherwise, this is a normal character, append it.
          else -> builder.append(thisChar).also {
            reset(thisChar)
          }
        }
      }
    }
  }

  // Implements checked (host-side) `util.format` from the Node.js API.
  internal fun UtilAPI.formatString(obj: String, effective: InspectOptionsAPI, args: List<Any?>): String = buildString {
    if (obj.isNotEmpty()) {
      StringFormatterImpl(obj, this@formatString, this@buildString, effective).apply {
        this(args)
      }
    }
  }
}
