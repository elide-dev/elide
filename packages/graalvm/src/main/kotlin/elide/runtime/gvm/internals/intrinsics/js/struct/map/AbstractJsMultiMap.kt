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

package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MultiMapLike

/**
 * # JS: Abstract Multi-Map
 *
 * Extends the base [AbstractJsMap] with support for the [MultiMapLike] interface, which allows for multiple map values
 * per key. Additional methods are available which resolve all values for a given key. Methods which typically return a
 * single value for a key instead return the first value, if any.
 *
 * @param K Key type for the map. Keys cannot be `null`.
 * @param V Value type for the map. Values can be `null`.
 * @param sorted Whether the map implementation holds a sorted representation.
 * @param mutable Whether the map implementation is mutable.
 * @param threadsafe Whether the map implementation is thread-safe.
 */
internal sealed class AbstractJsMultiMap<K: Any, V> constructor (
  sorted: Boolean,
  mutable: Boolean,
  threadsafe: Boolean,
) : AbstractJsMap<K, V>(
  multi = true,
  mutable = mutable,
  sorted = sorted,
  threadsafe = threadsafe,
), MultiMapLike<K, V>
