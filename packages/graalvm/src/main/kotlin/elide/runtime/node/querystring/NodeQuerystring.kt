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
import org.graalvm.polyglot.proxy.ProxyObject
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

  // Characters that Node.js querystring.escape() encodes beyond non-ASCII and %
  // These are the characters NOT in the Node.js whitelist: A-Z a-z 0-9 - _ . ! ~ * ' ( )
  private val ADDITIONAL_ENCODE_CHARS = byteArrayOf(
    // RFC 3986 reserved characters that Node.js encodes
    ' '.code.toByte(),   // %20
    ':'.code.toByte(),   // %3A
    '/'.code.toByte(),   // %2F
    '?'.code.toByte(),   // %3F
    '#'.code.toByte(),   // %23
    '['.code.toByte(),   // %5B
    ']'.code.toByte(),   // %5D
    '@'.code.toByte(),   // %40
    '$'.code.toByte(),   // %24
    '&'.code.toByte(),   // %26
    '+'.code.toByte(),   // %2B
    ','.code.toByte(),   // %2C
    ';'.code.toByte(),   // %3B
    '='.code.toByte(),   // %3D

    // Other characters that Node.js encodes
    '"'.code.toByte(),   // %22
    '<'.code.toByte(),   // %3C
    '>'.code.toByte(),   // %3E
    '\\'.code.toByte(),  // %5C
    '^'.code.toByte(),   // %5E
    '`'.code.toByte(),   // %60
    '{'.code.toByte(),   // %7B
    '|'.code.toByte(),   // %7C
    '}'.code.toByte(),    // %7D
  )

  // PercentCodec configured for querystring behavior
  // - Encodes additional characters specified above
  private val codec = PercentCodec(ADDITIONAL_ENCODE_CHARS, true)

  private fun valueToString(value: Value): String {
    println("DEBUG: valueToString called with: $value")
    println("DEBUG: value.isString = ${value.isString}")
    println("DEBUG: value.isNull = ${value.isNull}")
    println("DEBUG: value.isNumber = ${value.isNumber}")
    println("DEBUG: value.isBoolean = ${value.isBoolean}")
    println("DEBUG: value.hasArrayElements() = ${value.hasArrayElements()}")
    println("DEBUG: value.isHostObject = ${value.isHostObject}")
    println("DEBUG: value.hasMembers() = ${value.hasMembers()}")



    return when {
      value.isString -> value.asString()
      value.isNull -> "null"
      value.isNumber -> value.toString()
      value.isBoolean -> value.toString()
      value.hasArrayElements() -> {
        // Handle arrays - convert to JavaScript-like string representation
        (0 until value.arraySize).joinToString(",") { index -> valueToString(value.getArrayElement(index)) }
      }

      !value.hasMembers() -> "[object Object]"
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

      else -> {
        println(value.toString())
        throw JsError.typeError("Cannot convert object to primitive value")
      }
    }
  }

  internal companion object {
    @JvmStatic fun create(): NodeQuerystring = NodeQuerystring()
  }

  override fun getMemberKeys(): Array<String> = moduleMembers
  override fun getMember(key: String?): Any? = when (key) {
    QS_DECODE -> ProxyExecutable { args ->
      when (args.size) {
        1 -> decode(args.first(), null, null, null)
        2 -> decode(args.first(), args[1], null, null)
        3 -> decode(args.first(), args[1], args[2], null)
        4 -> decode(args.first(), args[1], args[2], args[3])
        else -> throw JsError.typeError("Invalid number of arguments to `querystring.decode`")
      }
    }

    QS_ENCODE -> ProxyExecutable { args ->
      when (args.size) {
        1 -> encode(args[0], null, null, null)
        2 -> encode(args[0], args[1], null, null)
        3 -> encode(args[0], args[1], args[2], null)
        4 -> encode(args[0], args[1], args[2], args[3])
        else -> throw JsError.typeError("Invalid number of arguments to `querystring.encode`")
      }
    }

    QS_ESCAPE -> ProxyExecutable { args ->
      when (args.size) {
        1 -> escape(args.first())
        else -> throw JsError.typeError("Invalid number of arguments to `querystring.escape`")
      }
    }

    QS_PARSE -> ProxyExecutable { args ->
      when (args.size) {
        1 -> parse(args.first(), null, null, null)
        2 -> parse(args.first(), args[1], null, null)
        3 -> parse(args.first(), args[1], args[2], null)
        4 -> parse(args.first(), args[1], args[2], args[3])
        else -> throw JsError.typeError("Invalid number of arguments to `querystring.parse`")
      }
    }

    QS_STRINGIFY -> ProxyExecutable { args ->
      when (args.size) {
        1 -> stringify(args[0], null, null, null)
        2 -> stringify(args[0], args[1], null, null)
        3 -> stringify(args[0], args[1], args[2], null)
        4 -> stringify(args[0], args[1], args[2], args[3])
        else -> throw JsError.typeError("Invalid number of arguments to `querystring.stringify`")
      }
    }

    QS_UNESCAPE -> ProxyExecutable { args ->
      when (args.size) {
        1 -> unescape(args[0])
        else -> throw JsError.typeError("Invalid number of arguments to `querystring.unescape`")
      }
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
      return QueryParams.of(result)
    }

    var keyCount = 0
    string.split(separator)
      .asSequence()
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

    println("result type: ${result::class}")
    println("result contents: $result")
    val proxy = QueryParams.of(result)
    println("proxy.hasMembers(): ${proxy.getMember("__proto__")}")
    println("proxy type: ${proxy::class}")
    val wrapped = Value.asValue(proxy)
    println("wrapped type: ${wrapped::class}")
    println("wrapped.hasMembers(): ${wrapped.hasMembers()}")

    return proxy
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
      obj.hasMembers() -> {
        obj.memberKeys.forEach { key ->
          val value = obj.getMember(key)
          appendKeyValuePairs(result, key, value, equals, stringifyOptions)
        }
      }

      else -> {
        // Node.js returns empty string for non-objects (Maps, etc.)
        return ""
      }
    }

    return result.joinToString(separator)
  }

  private fun appendKeyValuePairs(
    result: MutableList<String>,
    key: String,
    value: Value?,
    equals: String,
    options: StringifyOptions
  ) {
    if (value?.isNull != false) return

    val encoder = options.encodeURIComponent
    val encodedKey = encoder?.execute(Value.asValue(key))?.asString() ?: escape(Value.asValue(key))

    when {
      value.hasArrayElements() -> {
        (0 until value.arraySize)
          .asSequence()
          .mapNotNull { value.getArrayElement(it) }
          .filterNot { it.isNull }
          .map { element ->
            val encodedValue = encoder?.execute(element)?.asString() ?: escape(element)
            "$encodedKey$equals$encodedValue"
          }
          .forEach { result += it }
      }

      else -> {
        val stringValue = when {
          value.isString -> value.asString()
          value.isNumber -> value.toString()
          value.isBoolean -> value.toString()
          else -> valueToString(value)
        }
        val encodedValue =
          encoder?.execute(Value.asValue(stringValue))?.asString() ?: escape(Value.asValue(stringValue))
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
      input.replace(Regex("%([0-9A-Fa-f]{2})")) { match ->
        when (val charCode = match.groupValues[1].toIntOrNull(16)) {
          null -> match.value   // Keep original if hex is invalid
          else -> charCode.toChar().toString()
        }
      }
    }
  }
}
