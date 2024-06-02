/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.sqlite

import io.micronaut.core.annotation.ReflectiveAccess
import elide.annotations.API
import elide.runtime.intrinsics.js.MapLike

/**
 * # SQLite Object
 *
 * Describes a map-like object which is backed by a single SQLite query result; the object provides access to values at
 * the names for each associated column, along with certain host-side metadata ([columns], [columnTypes]) which describe
 * the structure of the result set.
 *
 * The result is also available as a list of values, via [asList].
 *
 * @see MapLike Map-like base interface
 */
@API @ReflectiveAccess public interface SQLiteObject: MapLike<String, Any?> {
  /** Host-side only: array of columns specified by this object. */
  public val columns: Array<String>

  /** Host-side only: mapping of columns to their interpreted type. */
  public val columnTypes: Map<String, SQLiteType>

  /**
   * Return this object as an ordered list of values.
   *
   * The order of these values is preserved for a given query ordering; for example, given the query:
   * ```sql
   * SELECT one, two, three FROM table;
   * ```
   * The list will be ordered as `[one, two, three]`, regardless of other database conditions (open/close state, etc.).
   *
   * @return an ordered list of values.
   */
  public fun asList(): List<Any?>
}
