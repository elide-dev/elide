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

package elide.exec

import kotlinx.serialization.Serializable

/**
 * # Container
 */
@Serializable
public sealed interface Container<K, T> {
  /**
   *
   */
  public val count: UInt

  /**
   *
   */
  public operator fun get(key: K): T?

  /**
   *
   */
  public operator fun contains(key: K): Boolean

  /**
   *
   */
  public operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T?

  /**
   *
   */
  public sealed interface Mutable<K, T> : Container<K, T> {
    /**
     *
     */
    public operator fun set(key: K, value: T)

    /**
     *
     */
    public fun remove(key: K): T?

    /**
     *
     */
    public fun clear()
  }
}
