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
package elide.runtime.node.querystring

import org.apache.commons.codec.net.PercentCodec
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.nio.charset.StandardCharsets
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.QuerystringAPI
import elide.runtime.intrinsics.js.node.querystring.ParseOptions
import elide.runtime.intrinsics.js.node.querystring.QueryParams
import elide.runtime.intrinsics.js.node.querystring.StringifyOptions
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.querystring.QueryParams.Companion.of
import elide.runtime.intrinsics.js.node.querystring.ReadOnlyArrayProxy

// Names of `querystring` methods.
private const val QS_DECODE = "decode"
private const val QS_ENCODE = "encode"
private const val QS_ESCAPE = "escape"
private const val QS_PARSE = "parse"
private const val QS_STRINGIFY = "stringify"
private const val QS_UNESCAPE = "unescape"

private val moduleMembers = arrayOf(
  QS_DECODE,
  QS_ENCODE,
  QS_ESCAPE,
  QS_PARSE,
  QS_STRINGIFY,
  QS_UNESCAPE,
)

// Installs the Node query-string module into the intrinsic bindings.
@Intrinsic internal class NodeQuerystringModule : AbstractNodeBuiltinModule() {
  private val querystring by lazy { NodeQuerystring.create() }

  fun provide(): NodeQuerystring = querystring

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.QUERYSTRING)) { querystring }
  }
}

/**
 * # Node API: `querystring`
 */
internal class NodeQuerystring : ReadOnlyProxyObject, QuerystringAPI {

  internal companion object {
    // Characters that Node.js querystring.escape() encodes beyond non-ASCII and %
    private val ADDITIONAL_ENCODE_CHARS = charArrayOf(
      // RFC 3986 reserved characters that Node.js encodes
      ' ', ':', '/', '?', '#', '[', ']', '@', '$', '&', '+', ',', ';', '=',

      // Other characters that Node.js encodes
      '"', '<', '>', '\\', '^', '`', '{', '|', '}',
    ).map { it.code.toByte() }.toByteArray()

    private const val HEX_RADIX = 16
    
    /** Regex pattern for matching percent-encoded sequences in URL unescape fallback. */
    private val PERCENT_ENCODED_PATTERN = Regex("%([0-9A-Fa-f]{2})")

    @JvmStatic fun create(): NodeQuerystring = NodeQuerystring()
  }

  // PercentCodec configured for querystring behavior
  // - Encodes additional characters specified above
  private val codec = PercentCodec(ADDITIONAL_ENCODE_CHARS, true)

  private fun valueToString(value: Value): String {
    return when {
      value.isString -> value.asString()
      value.isNull -> "null"
      value.isNumber -> value.toString()
      value.isBoolean -> value.toString()
      value.hasArrayElements() -> {
        // Handle arrays - convert to JavaScript-like string representation
        (0 until value.arraySize).joinToString(",") { index -> valueToString(value.getArrayElement(index)) }
      }
      value.hasMembers() -> {
        sequenceOf("toString", "valueOf")
          .mapNotNull { methodName ->
            value.getMember(methodName)
              ?.takeIf { it.canExecute() }
              ?.runCatching { execute().asString() }
              ?.getOrNull()
          }
          .firstOrNull()
          ?: throw JsError.typeError("Cannot convert object to primitive value")
      }
      value.isHostObject -> {
        val hostObject = value.asHostObject<Any>()
        when (hostObject) {
          is Array<*> -> {
            hostObject.joinToString(",") { element -> valueToString(Value.asValue(element)) }
          }
          is List<*> -> {
            hostObject.joinToString(",") { element -> valueToString(Value.asValue(element))}
          }
          else -> "[object Object]"
        }
      }

      else -> "[object Object]"
    }
  }

  override fun getMemberKeys(): Array<String> = moduleMembers
  override fun getMember(key: String?): Any? = when (key) {
    QS_DECODE -> ProxyExecutable { args ->
      if (args.isEmpty()) throw JsError.typeError("Invalid number of arguments to `querystring.decode`")
      decode(args[0], args.getOrNull(1), args.getOrNull(2), args.getOrNull(3))
    }

    QS_ENCODE -> ProxyExecutable { args ->
      if (args.isEmpty()) throw JsError.typeError("Invalid number of arguments to `querystring.encode`")
      encode(args[0], args.getOrNull(1), args.getOrNull(2), args.getOrNull(3))
    }

    QS_ESCAPE -> ProxyExecutable { args ->
      if (args.isEmpty()) throw JsError.typeError("Invalid number of arguments to `querystring.escape`")
      escape(args[0])
    }

    QS_PARSE -> ProxyExecutable { args ->
      if (args.isEmpty()) throw JsError.typeError("Invalid number of arguments to `querystring.parse`")
      parse(args[0], args.getOrNull(1), args.getOrNull(2), args.getOrNull(3))
    }

    QS_STRINGIFY -> ProxyExecutable { args ->
      if (args.isEmpty()) throw JsError.typeError("Invalid number of arguments to `querystring.stringify`")
      stringify(args[0], args.getOrNull(1), args.getOrNull(2), args.getOrNull(3))
    }

    QS_UNESCAPE -> ProxyExecutable { args ->
      if (args.isEmpty()) throw JsError.typeError("Invalid number of arguments to `querystring.unescape`")
      unescape(args[0])
    }

    else -> null
  }

