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
@file:Suppress("unused")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.intrinsics.js.url

import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import org.jetbrains.annotations.VisibleForTesting
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError.valueError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.gvm.internals.intrinsics.js.struct.map.BaseJsMultiMap
import elide.runtime.gvm.internals.intrinsics.js.struct.map.BaseMutableJsMultiMap
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.JsIterator.JsIteratorFactory
import elide.runtime.intrinsics.js.MapLike
import elide.runtime.intrinsics.js.MutableURLSearchParams
import elide.runtime.intrinsics.js.URLSearchParams
import elide.runtime.intrinsics.js.err.TypeError
import elide.vm.annotations.Polyglot
import org.graalvm.polyglot.Value as GuestValue
import elide.runtime.intrinsics.js.MutableURLSearchParams as IMutableSearchParams
import elide.runtime.intrinsics.js.URLSearchParams as IURLSearchParams

// Separator between URL parameter segments.
private const val URL_PARAMS_SEPARATOR = '&'

// Separator between keys/values of each URL parameter segment.
private const val URL_PARAM_KV_SEPARATOR = '='

/**
 * Parse a string of (presumed) query parameters to a list of pairs; each pair represents a key-value pair.
 *
 * Strings which are empty or otherwise invalid are skipped.
 * This method is used when parsing data from guests, so care must be taken to ensure that the input is safe.
 *
 * @param params The string of query parameters to parse.
 * @return A list of key-value pairs representing the parsed query parameters.
 */
@VisibleForTesting
internal fun parseParamsToMultiMap(params: String): Sequence<Pair<String, String>> = sequence {
  var currentKey: StringBuilder = StringBuilder()
  var currentValue: StringBuilder = StringBuilder()
  var kvSplitSeen = false
  var emptyKeyIgnoreValue = false

  val reset = {
    // reset the key and value buffers.
    currentKey = StringBuilder()
    currentValue = StringBuilder()
    kvSplitSeen = false
    emptyKeyIgnoreValue = false
  }

  val appendTo: (StringBuilder, Char) -> Unit = { buffer, char ->
    // if character is out of UTF-8 range, skip it, otherwise append.
    if (char.code < 0x7F) buffer.append(char)
  }

  // step character-wise through the string, gathering at `currentKey` and `currentValue` until `&` is encountered.
  for (char in params) {
    when (char) {
      URL_PARAMS_SEPARATOR -> {
        // yield the current key-value pair, but only with a valid key. values can be empty strings, but keys cannot.
        if (currentKey.isNotEmpty()) yield(currentKey.toString() to currentValue.toString())

        // reset and continue.
        reset()
      }

      URL_PARAM_KV_SEPARATOR -> if (currentKey.isNotEmpty()) {
        // switch to gathering the value.
        kvSplitSeen = true
      } else {
        // if we arrive here, the key is empty; we need to discard the value.
        emptyKeyIgnoreValue = true
      }

      // append the character to the current buffer.
      else -> if (!emptyKeyIgnoreValue) appendTo(
        if (kvSplitSeen) currentValue else currentKey,
        char,
      )
    }
  }

  // if there is a final buffered key-value pair remaining, yield it.
  if (currentKey.isNotEmpty()) {
    yield(currentKey.toString() to currentValue.toString())
  }
}

/**
 * Merge parameters with the same name into an order-preserving list.
 *
 * @param count Known count of key-value pairs.
 * @param seq The sequence of key-value pairs to merge.
 * @return A sequence of key-value pairs, where the values are merged into a list.
 */
@VisibleForTesting
internal fun mergeFoldQueryParams(count: Int, seq: Sequence<Pair<String, String>>): Map<String, MutableList<String>> {
  return seq.groupByTo(HashMap(count), { it.first }, { it.second })
}

/**
 * Merge parameters with the same name into an order-preserving list.
 *
 * This variant will count the key-value pairs to estimate map size.
 *
 * @param seq The sequence of key-value pairs to merge.
 * @return A sequence of key-value pairs, where the values are merged into a list.
 */
@VisibleForTesting
internal fun mergeFoldQueryParams(str: String, seq: Sequence<Pair<String, String>>): Map<String, MutableList<String>> {
  return mergeFoldQueryParams(str.count { it == URL_PARAMS_SEPARATOR } + 1, seq)
}

/**
 * Parse a guest value as a set of URL parameters.
 *
 * @param value The guest value to parse.
 * @return A sequence of key-value pairs representing the parsed query parameters.
 */
@VisibleForTesting
internal fun parseParamsFromGuest(value: GuestValue): Sequence<Pair<String, String>> = when {
  // strings are accepted for parsing
  value.isString -> parseParamsToMultiMap(value.asString())

  // null produces an empty map
  value.isNull -> emptySequence()

  // throw an error because we can't recognize the type
  else -> throw TypeError.create("Cannot parse URLSearchParams from value")
}

/**
 * Parse and build parameters from a guest value.
 *
 * @param value The guest value to parse and build from.
 * @return Finalized URL params map.
 */
