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
package elide.runtime.node.util

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyIterator
import kotlinx.collections.immutable.toImmutableMap
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.util.MIMEParams
import elide.runtime.intrinsics.js.node.util.MIMEType
import elide.vm.annotations.Polyglot

// Constants employed by `MIMEType` and `MIMEParams`.
private const val P_MIME_TYPE_TYPE = "type"
private const val P_MIME_TYPE_SUBTYPE = "subtype"
private const val P_MIME_TYPE_PARAMS = "params"
private const val P_MIME_TYPE_ESSENCE = "essence"
private const val F_MIME_PARAMS_GET = "get"
private const val F_MIME_PARAMS_HAS = "has"

// All methods and properties for `MIMEType`.
private val mimeTypeInfoPropsAndMethods = arrayOf(
  P_MIME_TYPE_TYPE,
  P_MIME_TYPE_SUBTYPE,
  P_MIME_TYPE_PARAMS,
  P_MIME_TYPE_ESSENCE,
)

// Singleton utilities for accessing MIME types.
internal object MimeTypes: MIMEType.Factory {
  override fun toString(): String = "MIMEType"

  // Implements the `MIMEParams` constructor.
  internal object Params: MIMEParams.Factory {
    override fun toString(): String = "MIMEParams"

    override fun parse(params: String): MIMEParams {
      return params.split(';').associate {
        val (key, value) = it.split('=', limit = 2)
        key.trim() to value.trim()
      }.let {
        if (it.isEmpty()) {
          MIMEParamData(emptyMap())
        } else {
          MIMEParamData(it)
        }
      }
    }

    override fun create(params: Map<String, String>): MIMEParams = MIMEParamData(params)
  }

  // Implements MIME type info.
  internal data class MIMETypeInfo internal constructor (
    override var type: String,
    override var subtype: String,
    override val params: MIMEParams? = null,
  ): MIMEType {
    override fun toString(): String = create(type, subtype, params?.toMap())
    override val essence: String get() = toString()

    override fun getMemberKeys(): Array<String> = mimeTypeInfoPropsAndMethods
    override fun hasMember(key: String): Boolean = key in mimeTypeInfoPropsAndMethods

    override fun getMember(key: String): Any? = when (key) {
      P_MIME_TYPE_TYPE -> type
      P_MIME_TYPE_SUBTYPE -> subtype
      P_MIME_TYPE_PARAMS -> params
      P_MIME_TYPE_ESSENCE -> essence
      else -> null
    }

    override fun putMember(key: String, value: Value) {
      if (!value.isString) {
        throw JsError.typeError("`MIMEType` property '$key' must be a string")
      }
      when (key) {
        P_MIME_TYPE_TYPE -> type = value.asString()
        P_MIME_TYPE_SUBTYPE -> subtype = value.asString()
        else -> throw JsError.typeError("Property '$key' is not writable on `MIMEType`")
      }
    }
  }

  // Implements MIME parameter data.
  internal class MIMEParamData internal constructor (private val map: Map<String, String>): MIMEParams {
    override fun toMap(): Map<String, String> = map.toImmutableMap()
    override fun getMemberKeys(): Array<String> = map.keys.toTypedArray()
    override fun getMember(key: String): Any? = when (key) {
      F_MIME_PARAMS_GET -> ProxyExecutable {
        (it.firstOrNull()?.takeIf { it.isString }?.asString()
          ?: throw JsError.typeError("`MIMEParams.get` key must be a string")).let {
          get(it)
        }
      }

      F_MIME_PARAMS_HAS -> ProxyExecutable {
        (it.firstOrNull()?.takeIf { it.isString }?.asString()
          ?: throw JsError.typeError("`MIMEParams.has` key must be a string")).let {
          has(it)
        }
      }
      else -> map[key]
    }

    override fun getHashSize(): Long = map.size.toLong()

    override fun hasHashEntry(key: Value): Boolean {
      if (!key.isString) {
        throw JsError.typeError("`MIMEParams` key must be a string")
      }
      return key.asString() in map
    }

    override fun getHashValue(key: Value): Any? {
      if (!key.isString) {
        throw JsError.typeError("`MIMEParams` key must be a string")
      }
      return map[key.asString()]
    }

    override fun putHashEntry(key: Value?, value: Value?) {
      throw JsError.typeError("`MIMEParams` is read-only")
    }

    override fun removeHashEntry(key: Value?): Boolean {
      throw JsError.typeError("`MIMEParams` is read-only")
    }

    override fun getHashEntriesIterator(): Any? = map.keys.iterator().let { iter ->
      object: ProxyIterator {
        override fun hasNext(): Boolean = iter.hasNext()
        override fun getNext(): Any? = iter.next()
      }
    }

    @Polyglot override fun get(key: String): String? {
      return map[key]
    }

    @Polyglot override fun has(key: String): Boolean {
      return key in map
    }
  }

  override fun parse(mimeType: String): MIMEType {
    val type = mimeType.substringBefore('/')
    val hasParams = mimeType.contains(';')
    val subtype = if (hasParams) mimeType.substringBefore(';').substringAfter('/') else mimeType.substringAfter('/')
    val params = if (!hasParams) null else mimeType.substringAfter(';').split(';').associate {
      val (key, value) = it.split('=', limit = 2)
      key.trim() to value.trim()
    }
    return MIMETypeInfo(type, subtype, params?.let { MIMEParamData(it) })
  }

  override fun create(type: String, subtype: String): MIMEType = MIMETypeInfo(type, subtype)

  /**
   * Render a well-formed MIME type string from its components.
   *
   * @param type The primary type, e.g. "text".
   * @param subtype The sub-type, e.g. "html".
   * @param params Optional parameters to include, e.g. `mapOf("charset" to "UTF-8")`.
   */
  @JvmStatic fun create(type: String, subtype: String, params: Map<String, String>? = null): String = buildString {
    append(type)
    append('/')
    append(subtype)
    if (params != null && params.isNotEmpty()) {
      append(';')
      params.entries.joinTo(this, separator = ";") { "${it.key}=${it.value}" }
    }
  }
}