  override fun decode(
    str: Value,
    sep: Value?,
    eq: Value?,
    options: Value?
  ): QueryParams = parse(str, sep, eq, options)

  override fun encode(
    obj: Value,
    sep: Value?,
    eq: Value?,
    options: Value?
  ): String = stringify(obj, sep, eq, options)

  override fun escape(str: Value): String {
    val bytes = valueToString(str).toByteArray(StandardCharsets.UTF_8)
    return codec.encode(bytes).toString(Charsets.UTF_8)
  }

  override fun parse(
    str: Value,
    sep: Value?,
    eq: Value?,
    options: Value?
  ): QueryParams {


    val string = valueToString(str)
    val separator = sep?.let(::valueToString) ?: "&"
    val equals = eq?.let(::valueToString) ?: "="
    val parseOptions = ParseOptions.fromGuest(options)

    val result = mutableMapOf<String, Any>()

    if (string.isBlank()) {
      return of(result)
    }

    var keyCount = 0
    string.splitToSequence(separator)
      .filter { it.isNotEmpty() }
      .takeWhile { keyCount < parseOptions.maxKeys }
      .forEach { pair ->
        val (rawKey, rawValue) = pair.split(equals, limit = 2)
          .let { parts -> parts[0] to parts.getOrElse(1) { "" } }

        val decoder = parseOptions.decodeURIComponent
        val key = decoder?.execute(Value.asValue(rawKey))?.asString() ?: unescape(Value.asValue(rawKey))
        val value = decoder?.execute(Value.asValue(rawValue))?.asString() ?: unescape(Value.asValue(rawValue))

        when (val existing = result[key]) {
          null -> {
            result[key] = value
            keyCount++
          }

          is MutableList<*> -> {
            @Suppress("UNCHECKED_CAST")
            (existing as MutableList<String>).add(value)
          }

          is String -> {
            result[key] = mutableListOf(existing, value)
          }
        }
      }

    return of(result)
  }

  override fun stringify(
    obj: Value,
    sep: Value?,
    eq: Value?,
    options: Value?
  ): String {
    val separator = sep?.let(::valueToString) ?: "&"
    val equals = eq?.let(::valueToString) ?: "="
    val stringifyOptions = StringifyOptions.fromGuest(options)
    val result = mutableListOf<String>()

    when {
      obj.isHostObject -> {
        // Handle host objects directly without QueryParams conversion
        val hostObject = obj.asHostObject<Any>()
        if (hostObject is Map<*, *>) {
          hostObject.forEach { (key, value) ->
            if (key is String && value != null) {
              appendKeyValuePairs(result, key, Value.asValue(value), equals, stringifyOptions)
            }
          }
        } else {
          // Non-map host objects should return empty string
          return ""
        }
      }

      else -> {
        // Use QueryParams.fromGuest for guest objects
        val queryParams = QueryParams.fromGuest(obj)
        if (queryParams != null) {
          queryParams.memberKeys.forEach { key ->
            val value = queryParams.getMember(key)
            appendKeyValuePairs(result, key, value, equals, stringifyOptions)
          }
        } else {
          // Node.js returns empty string for non-objects (primitives, null, etc.)
          return ""
        }
      }
    }

    return result.joinToString(separator)
  }

  private fun encodeValue(value: Any?, encoder: Value?): String {
    return when (value) {
      null -> ""
      else -> encoder?.execute(Value.asValue(value))?.asString() ?: escape(Value.asValue(value))
    }
  }

  private fun appendKeyValuePairs(
    result: MutableList<String>,
    key: String,
    value: Any?,
    equals: String,
    options: StringifyOptions
  ) {
    val encoder = options.encodeURIComponent
    val encodedKey = encoder?.execute(Value.asValue(key))?.asString() ?: escape(Value.asValue(key))

    when (value) {
      is ReadOnlyArrayProxy -> {
        for (index in 0 until value.size) {
          val encodedValue = encodeValue(value.get(index), encoder)
          result += "$encodedKey$equals$encodedValue"
        }
      }

      is Value -> {
        when {
          value.isNull -> result += "null"
          value.isHostObject -> {
            val hostObject = value.asHostObject<Any>()
            if (hostObject is Array<*>) {
              hostObject.forEach { element ->
                val encodedValue = encodeValue(element, encoder)
                result += "$encodedKey$equals$encodedValue"
              }
            }
          }
          else -> {
            val encodedValue = encodeValue(valueToString(value), encoder)
            result += "$encodedKey$equals$encodedValue"
          }
        }
      }

      else -> {
        val encodedValue = encodeValue(value.toString(), encoder)
        result += "$encodedKey$equals$encodedValue"
      }
    }
  }

  override fun unescape(str: Value): String {
    val input = valueToString(str)

    return runCatching {
      val bytes = input.toByteArray(StandardCharsets.UTF_8)
      codec.decode(bytes).toString(Charsets.UTF_8)
    }.getOrElse {
      // Fallback: manually decode percent-encoded sequences
      input.replace(PERCENT_ENCODED_PATTERN) { match ->
        when (val charCode = match.groupValues[1].toIntOrNull(HEX_RADIX)) {
          null -> match.value   // Keep original if hex is invalid
          else -> charCode.toChar().toString()
        }
      }
    }
  }
}
