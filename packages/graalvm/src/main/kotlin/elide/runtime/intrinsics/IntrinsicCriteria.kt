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
package elide.runtime.intrinsics

/**
 * # Intrinsic Criteria
 *
 * Specifies a function which filters [GuestIntrinsic] objects by some criteria during resolution through an instance of
 * a [IntrinsicsResolver].
 */
@FunctionalInterface public fun interface IntrinsicCriteria {
  /**
   * Filters a set of intrinsic implementation objects by some criteria during resolution through an instance of a
   * [IntrinsicsResolver].
   *
   * @param intrinsic Intrinsic to filter.
   * @return Whether the intrinsic should be included in the final set.
   */
  public fun filter(intrinsic: GuestIntrinsic): Boolean

  /** Factory methods which create criteria. */
  public companion object {
    /** @return Compound filter which applies all provided [filters]. */
    @JvmStatic public fun all(filters: List<IntrinsicCriteria>): IntrinsicCriteria = IntrinsicCriteria { intrinsic ->
      filters.all { it.filter(intrinsic) }
    }
  }
}
