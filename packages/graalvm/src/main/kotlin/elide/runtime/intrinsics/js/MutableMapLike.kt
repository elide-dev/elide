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

import elide.runtime.intrinsics.js.err.TypeError

/**
 * TBD.
 */
public interface MutableMapLike<K: Any, V> : MapLike<K, V>, MutableMap<K, V> {
  /**
   * TBD.
   */
  public fun delete(key: K)

  /**
   * TBD.
   */
  public fun set(key: K, value: V)

  /**
   * TBD.
   *
   * @throws TypeError if the underlying map key type is not sortable.
   */
  @Throws(TypeError::class)
  public fun sort()
}
