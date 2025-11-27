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
package elide.runtime.intrinsics.js

import org.graalvm.polyglot.proxy.ProxyIterable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.vm.annotations.Polyglot

/**
 * Implements the FormData Web API interface.
 *
 * FormData provides a way to construct a set of key/value pairs representing form fields
 * and their values, which can be sent using fetch() or XMLHttpRequest.
 */
public class FormData : ProxyObject, ProxyIterable {
  private val entries: MutableMap<String, MutableList<Any>> = mutableMapOf()

  /** Appends a new value onto an existing key, or adds the key if it doesn't exist. */
  @Polyglot
  public fun append(name: String, value: Any) {
    entries.getOrPut(name) { mutableListOf() }.add(value)
  }

  /** Deletes a key and all its values. */
  @Polyglot
  public fun delete(name: String) {
    entries.remove(name)
  }

  /** Returns an iterator of all key/value pairs. */
  @Polyglot
  public fun entries(): Iterator<Array<Any>> = iterator {
    for ((key, values) in entries) {
      for (value in values) {
        yield(arrayOf(key, value))
      }
    }
  }

  /** Returns the first value associated with a given key. */
  @Polyglot
  public fun get(name: String): Any? = entries[name]?.firstOrNull()

  /** Returns all values associated with a given key. */
  @Polyglot
  public fun getAll(name: String): List<Any> = entries[name] ?: emptyList()

  /** Returns whether a key exists. */
  @Polyglot
  public fun has(name: String): Boolean = entries.containsKey(name)

  /** Returns an iterator of all keys. */
  @Polyglot
  public fun keys(): Iterator<String> = entries.keys.iterator()

  /** Sets a new value for an existing key, or adds the key/value if it doesn't exist. */
  @Polyglot
  public fun set(name: String, value: Any) {
    entries[name] = mutableListOf(value)
  }

  /** Returns an iterator of all values. */
  @Polyglot
  public fun values(): Iterator<Any> = iterator {
    for (values in entries.values) {
      for (value in values) {
        yield(value)
      }
    }
  }

  // ProxyObject implementation
  override fun getMemberKeys(): Any = arrayOf("append", "delete", "entries", "get", "getAll", "has", "keys", "set", "values")

  override fun hasMember(key: String?): Boolean = key in arrayOf("append", "delete", "entries", "get", "getAll", "has", "keys", "set", "values")

  override fun getMember(key: String?): Any? = when (key) {
    "append" -> { name: String, value: Any -> append(name, value) }
    "delete" -> { name: String -> delete(name) }
    "entries" -> entries()
    "get" -> { name: String -> get(name) }
    "getAll" -> { name: String -> getAll(name) }
    "has" -> { name: String -> has(name) }
    "keys" -> keys()
    "set" -> { name: String, value: Any -> set(name, value) }
    "values" -> values()
    else -> null
  }

  override fun putMember(key: String?, value: org.graalvm.polyglot.Value?) {
    // FormData is not directly mutable via property access
  }

  // ProxyIterable implementation - iterates over entries
  override fun getIterator(): Any = entries()

  public companion object {
    /**
     * Parse URL-encoded form data (application/x-www-form-urlencoded).
     */
    @JvmStatic
    public fun parseUrlEncoded(body: String): FormData {
      val formData = FormData()
      if (body.isBlank()) return formData

      for (pair in body.split("&")) {
        val parts = pair.split("=", limit = 2)
        val key = java.net.URLDecoder.decode(parts[0], Charsets.UTF_8)
        val value = if (parts.size > 1) java.net.URLDecoder.decode(parts[1], Charsets.UTF_8) else ""
        formData.append(key, value)
      }
      return formData
    }
  }
}
