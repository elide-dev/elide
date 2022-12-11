package elide.runtime.gvm.intrinsics

import elide.runtime.gvm.internals.GraalVMGuest
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.DefaultScope
import io.micronaut.context.annotation.Infrastructure
import jakarta.inject.Singleton

/**
 * TBD.
 */
@Singleton
@Infrastructure
@MustBeDocumented
@DefaultScope(Context::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class GuestRuntime