@VisibleForTesting
internal fun buildQueryParamsFromGuest(value: GuestValue): URLParamsMap {
  return parseParamsFromGuest(value).let { seq ->
    URLParamsMap.fromEntries(mergeFoldQueryParams(seq.count(), seq).entries)
  }
}

/**
 * Parse and build parameters from a raw string.
 *
 * @param value The string value to parse and build from.
 * @return Finalized URL params map.
 */
@VisibleForTesting
internal fun buildQueryParamsFromString(value: String): URLParamsMap {
  return parseParamsToMultiMap(value).let { seq ->
    URLParamsMap.fromEntries(mergeFoldQueryParams(value, seq).entries)
  }
}

private inline fun <reified T> IURLSearchParams.ensureWritable(
  op: URLSearchParamsIntrinsic.MutableURLSearchParams.() -> T
): T {
  if (this !is IMutableSearchParams) throw UnsupportedOperationException("URLSearchParams is immutable")
  return op(this as URLSearchParamsIntrinsic.MutableURLSearchParams)
}

private fun IURLSearchParams.pluckMember(key: String): Any {
  return when (key) {
    // read methods and properties first
    "entries" -> entries
    "forEach" -> ProxyExecutable {
      val first = it.firstOrNull() ?: throw TypeError.create("Cannot execute `forEach` without a function")
      if (!first.canExecute()) throw TypeError.create("Cannot execute non-function with `forEach`")
      forEach { entry ->
        first.execute(object: MapLike.Entry<String, String> {
          @get:Polyglot override val key: String = entry.key
          @get:Polyglot override val value: String = entry.value
        })
      }
    }

    "get" -> ProxyExecutable {
      val getKey = it.getOrNull(0)
      if (getKey == null || getKey.isNull || !getKey.isString) throw TypeError.create("Invalid key")
      get(getKey.asString())
    }

    "getAll" -> ProxyExecutable {
      val getAllKey = it.getOrNull(0)
      if (getAllKey == null || getAllKey.isNull || !getAllKey.isString) throw TypeError.create("Invalid key")
      getAll(getAllKey.asString())
    }

    "has" -> ProxyExecutable {
      val hasKey = it.getOrNull(0)
      if (hasKey == null || hasKey.isNull || !hasKey.isString) throw TypeError.create("Invalid key")
      has(hasKey.asString())
    }

    "keys" -> keys
    "size" -> size
    "toString" -> ProxyExecutable { toString() }
    "values" -> values

    // write methods and properties second
    "append" -> ProxyExecutable {
      ensureWritable {
        val appendedKey = it.getOrNull(0)?.asString()
        val value = it.getOrNull(1)?.asString()
        if (appendedKey == null || value == null) {
          throw TypeError.create("Invalid key or value")
        }
        append(appendedKey, value)
      }
    }

    "delete" -> ProxyExecutable {
      ensureWritable {
        val deletedKey = it.getOrNull(0)?.asString() ?: throw TypeError.create("Invalid key")
        delete(deletedKey)
      }
    }

    "set" -> ProxyExecutable {
      ensureWritable {
        val setKey = it.getOrNull(0)?.asString()
        val value = it.getOrNull(1)?.asString()
        if (setKey == null || value == null) {
          throw TypeError.create("Invalid key or value")
        }
        set(setKey, value)
      }
    }

    // sort in-place
    "sort" -> ProxyExecutable { sort() }

    // illegal: not in members list
    else -> error("Unknown member: $key")
  }
}

/** Implements an intrinsic for the `URLSearchParams` global defined by the WhatWG URL Specification. */
@Suppress("RedundantVisibilityModifier")
@Intrinsic(URLSearchParamsIntrinsic.GLOBAL_URL_SEARCH_PARAMS, internal = false)
internal class URLSearchParamsIntrinsic : AbstractJsIntrinsic() {
  internal companion object {
    /** Global where the `URL` constructor is mounted. */
    const val GLOBAL_URL_SEARCH_PARAMS = "URLSearchParams"

    // JS symbol for the `URLSearchParams` constructor.
    private val URL_SEARCH_PARAMS_SYMBOL = GLOBAL_URL_SEARCH_PARAMS.asPublicJsSymbol()

    // Default constructor entry for `URLSearchParams`.
    @JvmStatic public val constructor = ProxyInstantiable { arguments ->
      when (arguments.size) {
        0 -> URLSearchParams()
        1 -> URLSearchParams(arguments[0])
        else -> throw valueError("Invalid number of arguments: ${arguments.size}")
      }
    }
  }

  /**
   * ## Extractable Backing Map
   *
   * Describes a type which can provide its contents as a map of URL parameters.
   */
  internal interface ExtractableBackingMap<T, K, V> where T: Map<K, V> {
    /**
     * TBD.
     */
    fun asMap(): T
  }

