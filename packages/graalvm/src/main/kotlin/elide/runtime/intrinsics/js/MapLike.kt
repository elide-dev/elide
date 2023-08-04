/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

/**
 * TBD.
 */
public interface MapLike<K: Any, V> : Map<K, V> {
  /**
   * TBD.
   */
  public interface Entry<K, V> {
    /**
     * TBD.
     */
    public val key: K

    /**
     * TBD.
     */
    public val value: V
  }

  /**
   * TBD.
   */
  public fun entries(): JsIterator<Entry<K, V>>

  /**
   * TBD.
   */
  public fun forEach(op: (Entry<K, V>) -> Unit)

  /**
   * TBD.
   */
  public override fun get(key: K): V?

  /**
   * TBD.
   */
  public fun has(key: K): Boolean

  /**
   * TBD.
   */
  public fun keys(): JsIterator<K>

  /**
   * TBD.
   */
  override fun toString(): String

  /**
   * TBD.
   */
  public fun values(): JsIterator<V>
}
