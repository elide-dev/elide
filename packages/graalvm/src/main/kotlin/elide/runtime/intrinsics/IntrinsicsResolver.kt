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

package elide.runtime.intrinsics

import elide.runtime.gvm.GuestLanguage

/**
 * # Intrinsics Resolver
 *
 * Resolves a set of intrinsic implementation objects which should be provided to a given guest VM; typically,
 * intrinsics are resolved at build-time and defined statically within an application or image.
 */
@FunctionalInterface public fun interface IntrinsicsResolver {
  /**
   * Resolves a set of intrinsic implementation objects which should be provided to a given guest VM; intrinsics are
   * typically resolved at build time.
   *
   * @param language Language to resolve intrinsics for.
   * @param internals Whether internal symbols are made available.
   * @return A set of intrinsic implementation objects.
   */
  public fun generate(language: GuestLanguage, internals: Boolean): Sequence<GuestIntrinsic>

  /**
   * Resolves a set of intrinsic implementation objects which should be provided to a given guest VM; intrinsics are
   * typically resolved at build time.
   *
   * @param language Language to resolve intrinsics for.
   * @param internals Whether internal symbols are made available.
   * @return A set of intrinsic implementation objects.
   */
  public fun resolve(language: GuestLanguage, internals: Boolean): Sequence<GuestIntrinsic> {
    val ext = criteria(internals)
    val base = defaultCriteria(internals)
    val compound = IntrinsicCriteria {
      base.filter(it) && ext.filter(it)
    }

    return generate(
      language,
      internals,
    ).filter {
      compound.filter(it)
    }
  }

  /**
   * Default criteria to apply when resolving intrinsics.
   *
   * @param allowInternal Whether internal symbols should be included.
   * @return Criteria to apply when resolving intrinsics; `false` should exclude a given symbol.
   */
  public fun defaultCriteria(allowInternal: Boolean): IntrinsicCriteria = IntrinsicCriteria { symbol ->
    !symbol.isInternal || allowInternal
  }

  /**
   * Criteria to apply when resolving intrinsics.
   *
   * @param allowInternal Whether internal symbols should be included.
   * @return Criteria to apply when resolving intrinsics; `false` should exclude a given symbol.
   */
  public fun criteria(allowInternal: Boolean): IntrinsicCriteria = IntrinsicCriteria { symbol ->
    true  // no-op by default
  }

  /**
   * Resolves a set of intrinsic implementation objects which should be provided to a given guest VM; intrinsics are
   * typically resolved at build time.
   *
   * NOTE: Internal symbols are not returned by default.
   *
   * @param language Language to resolve intrinsics for.
   * @return A set of intrinsic implementation objects.
   */
  public fun resolve(language: GuestLanguage): Sequence<GuestIntrinsic> = resolve(
    language,
    internals = true,  // @TODO: disablement
  )
}