  // Base implementation of shared logic across all URL search parameter classes.
  internal sealed class AbstractURLSearchParams (backingMap: URLParamsMap) :
    ProxyHashMap,
    ProxyObject,
    BaseJsMultiMap<String, String>(backingMap, mutable = false, sorted = true, threadsafe = true),
    ExtractableBackingMap<URLParamsMap, String, MutableList<String>> {
    @Suppress("UNCHECKED_CAST")
    override fun asMap(): URLParamsMap = backingMap as URLParamsMap
    override fun getHashSize(): Long = size.toLong()
    override fun hasHashEntry(key: GuestValue): Boolean = has(key.asString())
    override fun getHashValue(key: GuestValue): String? = get(key.asString())
    override fun getMember(key: String): Any = (this as IURLSearchParams).pluckMember(key)
    override fun getHashEntriesIterator(): Any = JsIteratorFactory.forIterator(entries())
    override fun putHashEntry(key: GuestValue?, value: GuestValue?) {
      throw UnsupportedOperationException("URLSearchParams is immutable")
    }

    override fun putMember(key: String?, value: GuestValue?) {
      throw UnsupportedOperationException("URLSearchParams members are immutable")
    }
  }

  // Extends the base URL search params implementation with mutability.
  internal sealed class AbstractMutableURLSearchParams (backingMap: URLParamsMap = URLParamsMap.empty()):
    ProxyHashMap,
    ProxyObject,
    BaseMutableJsMultiMap<String, String>(backingMap, threadsafe = true, sorted = true),
    ExtractableBackingMap<URLParamsMap, String, MutableList<String>> {
    @Suppress("UNCHECKED_CAST")
    override fun asMap(): URLParamsMap = backingMap as URLParamsMap
    override fun getHashSize(): Long = size.toLong()
    override fun hasHashEntry(key: GuestValue): Boolean = has(key.asString())
    override fun getHashValue(key: GuestValue): String? = get(key.asString())
    override fun getMember(key: String): Any = (this as IURLSearchParams).pluckMember(key)
    override fun getHashEntriesIterator(): Any = JsIteratorFactory.forIterator(entries())

    override fun putHashEntry(key: GuestValue, value: GuestValue) {
      if (!key.isString) throw TypeError.create("Invalid key type")
      if (!value.isString) throw TypeError.create("Invalid value type")
      put(key.asString(), value.asString())
    }

    override fun putMember(key: String?, value: GuestValue?) {
      throw UnsupportedOperationException("URLSearchParams members are immutable")
    }

    override fun sort() {
      // no-op (already sorted)
    }
  }

  // Concrete (effective) implementation for immutable URL search parameters.
  public class URLSearchParams private constructor (backingMap: URLParamsMap) :
    AbstractURLSearchParams(backingMap),
    IURLSearchParams {
    @Polyglot constructor() : this(URLParamsMap.empty())

    @Polyglot constructor(other: Any?) : this(when (other) {
      null -> URLParamsMap.empty()
      is GuestValue -> buildQueryParamsFromGuest(other)
      is String -> buildQueryParamsFromString(other)
      is AbstractURLSearchParams -> other.asMap()
      is AbstractMutableURLSearchParams -> other.asMap()
      else -> throw TypeError.create("Cannot parse URLSearchParams from value")
    })

    @Polyglot override fun toString(): String = "URLSearchParams(immutable, count=$size)"

    override fun sort() {
      // no-op (already sorted)
    }
  }

  // Concrete (effective) implementation for mutable URL search parameters.
  public class MutableURLSearchParams private constructor (backingMap: URLParamsMap) :
    AbstractMutableURLSearchParams(backingMap),
    IMutableSearchParams {

    @Polyglot constructor() : this(URLParamsMap.empty())

    @Polyglot constructor(other: Any?) : this(when (other) {
      null -> URLParamsMap.empty()
      is GuestValue -> buildQueryParamsFromGuest(other)
      is String -> buildQueryParamsFromString(other)
      is AbstractURLSearchParams -> other.asMap()
      is AbstractMutableURLSearchParams -> other.asMap()
      else -> throw TypeError.create("Cannot parse URLSearchParams from value")
    })

    @Polyglot override fun toString(): String = "URLSearchParams(mutable, count=$size)"
  }

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // mount `URLSearchParams`
    bindings[URL_SEARCH_PARAMS_SYMBOL] = constructor
  }
}

/**
 * ## Parse URL Params From String
 *
 * Host-side utility function which parses URL parameters from a given [String] context.
 * URL search parameters are immutable by default; see [mutableUrlParams] to parse to a mutable result.
 *
 * @receiver String to parse
 * @return Parsed URL search parameters (immutable)
 */
public fun String.urlParams(): URLSearchParams = URLSearchParamsIntrinsic.URLSearchParams(this)

/**
 * ## Parse URL Params From String (Mutable)
 *
 * Host-side utility function which parses URL parameters from a given [String] context.
 * URL search parameters are mutable by default; see [urlParams] to parse to an immutable result.
 *
 * @receiver String to parse
 * @return Parsed URL search parameters (mutable)
 */
public fun String.mutableUrlParams(): MutableURLSearchParams =
  URLSearchParamsIntrinsic.MutableURLSearchParams(this)
