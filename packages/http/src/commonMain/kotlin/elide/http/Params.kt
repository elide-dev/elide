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

package elide.http

/**
 * ## HTTP Parameters
 *
 * Describes a container of HTTP URL parameters (also referred to as "query parameters" or "GET parameters") which are
 * parsed from an HTTP request URL. URL parameters can be repeated at a given key, so they are held in a multi-value map
 * form.
 *
 * Parameter keys and values are typically generated via lazy parsing (through [parse]). Keys can be typed or untyped,
 * and values are always strings (but can be singular string or multiple, similar to [Headers]). Unlike [Headers], no
 * case normalization is performed.
 */
public sealed interface Params {
  /** Count of parameters within this URL params container, including duplicated keys. */
  public val size: UInt

  /** Count of parameters within this URL params container, ignoring duplicated keys. */
  public val sizeDistinct: UInt

  /** A sequence of all the parameter keys in this map. */
  public val keys: Sequence<String>

  /**
   * Indicate whether these parameters contain the provided [key].
   *
   * @param key Key to check for presence in this parameter set.
   * @return `true` if the key is present in this parameter set, `false` otherwise.
   */
  public operator fun contains(key: ParamName): Boolean

  /**
   * Indicate whether these parameters contain the provided [key].
   *
   * @param key Key to check for presence in this parameter set.
   * @return `true` if the key is present in this parameter set, `false` otherwise.
   */
  public operator fun contains(key: String): Boolean

  /**
   * Retrieve any extant parameter value bound to the specified [key].
   *
   * @param key Key to retrieve from this parameter set.
   * @return The parameter value bound to the specified key, or `null` if no such value exists.
   */
  public operator fun get(key: ParamName): ParamValue?

  /**
   * Retrieve any extant parameter value bound to the specified [key].
   *
   * @param key Key to retrieve from this parameter set.
   * @return The parameter value bound to the specified key, or `null` if no such value exists.
   */
  public operator fun get(key: String): ParamValue?

  /** Represents an empty suite of URL params. */
  public data object Empty : Params {
    override fun toString(): String = "Params.Empty"
    override val keys: Sequence<String> = emptySequence()
    override val size: UInt get() = 0u
    override val sizeDistinct: UInt get() = 0u
    override fun contains(key: ParamName): Boolean = false
    override fun contains(key: String): Boolean = false
    override fun get(key: ParamName): ParamValue? = null
    override fun get(key: String): ParamValue? = null
  }

  /** Platform implementation of an HTTP parameters container. */
  public interface PlatformParams<T> : Params {
    /** Platform-specific container value. */
    public val value: T & Any
  }

  /** Factories for obtaining [Params]. */
  public companion object {
    /** @return Lazily parsed URL params. */
    @JvmStatic public fun parse(params: String): Params = DefaultUrlParams.parse(params)
  }
}
