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
package elide.runtime.intrinsics.js

import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API

/**
 * Object properties expected to be present on [URLSearchParams].
 */
internal val URL_SEARCH_PARAMS_PROPERTIES = arrayOf(
  "size",
  "entries",
  "forEach",
  "get",
  "getAll",
  "has",
  "keys",
  "toString",
  "values",
)

/**
 * Object properties expected to be present on [MutableURLSearchParams].
 */
internal val URL_SEARCH_PARAMS_MUTABLE_PROPERTIES = URL_SEARCH_PARAMS_PROPERTIES.plus(arrayOf(
  "append",
  "delete",
  "set",
  "sort",
))

/**
 * # JavaScript: URL Search Parameters
 *
 * The `URLSearchParams` interface and class is universally supported across most JavaScript and browser environments,
 * and models query (`GET`-method) parameters expressed within a URL.
 *
 * URL search parameters are typically associated with, and made available from, [URL] instances.
 *
 * Note that parameters can be specified multiple times in a URL; as a result, this structure behaves as [MultiMapLike].
 *
 * &nbsp;
 *
 * ## Mutability
 *
 * For mutable URL search params, this interface is extended into [MutableURLSearchParams].
 */
@API public interface URLSearchParams :
  java.io.Serializable,
  MultiMapLike<String, String>,
  ProxyHashMap,
  ProxyObject {
  override fun getMemberKeys(): Array<String> = URL_SEARCH_PARAMS_PROPERTIES
  override fun hasMember(key: String): Boolean = key in URL_SEARCH_PARAMS_PROPERTIES

  /**
   * Sort the parameters in-place.
   */
  public fun sort()
}
