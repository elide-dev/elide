package elide.proto.internal.annotations

import elide.annotations.Eager
import elide.annotations.Singleton
import elide.proto.ElideProtocol

/**
 * Annotates an Elide Protocol implementation with DI resolution metadata.
 */
@Eager
@Singleton
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Protocol(
  /** Protocol implementation library. */
  val value: ElideProtocol.ImplementationLibrary,
)
