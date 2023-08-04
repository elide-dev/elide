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

package elide.runtime.gvm.internals.intrinsics

import io.micronaut.context.annotation.DefaultScope
import io.micronaut.context.annotation.Infrastructure
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import elide.annotations.Context
import elide.annotations.Singleton
import elide.runtime.gvm.internals.GraalVMGuest

/**
 * # Intrinsic Class
 *
 * This annotation marks a class as an "intrinsic" which is used by Elide's guest VM system to implement native methods
 * provided to guest code. Intrinsics are initialized early and included with every spawned context as injected globals
 * present within every scope.
 *
 * @param global Global name implemented by this class; only present when a single intrinsic implementation class
 *   corresponds 1:1 with a global name in the guest language.
 * @param language Defines the language this intrinsic is implemented for; defaults to JavaScript which is the only
 *   supported guest language at this time.
 */
@Singleton
@Introspected
@Infrastructure
@MustBeDocumented
@ReflectiveAccess
@DefaultScope(Context::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
internal annotation class Intrinsic(
  val global: String = "",
  val language: GraalVMGuest = GraalVMGuest.JAVASCRIPT,
)
