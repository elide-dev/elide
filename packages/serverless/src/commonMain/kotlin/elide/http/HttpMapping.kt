/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

import elide.http.api.HttpText
import elide.struct.TreeMap
import elide.struct.api.SortedMap
import elide.http.api.HttpMapping as HttpMappingAPI

/**
 *
 */
public class HttpMapping<Key, Value> private constructor (
  /** */
  private val backing: SortedMap<Key, Value> = TreeMap.empty(),
) : HttpMappingAPI<Key, Value>, SortedMap<Key, Value> by backing
        where Key : Comparable<Key>, Key: HttpText, Value: HttpText
