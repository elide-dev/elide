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

package elide.runtime.intrinsics

import elide.runtime.gvm.GuestLanguage

/**
 * # Intrinsics Resolver
 *
 * Resolves a set of intrinsic implementation objects which should be provided to a given guest VM; typically,
 * intrinsics are resolved at build-time and defined statically within an application or image.
 */
internal interface IntrinsicsResolver {
  /**
   * Resolves a set of intrinsic implementation objects which should be provided to a given guest VM; intrinsics are
   * typically resolved at build time.
   *
   * @param language Language to resolve intrinsics for.
   * @return A set of intrinsic implementation objects.
   */
  fun resolve(language: GuestLanguage): Set<GuestIntrinsic>
}
