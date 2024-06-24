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
@file:Suppress("MnInjectionPoints")

package elide.runtime.gvm.internals.intrinsics

import io.micronaut.context.annotation.Infrastructure
import elide.annotations.Context
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.gvm.GuestLanguage
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.IntrinsicsResolver

/** Resolve intrinsics known at build-time via annotation processing. */
@Context @Singleton @Infrastructure internal class BuiltinIntrinsicsResolver : IntrinsicsResolver {
  /** All candidate guest intrinsics. */
  @Inject lateinit var intrinsics: Collection<GuestIntrinsic>

  /** @inheritDoc */
  override fun generate(language: GuestLanguage, internals: Boolean): Sequence<GuestIntrinsic> = intrinsics
    .asSequence()
    .filter { it.supports(language) }
}
