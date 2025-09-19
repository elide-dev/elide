/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.intrinsics.server.http.v2

/**
 * A mutable key-value map with type-safe [keys][Key], used to store arbitrary data associated with an [HttpContext].
 * Sessions are not automatically persisted or populated, instead they provide a base storage API for frameworks.
 */
@JvmInline public value class HttpSession private constructor(private val values: MutableMap<String, Any>) {
  /**
   * A type-safe key used to store and retrieve values from an [HttpSession] map. Keys are inline wrappers around a
   * string token, and are meant to add semantics to session map entries.
   */
  @JvmInline public value class Key<@Suppress("unused") T : Any>(public val token: String) {
    public companion object {
      /**
       * Returns a type-unsafe [Key]; this is discouraged for general use, but kept at as a shortcut for keys
       * originating from guest code, where values may not have a strong type.
       */
      public fun unsafe(token: String): Key<Any> = Key(token)
    }
  }

  /** Create a new empty session map. */
  public constructor() : this(mutableMapOf())

  /** Returns whether the given [key] is present in the session map. */
  public operator fun contains(key: Key<*>): Boolean {
    return values.containsKey(key.token)
  }

  /** Returns the value associated with the given [key], or `null` if the key is not present in the session map. */
  public operator fun <T : Any> get(key: Key<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return values[key.token] as T?
  }

  /** Associate a [value] with the given [key], replacing any existing values with the same key. */
  public operator fun <T : Any> set(key: Key<T>, value: T) {
    values[key.token] = value
  }

  /**
   * Associate a [value] with the given [key] in the session map, returning the previous value for that key if present.
   */
  public fun <T : Any> put(key: Key<T>, value: T): T? {
    @Suppress("UNCHECKED_CAST")
    return values.put(key.token, value) as T?
  }

  /** Remove the given [key] from the session map, returning the associated value if it exists. */
  public fun <T : Any> remove(key: Key<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return values.remove(key.token) as T?
  }

  public companion object {
    /** Construct a new [HttpSession] by copying the given [values] into an empty session map. */
    public fun from(values: Map<String, Any>): HttpSession {
      return HttpSession(values.toMutableMap())
    }
  }
}
