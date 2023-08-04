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

import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchHeadersIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsMultiMap

/**
 * # Fetch: Headers.
 *
 * Describes the interface for a Fetch API compliant structure containing HTTP headers; this includes both the header
 * names (as keys), and the header values, including multiple values per header, as applicable. Effectively, the fetch
 * headers object acts as a container (as a [JsMultiMap], specifically of all strings).
 *
 * &nbsp;
 *
 * ## Mutability
 *
 * Headers can be expressed in mutable or immutable form. For example, when headers are used as inputs for a request
 * (meaning the values originate from another machine), they are expressed in immutable form. To mutate such headers,
 * the developer can simply construct a mutable copy using one of the constructors available via [FetchHeaders.Factory].
 *
 * &nbsp;
 *
 * ## Rendering headers
 *
 * Multiple header values, by spec, can often be reduced to comma-separated values within the same header name. Child
 * implementations are expected to provide a render function which performs this task from held values.
 */
public interface FetchHeaders : MultiMapLike<String, String> {

  /**
   * ## Fetch: Headers factory.
   *
   * Specifies constructors which are expected to be made available for a given [FetchHeaders] implementation. These
   * constructors include JavaScript-spec constructors as well as regular host-side `wrap` or `create` constructors. In
   * all cases, these constructors are expected to return a copy of the provided data, as applicable.
   */
  public interface Factory<Impl> where Impl: FetchHeaders {
    /**
     * Create an empty set of immutable fetch headers.
     *
     * @return Immutable and empty set of fetch headers.
     */
    public fun empty(): Impl

    /**
     * Create a set of immutable fetch headers from the provided set of [pairs].
     *
     * @return Immutable set of fetch headers.
     */
    public fun fromPairs(pairs: Collection<Pair<String, String>>): Impl

    /**
     * Create a set of immutable fetch headers from the provided set of [pairs].
     *
     * @return Immutable set of fetch headers.
     */
    public fun from(vararg pairs: Pair<String, String>): Impl

    /**
     * Create a set of immutable fetch headers from the provided [map] data.
     *
     * @return Immutable set of fetch headers.
     */
    public fun fromMap(map: Map<String, String>): Impl

    /**
     * Create a set of immutable fetch headers from the provided [map] data.
     *
     * @return Immutable set of fetch headers.
     */
    public fun fromMultiMap(map: Map<String, List<String>>): Impl

    /**
     * Create an immutable copy of the provided [previous] fetch headers.
     *
     * @return Immutable copy of the provided fetch headers.
     */
    public fun from(previous: FetchHeaders): Impl
  }

  /** Factory for default-implementation instances of [FetchHeaders]. */
  public companion object DefaultFactory : Factory<FetchHeaders> {
    /**
     * Create an empty set of immutable fetch headers.
     *
     * @return Immutable and empty set of fetch headers.
     */
    override fun empty(): FetchHeaders = FetchHeadersIntrinsic.empty()

    /**
     * Create a set of immutable fetch headers from the provided set of [pairs].
     *
     * @return Immutable set of fetch headers.
     */
    override fun fromPairs(pairs: Collection<Pair<String, String>>): FetchHeaders =
      FetchHeadersIntrinsic.fromPairs(pairs)

    /**
     * Create a set of immutable fetch headers from the provided set of [pairs].
     *
     * @return Immutable set of fetch headers.
     */
    override fun from(vararg pairs: Pair<String, String>): FetchHeaders =
      FetchHeadersIntrinsic.from(*pairs)

    /**
     * Create a set of immutable fetch headers from the provided [map] data.
     *
     * @return Immutable set of fetch headers.
     */
    override fun fromMap(map: Map<String, String>): FetchHeaders = FetchHeadersIntrinsic.fromMap(map)

    /**
     * Create a set of immutable fetch headers from the provided [map] data.
     *
     * @return Immutable set of fetch headers.
     */
    override fun fromMultiMap(map: Map<String, List<String>>): FetchHeaders = FetchHeadersIntrinsic.fromMultiMap(map)

    /**
     * Create an immutable copy of the provided [previous] fetch headers.
     *
     * @return Immutable copy of the provided fetch headers.
     */
    override fun from(previous: FetchHeaders): FetchHeaders = FetchHeadersIntrinsic.from(previous)
  }

  /**
   * Render these fetch headers into a raw map where the keys are normalized HTTP header names, and the values are
   * either rendered into a single comma-separated string, or expressed literally, depending on the value of [flatten].
   *
   * @param flatten Whether to flatten repeated values into a single comma-separated value.
   * @return Raw map as requested.
   */
  public fun render(flatten: Boolean = true): Map<String, List<String>>
}
