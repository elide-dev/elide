package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * Lightweight wrapper around a [PolyglotValue] that represents an executable request handler, allowing calls to the
 * underlying guest callable as a normal Kotlin function without arguments or return type.
 *
 * @see GuestCallback.of
 */
@DelicateElideApi @JvmInline internal value class GuestCallback private constructor(
  private val value: PolyglotValue
) : () -> Unit {
  override fun invoke() {
    value.executeVoid()
  }

  internal companion object {
    /** Wraps a [PolyglotValue] and returns it as a [GuestCallback]. The [value] must be executable. */
    infix fun of(value: PolyglotValue): GuestCallback {
      // we can only verify whether the value is function-like
      require(value.canExecute()) { "Guest handlers must be executable values." }
      return GuestCallback(value)
    }
  }
}