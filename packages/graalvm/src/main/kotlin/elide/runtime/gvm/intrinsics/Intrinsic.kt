package elide.runtime.gvm.intrinsics

import elide.runtime.gvm.internals.GraalVMGuest
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.DefaultScope
import io.micronaut.context.annotation.Infrastructure
import jakarta.inject.Singleton

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
@Infrastructure
@MustBeDocumented
@DefaultScope(Context::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class Intrinsic(
  val global: String = "",
  val language: GraalVMGuest = GraalVMGuest.JAVASCRIPT,
)
