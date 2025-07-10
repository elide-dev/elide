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
  private val codec = PercentCodec(ADDITIONAL_ENCODE_CHARS, false)

  private fun valueToString(value: Value): String = when {
    value.isString -> value.asString()
    value.isNull -> "null"
    value.isNumber -> value.toString()
    value.isBoolean -> value.toString()
    value.hasMembers() -> {
      sequenceOf("toString", "valueOf")
        .mapNotNull { methodName ->
          value.getMember(methodName)
            ?.takeIf { it.canExecute() }
            ?.runCatching { execute().asString() }
            ?.getOrNull()
        }
        .firstOrNull()
        ?: throw JsError.typeError("Cannot convert object to string")
    }

    else -> throw JsError.typeError("Cannot convert value to string")
  }

  internal companion object {
    @JvmStatic fun create(): NodeQuerystring = NodeQuerystring()
  }

  override fun getMemberKeys(): Array<String> = moduleMembers
  override fun getMember(key: String?): Any? = when (key) {
    QS_DECODE -> ProxyExecutable { args ->
      when (args.size) {
        1 -> decode(args.first().toString(), null, null, null)
        2 -> decode(args.first().toString(), args[1].toString(), null, null)
        3 -> decode(args.first().toString(), args[1].toString(), args[2].toString(), null)
        4 -> decode(args.first().toString(), args[1].toString(), args[2].toString(), args[3])
        else -> throw JsError.typeError("Invalid number of arguments to `querystring.decode`")
      }
    }

    QS_ENCODE -> ProxyExecutable { args ->
      when (args.size) {
        1 -> encode(args[0], null, null, null)
        2 -> encode(args[0], args[1].toString(), null, null)
        3 -> encode(args[0], args[1].toString(), args[2].toString(), null)
        4 -> encode(args[0], args[1].toString(), args[2].toString(), args[3])
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
        1 -> parse(args[0].toString(), null, null, null)
        2 -> parse(args[0].toString(), args[1].toString(), null, null)
        3 -> parse(args[0].toString(), args[1].toString(), args[2].toString(), null)
        4 -> parse(args[0].toString(), args[1].toString(), args[2].toString(), args[3])
        else -> throw JsError.typeError("Invalid number of arguments to `querystring.parse`")
      }
    }

    QS_STRINGIFY -> ProxyExecutable { args ->
      when (args.size) {
        1 -> stringify(args[0], null, null, null)
        2 -> stringify(args[0], args[1].toString(), null, null)
        3 -> stringify(args[0], args[1].toString(), args[2].toString(), null)
        4 -> stringify(args[0], args[1].toString(), args[2].toString(), args[3])
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
    str: String,
    sep: String?,
    eq: String?,
    options: Value?
  ): Value = parse(str, sep, eq, options)

  override fun encode(
    obj: Value,
    sep: String?,
    eq: String?,
    options: Value?
  ): String = stringify(obj, sep, eq, options)

  override fun escape(str: Value): String {
    val bytes = valueToString(str).toByteArray(StandardCharsets.UTF_8)
    return codec.encode(bytes).toString(Charsets.UTF_8)
  }


  override fun parse(
    str: String,
    sep: String?,
    eq: String?,
    options: Value?
  ): Value {
    TODO("Not yet implemented")
  }

  override fun stringify(
    obj: Value,
    sep: String?,
    eq: String?,
    options: Value?
  ): String {
    TODO("Not yet implemented")
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
